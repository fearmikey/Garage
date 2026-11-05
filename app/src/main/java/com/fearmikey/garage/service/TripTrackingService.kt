package com.fearmikey.garage.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.fearmikey.garage.R
import com.fearmikey.garage.data.local.PreferencesManager
import com.fearmikey.garage.data.local.entity.TripCategory
import com.fearmikey.garage.data.local.entity.TripRecord
import com.fearmikey.garage.data.repository.TripRepository
import com.fearmikey.garage.ui.util.UnitSystem
import com.fearmikey.garage.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.NonCancellable
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.roundToLong

@AndroidEntryPoint
class TripTrackingService : Service() {

    @Inject
    lateinit var preferencesManager: PreferencesManager

    @Inject
    lateinit var tripRepository: TripRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var locationManager: LocationManager
    private var isTracking = false
    private var isPaused = false
    private var distanceMeters = 0.0
    /**
     * The last fix that was actually counted toward the trip distance. Jittery fixes while
     * stationary wander around this anchor without moving it, so they never accumulate.
     */
    private var anchorLocation: Location? = null
    private var unitSystem: UnitSystem = UnitSystem.IMPERIAL
    
    private var startTimeMillis: Long = 0L
    private var vehicleId: Long? = null

    // For simplicity, we expose a companion object state for the UI to observe instantly.
    // In a larger app, this might live in a Singleton Repository.
    companion object {
        private val _tripState = MutableStateFlow<TripState>(TripState.Stopped)
        val tripState: StateFlow<TripState> = _tripState.asStateFlow()

        /** The trip most recently saved by "Stop & Save", so the UI can offer to edit its details. */
        private val _lastSavedTrip = MutableStateFlow<TripRecord?>(null)
        val lastSavedTrip: StateFlow<TripRecord?> = _lastSavedTrip.asStateFlow()

        fun consumeLastSavedTrip() {
            _lastSavedTrip.value = null
        }
        
        const val ACTION_START = "ACTION_START"
        const val ACTION_PAUSE = "ACTION_PAUSE"
        const val ACTION_RESUME = "ACTION_RESUME"
        const val ACTION_STOP = "ACTION_STOP"
        const val EXTRA_VEHICLE_ID = "EXTRA_VEHICLE_ID"

        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "trip_tracking_channel"

        // Drift filtering
        private const val MAX_ACCURACY_METERS = 20f
        private const val MIN_SEGMENT_METERS = 15f
        private const val MAX_FIX_AGE_NANOS = 10_000_000_000L
        private const val MIN_MOVING_SPEED_MPS = 1.0f // ~3.6 km/h
        private const val MAX_PLAUSIBLE_SPEED_MPS = 70f // ~250 km/h
        private const val METERS_PER_MILE = 1609.344
        private const val METERS_PER_KM = 1000.0
    }
    
    sealed class TripState {
        object Stopped : TripState()
        data class Active(
            val distanceMeters: Double,
            val isPaused: Boolean,
            val startTime: Long,
            val vehicleId: Long?,
        ) : TripState()
    }

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            if (isPaused) return
            if (!isUsableFix(location)) return

            val anchor = anchorLocation
            if (anchor == null) {
                anchorLocation = location
                serviceScope.launch {
                    preferencesManager.setActiveTripLastLocation(location.latitude, location.longitude)
                }
                return
            }

            val segment = anchor.distanceTo(location)
            // A move only counts once it clearly exceeds the combined uncertainty of both fixes.
            val threshold = max(MIN_SEGMENT_METERS, anchor.accuracy + location.accuracy)
            if (segment < threshold) return

            // If the receiver reports we're essentially stationary, treat the offset as drift.
            if (location.hasSpeed() && location.speed < MIN_MOVING_SPEED_MPS) return

            val elapsedSec = (location.elapsedRealtimeNanos - anchor.elapsedRealtimeNanos) / 1e9
            if (elapsedSec > 0 && segment / elapsedSec > MAX_PLAUSIBLE_SPEED_MPS) return // GPS jump

            distanceMeters += segment
            anchorLocation = location
            updateNotification()
            publishState()

            // Save to preferences for crash recovery
            serviceScope.launch {
                preferencesManager.setActiveTripDistance(distanceMeters)
                preferencesManager.setActiveTripLastLocation(location.latitude, location.longitude)
            }
        }

        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    private fun isUsableFix(location: Location): Boolean {
        if (!location.hasAccuracy() || location.accuracy > MAX_ACCURACY_METERS) return false
        // Drop stale/cached fixes delivered when updates first start.
        val ageNanos = android.os.SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos
        return ageNanos <= MAX_FIX_AGE_NANOS
    }

    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("TripTrackingService", "onStartCommand action=${intent?.action}")
        when (intent?.action) {
            ACTION_START -> {
                vehicleId = intent.getLongExtra(EXTRA_VEHICLE_ID, -1L).takeIf { it != -1L }
                startTracking()
            }
            ACTION_PAUSE -> pauseTracking()
            ACTION_RESUME -> resumeTracking()
            ACTION_STOP -> stopTracking()
        }
        return START_STICKY
    }

    private fun startTracking() {
        Log.d("TripTrackingService", "startTracking called, isTracking=$isTracking")
        if (isTracking) return
        
        isTracking = true
        publishState()
        
        try {
            val notification = buildNotification()
            startForeground(
                NOTIFICATION_ID, 
                notification, 
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION,
            )
            requestLocationUpdates()
            Log.d("TripTrackingService", "startTracking successfully completed startForeground")
        } catch (e: Exception) {
            Log.e("TripTrackingService", "Error starting tracking foreground service synchronously", e)
            stopTracking()
            return
        }

        serviceScope.launch {
            try {
                unitSystem = preferencesManager.unitSystem.first()
                // Restore from crash if needed
                val savedDistance = preferencesManager.activeTripDistance.first()
                if (savedDistance > 0) {
                    distanceMeters = savedDistance
                    startTimeMillis = preferencesManager.activeTripStartTime.first()
                    isPaused = preferencesManager.activeTripIsPaused.first()
                    vehicleId = preferencesManager.activeTripVehicleId.first()
                } else {
                    distanceMeters = 0.0
                    startTimeMillis = System.currentTimeMillis()
                    isPaused = false
                    preferencesManager.setActiveTripStartTime(startTimeMillis)
                    vehicleId?.let { preferencesManager.setActiveTripVehicleId(it) }
                }
                publishState()
            } catch (e: Exception) {
                Log.e("TripTrackingService", "Error restoring active trip preferences", e)
            }
        }
    }
    
    private fun requestLocationUpdates() {
        try {
            // GPS only: network fixes are accurate to tens/hundreds of meters, and mixing
            // providers makes the position hop between sources, which reads as phantom distance.
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                2000L,
                5f,
                locationListener,
            )
        } catch (_: SecurityException) {
            stopSelf()
        }
    }

    private fun pauseTracking() {
        isPaused = true
        serviceScope.launch {
            preferencesManager.setActiveTripIsPaused(isPaused = true)
        }
        publishState()
        updateNotification()
    }

    private fun resumeTracking() {
        isPaused = false
        serviceScope.launch {
            preferencesManager.setActiveTripIsPaused(isPaused = false)
        }
        anchorLocation = null // Don't count the distance covered while paused
        publishState()
        updateNotification()
    }

    private fun stopTracking() {
        locationManager.removeUpdates(locationListener)
        val wasTracking = isTracking
        isTracking = false
        _tripState.value = TripState.Stopped

        val endTime = System.currentTimeMillis()
        val finalMeters = distanceMeters
        val startTime = startTimeMillis
        val tripVehicleId = vehicleId

        serviceScope.launch {
            withContext(NonCancellable) {
                if (wasTracking) {
                    saveCompletedTrip(tripVehicleId, startTime, endTime, finalMeters)
                }
                preferencesManager.clearActiveTrip()
            }
            distanceMeters = 0.0
            anchorLocation = null
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private suspend fun saveCompletedTrip(
        tripVehicleId: Long?,
        startTime: Long,
        endTime: Long,
        meters: Double,
    ) {
        val vid = tripVehicleId ?: preferencesManager.activeTripVehicleId.first()
        if (vid == null) {
            Log.w("TripTrackingService", "No vehicle for completed trip; not saving")
            return
        }
        try {
            val units = preferencesManager.unitSystem.first()
            val perUnit = if (units == UnitSystem.METRIC) METERS_PER_KM else METERS_PER_MILE
            val distance = (meters / perUnit * 100).roundToLong() / 100.0
            val trip = TripRecord(
                vehicleId = vid,
                startDate = if (startTime > 0) startTime else endTime,
                endDate = endTime,
                startOdometer = 0,
                endOdometer = 0,
                calculatedDistance = distance,
                category = TripCategory.BUSINESS,
                reimbursementRate = preferencesManager.mileageReimbursementRate.first(),
                tollExpense = 0.0,
                parkingExpense = 0.0,
                notes = "",
                isReimbursed = false,
            )
            val id = tripRepository.insertTrip(trip)
            _lastSavedTrip.value = trip.copy(id = id)
        } catch (e: Exception) {
            Log.e("TripTrackingService", "Failed to save completed trip", e)
        }
    }

    private fun publishState() {
        _tripState.value = TripState.Active(
            distanceMeters = distanceMeters,
            isPaused = isPaused,
            startTime = startTimeMillis,
            vehicleId = vehicleId,
        )
    }

    private fun buildNotification(): Notification {
        val perUnit = if (unitSystem == UnitSystem.METRIC) METERS_PER_KM else METERS_PER_MILE
        val title = if (isPaused) "Trip Paused" else "Logging Trip..."
        val content = "Distance: %.2f %s".format(distanceMeters / perUnit, unitSystem.distanceUnit)

        val mainIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingMainIntent = PendingIntent.getActivity(
            this, 0, mainIntent, PendingIntent.FLAG_IMMUTABLE,
        )

        val pauseResumeAction = if (isPaused) {
            val resumeIntent = Intent(this, TripTrackingService::class.java).apply { action = ACTION_RESUME }
            val pendingResume = PendingIntent.getService(this, 1, resumeIntent, PendingIntent.FLAG_IMMUTABLE)
            NotificationCompat.Action(0, "Resume", pendingResume)
        } else {
            val pauseIntent = Intent(this, TripTrackingService::class.java).apply { action = ACTION_PAUSE }
            val pendingPause = PendingIntent.getService(this, 2, pauseIntent, PendingIntent.FLAG_IMMUTABLE)
            NotificationCompat.Action(0, "Pause", pendingPause)
        }

        val stopIntent = Intent(this, TripTrackingService::class.java).apply { action = ACTION_STOP }
        val pendingStop = PendingIntent.getService(this, 3, stopIntent, PendingIntent.FLAG_IMMUTABLE)
        val stopAction = NotificationCompat.Action(0, "Stop & Save", pendingStop)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_shortcut_odometer) // Using an existing drawable
            .setContentIntent(pendingMainIntent)
            .addAction(pauseResumeAction)
            .addAction(stopAction)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun updateNotification() {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Trip Tracking",
            NotificationManager.IMPORTANCE_LOW, // Low importance so it doesn't beep every update
        ).apply {
            description = "Shows the current distance for active trips"
        }
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        locationManager.removeUpdates(locationListener)
        serviceScope.cancel()
    }
}
