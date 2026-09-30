package com.fearmikey.garage.obd

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream

class ObdParser {

    suspend fun init(inputStream: InputStream, outputStream: OutputStream) {
        sendCommand(inputStream, outputStream, "AT Z") // reset
        delay(500)
        sendCommand(inputStream, outputStream, "AT E0") // echo off
        sendCommand(inputStream, outputStream, "AT L0") // linefeeds off
        sendCommand(inputStream, outputStream, "AT S0") // spaces off
        sendCommand(inputStream, outputStream, "AT SP 0") // auto protocol
    }

    suspend fun getDtcs(inputStream: InputStream, outputStream: OutputStream): List<String> {
        val response = sendCommand(inputStream, outputStream, "03")
        return parseDtcs(response)
    }

    suspend fun clearDtcs(inputStream: InputStream, outputStream: OutputStream) {
        sendCommand(inputStream, outputStream, "04")
    }

    suspend fun getOdometerKm(inputStream: InputStream, outputStream: OutputStream): Int? {
        val response = sendCommand(inputStream, outputStream, "01 A6")
        return parseOdometer(response)
    }

    suspend fun getDistanceSinceCodesClearedKm(inputStream: InputStream, outputStream: OutputStream): Int? {
        val response = sendCommand(inputStream, outputStream, "01 31")
        return parseDistanceSinceCodesCleared(response)
    }

    private suspend fun sendCommand(inputStream: InputStream, outputStream: OutputStream, command: String): String = withContext(Dispatchers.IO) {
        val cmd = command + "\r"
        outputStream.write(cmd.toByteArray())
        outputStream.flush()
        
        val builder = StringBuilder()
        var c: Char
        var b: Int
        // read until '>'
        while (inputStream.read().also { b = it } > -1) {
            c = b.toChar()
            if (c == '>') {
                break
            }
            if (c.isWhitespace()) {
                continue
            }
            builder.append(c)
        }
        val result = builder.toString().trim()
        // clean up response
        // if command was echoed, remove it
        if (result.startsWith(command.replace(" ", ""))) {
            return@withContext result.substring(command.replace(" ", "").length)
        }
        result
    }

    internal fun parseDtcs(response: String): List<String> {
        val dtcs = mutableListOf<String>()
        val cleanResponse = response.replace(" ", "").replace("\r", "").replace("\n", "")
        if (cleanResponse.contains("NODATA") || cleanResponse.isEmpty()) return dtcs

        if (cleanResponse.startsWith("43")) {
            val data = cleanResponse.substring(2)
            // every 4 hex chars is a DTC
            for (i in 0 until data.length step 4) {
                if (i + 4 <= data.length) {
                    val dtcHex = data.substring(i, i + 4)
                    if (dtcHex == "0000") continue
                    
                    val b1 = dtcHex.substring(0, 2).toIntOrNull(16) ?: continue
                    val b2 = dtcHex.substring(2, 4).toIntOrNull(16) ?: continue
                    
                    val pType = (b1 shr 6) and 0x03
                    val firstChar = when (pType) {
                        0 -> 'P'
                        1 -> 'C'
                        2 -> 'B'
                        3 -> 'U'
                        else -> 'P'
                    }
                    val secondChar = (b1 shr 4) and 0x03
                    val thirdChar = b1 and 0x0F
                    val fourthChar = (b2 shr 4) and 0x0F
                    val fifthChar = b2 and 0x0F
                    
                    dtcs.add("$firstChar$secondChar${thirdChar.toString(16)}${fourthChar.toString(16)}${fifthChar.toString(16)}".uppercase())
                }
            }
        }
        return dtcs
    }

    internal fun parseOdometer(response: String): Int? {
        val cleanResponse = response.replace(" ", "").replace("\r", "").replace("\n", "")
        if (cleanResponse.contains("NODATA") || cleanResponse.isEmpty()) return null
        
        // 01 A6 response: 41 A6 A B C D (A*2^24 + B*2^16 + C*2^8 + D) / 10
        if (cleanResponse.startsWith("41A6")) {
            val data = cleanResponse.substring(4)
            if (data.length >= 8) {
                val a = data.substring(0, 2).toLongOrNull(16) ?: return null
                val b = data.substring(2, 4).toLongOrNull(16) ?: return null
                val c = data.substring(4, 6).toLongOrNull(16) ?: return null
                val d = data.substring(6, 8).toLongOrNull(16) ?: return null
                val value = (a * 16777216) + (b * 65536) + (c * 256) + d
                return (value / 10).toInt()
            }
        }
        return null
    }
    
    internal fun parseDistanceSinceCodesCleared(response: String): Int? {
        val cleanResponse = response.replace(" ", "").replace("\r", "").replace("\n", "")
        if (cleanResponse.contains("NODATA") || cleanResponse.isEmpty()) return null
        
        // 01 31 response: 41 31 A B (A*256 + B)
        if (cleanResponse.startsWith("4131")) {
            val data = cleanResponse.substring(4)
            if (data.length >= 4) {
                val a = data.substring(0, 2).toIntOrNull(16) ?: return null
                val b = data.substring(2, 4).toIntOrNull(16) ?: return null
                return (a * 256) + b
            }
        }
        return null
    }
}
