package com.fearmikey.garage.obd

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** An open byte pipe to an ELM327-compatible adapter. */
interface ObdTransport {
    val input: InputStream
    val output: OutputStream
    fun close()
}

private const val TAG = "ObdTransport"

// region Bluetooth Classic

private class SocketTransport(private val socket: BluetoothSocket) : ObdTransport {
    override val input: InputStream = socket.inputStream
    override val output: OutputStream = socket.outputStream
    override fun close() {
        runCatching { input.close() }
        runCatching { output.close() }
        runCatching { socket.close() }
    }
}

/**
 * Connects over RFCOMM, trying progressively more permissive strategies because many cheap
 * ELM327 clones reject the standard secure SPP connection:
 *  1. Secure SPP service record
 *  2. Insecure SPP service record
 *  3. Direct RFCOMM channel 1 (hidden API, used by most OBD apps as a last resort)
 *
 * Each attempt is bounded by [timeoutMs]; on timeout the socket is closed, which unblocks
 * the otherwise uninterruptible [BluetoothSocket.connect].
 */
@SuppressLint("MissingPermission", "DiscouragedPrivateApi")
internal suspend fun connectClassic(adapter: BluetoothAdapter, address: String, timeoutMs: Long): ObdTransport? {
    if (!adapter.isEnabled) return null
    val device = runCatching { adapter.getRemoteDevice(address) }.getOrNull() ?: return null
    runCatching { adapter.cancelDiscovery() }

    val strategies: List<Pair<String, (BluetoothDevice) -> BluetoothSocket>> = listOf(
        "secure" to { d -> d.createRfcommSocketToServiceRecord(SPP_UUID) },
        "insecure" to { d -> d.createInsecureRfcommSocketToServiceRecord(SPP_UUID) },
        "channel1" to { d ->
            d.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType).invoke(d, 1) as BluetoothSocket
        },
    )

    for ((label, factory) in strategies) {
        val socket = runCatching { factory(device) }.getOrNull() ?: continue
        val connected = connectWithTimeout(timeoutMs, onTimeout = { runCatching { socket.close() } }) {
            socket.connect()
        }
        if (connected) {
            Log.i(TAG, "Connected to $address using $label RFCOMM socket")
            return SocketTransport(socket)
        }
        runCatching { socket.close() }
        Log.w(TAG, "$label RFCOMM connect to $address failed")
    }
    return null
}

// endregion

// region Wi-Fi

private class TcpTransport(private val socket: Socket) : ObdTransport {
    override val input: InputStream = socket.getInputStream()
    override val output: OutputStream = socket.getOutputStream()
    override fun close() {
        runCatching { socket.close() }
    }
}

/**
 * Opens a TCP connection to a Wi-Fi adapter. The adapter's hotspot has no internet access, so
 * Android may otherwise route the socket over mobile data; binding to the Wi-Fi network avoids that.
 */
internal suspend fun connectWifi(context: Context, host: String, port: Int, timeoutMs: Long): ObdTransport? =
    withContext(Dispatchers.IO) {
        try {
            val socket = wifiSocketFactory(context)?.createSocket() ?: Socket()
            socket.tcpNoDelay = true
            socket.connect(InetSocketAddress(host, port), timeoutMs.toInt())
            TcpTransport(socket)
        } catch (e: IOException) {
            Log.w(TAG, "Wi-Fi connect to $host:$port failed", e)
            null
        }
    }

@Suppress("DEPRECATION")
private fun wifiSocketFactory(context: Context): javax.net.SocketFactory? = runCatching {
    val cm = context.getSystemService(ConnectivityManager::class.java) ?: return null
    cm.allNetworks.firstOrNull { network ->
        cm.getNetworkCapabilities(network)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
    }?.socketFactory
}.getOrNull()

// endregion

// region Bluetooth Low Energy

/**
 * Exposes a BLE GATT serial-style service (notify characteristic for replies, write
 * characteristic for commands) as a pair of streams so [ObdParser] can use it unchanged.
 */
@SuppressLint("MissingPermission")
internal class BleTransport private constructor() : ObdTransport {

    private val received = ArrayDeque<Byte>()
    private var gatt: BluetoothGatt? = null
    private var writeCharacteristic: BluetoothGattCharacteristic? = null
    private var writeLatch: CountDownLatch? = null
    private var chunkSize = DEFAULT_CHUNK

    @Volatile
    private var closed = false

    override val input: InputStream = object : InputStream() {
        override fun read(): Int = synchronized(received) {
            if (closed && received.isEmpty()) -1 else received.removeFirstOrNull()?.toInt()?.and(0xFF) ?: -1
        }

        override fun read(b: ByteArray, off: Int, len: Int): Int = synchronized(received) {
            if (received.isEmpty()) return if (closed) -1 else 0
            val count = minOf(len, received.size)
            for (i in 0 until count) b[off + i] = received.removeFirst()
            count
        }

        override fun available(): Int {
            if (closed) throw IOException("BLE connection closed")
            return synchronized(received) { received.size }
        }
    }

    override val output: OutputStream = object : OutputStream() {
        private val pending = java.io.ByteArrayOutputStream()
        override fun write(b: Int) {
            pending.write(b)
        }

        override fun flush() {
            val data = pending.toByteArray()
            pending.reset()
            data.toList().chunked(chunkSize).forEach { writeChunk(it.toByteArray()) }
        }
    }

    private fun writeChunk(chunk: ByteArray) {
        val g = gatt ?: throw IOException("BLE connection closed")
        val c = writeCharacteristic ?: throw IOException("No writable characteristic")
        val writeType = if (c.properties and BluetoothGattCharacteristic.PROPERTY_WRITE != 0) {
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        } else {
            BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
        }
        repeat(WRITE_RETRIES) {
            val latch = CountDownLatch(1)
            writeLatch = latch
            val status = g.writeCharacteristic(c, chunk, writeType)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (!latch.await(WRITE_TIMEOUT_MS, TimeUnit.MILLISECONDS)) throw IOException("BLE write timed out")
                return
            }
            // Stack busy with a previous operation; back off briefly and retry.
            Thread.sleep(20)
        }
        throw IOException("BLE write failed")
    }

    private val callback = object : BluetoothGattCallback() {
        var connected: CompletableDeferred<Boolean>? = null
        var servicesDiscovered: CompletableDeferred<Boolean>? = null
        var descriptorWritten: CompletableDeferred<Boolean>? = null
        var mtuChanged: CompletableDeferred<Int>? = null

        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED && status == BluetoothGatt.GATT_SUCCESS) {
                connected?.complete(true)
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                closed = true
                connected?.complete(false)
                writeLatch?.countDown()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            servicesDiscovered?.complete(status == BluetoothGatt.GATT_SUCCESS)
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            mtuChanged?.complete(if (status == BluetoothGatt.GATT_SUCCESS) mtu else DEFAULT_MTU)
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            descriptorWritten?.complete(status == BluetoothGatt.GATT_SUCCESS)
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            writeLatch?.countDown()
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
            synchronized(received) { value.forEach(received::addLast) }
        }
    }

    override fun close() {
        closed = true
        writeLatch?.countDown()
        runCatching { gatt?.disconnect() }
        runCatching { gatt?.close() }
        gatt = null
    }

    companion object {
        private const val DEFAULT_MTU = 23
        private const val DEFAULT_CHUNK = DEFAULT_MTU - 3
        private const val WRITE_RETRIES = 5
        private const val WRITE_TIMEOUT_MS = 2_000L
        private val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

        /** Services commonly used by BLE OBD2 adapters, tried before falling back to a generic search. */
        private val KNOWN_SERVICES = listOf(
            "0000fff0-0000-1000-8000-00805f9b34fb", // Most ELM327 BLE clones, Vgate
            "0000ffe0-0000-1000-8000-00805f9b34fb", // HM-10 style modules
            "000018f0-0000-1000-8000-00805f9b34fb", // Vgate iCar Pro / vLinker
            "e7810a71-73ae-499d-8c15-faa9aef0c3f2", // Veepeak / Konnwei
            "0000beef-0000-1000-8000-00805f9b34fb",
        ).map(UUID::fromString)

        /** Standard SIG services that are never the serial channel. */
        private val IGNORED_SERVICES = setOf(0x1800, 0x1801, 0x180A, 0x180F)

        suspend fun connect(context: Context, adapter: BluetoothAdapter, address: String, timeoutMs: Long): ObdTransport? {
            val device = runCatching { adapter.getRemoteDevice(address) }.getOrNull() ?: return null
            val transport = BleTransport()
            val cb = transport.callback
            try {
                val ok = withTimeoutOrNull(timeoutMs) {
                    cb.connected = CompletableDeferred()
                    val gatt = withContext(Dispatchers.Main) {
                        device.connectGatt(context, false, cb, BluetoothDevice.TRANSPORT_LE)
                    } ?: return@withTimeoutOrNull false
                    transport.gatt = gatt
                    if (!cb.connected!!.await()) return@withTimeoutOrNull false

                    cb.mtuChanged = CompletableDeferred()
                    val mtu = if (gatt.requestMtu(185)) {
                        withTimeoutOrNull(2_000) { cb.mtuChanged!!.await() } ?: DEFAULT_MTU
                    } else {
                        DEFAULT_MTU
                    }
                    transport.chunkSize = (mtu - 3).coerceAtLeast(DEFAULT_CHUNK)

                    cb.servicesDiscovered = CompletableDeferred()
                    if (!gatt.discoverServices() || !cb.servicesDiscovered!!.await()) return@withTimeoutOrNull false

                    val (notify, write) = findSerialCharacteristics(gatt) ?: return@withTimeoutOrNull false
                    transport.writeCharacteristic = write

                    gatt.setCharacteristicNotification(notify, true)
                    val cccd = notify.getDescriptor(CCCD_UUID)
                    if (cccd != null) {
                        val value = if (notify.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) {
                            BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        } else {
                            BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
                        }
                        cb.descriptorWritten = CompletableDeferred()
                        if (gatt.writeDescriptor(cccd, value) == BluetoothGatt.GATT_SUCCESS) {
                            withTimeoutOrNull(2_000) { cb.descriptorWritten!!.await() }
                        }
                    }
                    true
                }
                if (ok == true) return transport
            } catch (e: CancellationException) {
                transport.close()
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "BLE connect to $address failed", e)
            }
            transport.close()
            return null
        }

        private fun findSerialCharacteristics(gatt: BluetoothGatt): Pair<BluetoothGattCharacteristic, BluetoothGattCharacteristic>? {
            val services = gatt.services.orEmpty()
            val ordered = services.sortedBy { service ->
                KNOWN_SERVICES.indexOf(service.uuid).let { if (it < 0) Int.MAX_VALUE else it }
            }
            for (service in ordered) {
                val short = (service.uuid.mostSignificantBits ushr 32).toInt()
                if (short in IGNORED_SERVICES && service.uuid.toString().endsWith("-0000-1000-8000-00805f9b34fb")) continue
                val chars = service.characteristics.orEmpty()
                val notify = chars.firstOrNull {
                    it.properties and (BluetoothGattCharacteristic.PROPERTY_NOTIFY or BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0
                }
                val write = chars.firstOrNull {
                    it.properties and (BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0
                }
                if (notify != null && write != null) return notify to write
            }
            return null
        }
    }
}

// endregion

/**
 * Runs a blocking connect call on an IO thread, giving up after [timeoutMs]. [onTimeout] must
 * unblock the call (typically by closing the socket).
 */
internal suspend fun connectWithTimeout(timeoutMs: Long, onTimeout: () -> Unit, block: () -> Unit): Boolean =
    coroutineScope {
        val attempt = async(Dispatchers.IO) { runCatching(block).isSuccess }
        val result = withTimeoutOrNull(timeoutMs) { attempt.await() }
        if (result == null) {
            onTimeout()
            attempt.cancel()
            false
        } else {
            result
        }
    }

internal val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
