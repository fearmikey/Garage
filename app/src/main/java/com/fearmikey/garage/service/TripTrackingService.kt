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
import javax.inject.Inject

@AndroidEntryPoint
class TripTrackingService : Service() {

    @Inject
    lateinit var preferencesManager: PreferencesManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var locationManager: LocationManager
    private var isTracking = false
    private var isPaused = false
    private var distanceMeters = 0.0
    private var lastLocation: Location? = null
    
    private var startTimeMillis: Long = 0L
    private var vehicleId: Long? = null

    // For simplicity, we expose a companion object state for the UI to observe instantly.
    // In a larger app, this might live in a Singleton Repository.
    companion object {
        private val _tripState = MutableStateFlow<TripState>(TripState.Stopped)
        val tripState: StateFlow<TripState> = _tripState.asStateFlow()
        
        const val ACTION_START = "ACTION_START"
        const val ACTION_PAUSE = "ACTION_PAUSE"
        const val ACTION_RESUME = "ACTION_RESUME"
        const val ACTION_STOP = "ACTION_STOP"
        const val EXTRA_VEHICLE_ID = "EXTRA_VEHICLE_ID"

        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "trip_tracking_channel"
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
            
            // Basic filtering: Ignore highly inaccurate points or points too close together
            if (location.accuracy > 30.0f) return 
            
            lastLocation?.let { last ->
                val distanceToNew = last.distanceTo(location)
                if (distanceToNew > 2.0f) { // Ignore micro-movements < 2 meters (drift)
                    distanceMeters += distanceToNew
                    updateNotification()
                    publishState()
                    
                    // Save to preferences for crash recovery
                    serviceScope.launch {
                        preferencesManager.setActiveTripDistance(distanceMeters)
                        preferencesManager.setActiveTripLastLocation(location.latitude, location.longitude)
                    }
                }
            } ?: run {
                // First location point
                serviceScope.launch {
                    preferencesManager.setActiveTripLastLocation(location.latitude, location.longitude)
                }
            }
            lastLocation = location
        }
        
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
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
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    2000L,
                    2f,
                    locationListener,
                )
            }
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    2000L,
                    2f,
                    locationListener,
                )
            }
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
        lastLocation = null // Reset last location to prevent jumping lines
        publishState()
        updateNotification()
    }

    private fun stopTracking() {
        locationManager.removeUpdates(locationListener)
        isTracking = false
        _tripState.value = TripState.Stopped
        
        serviceScope.launch {
            preferencesManager.clearActiveTrip()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
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
        val miles = distanceMeters / 1609.34 // Conversion. Assuming miles for now.
        val title = if (isPaused) "Trip Paused" else "Logging Trip..."
        val content = "Distance: %.2f mi".format(miles)

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
