package com.example.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.R
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

/**
 * User-visible foreground location sharing while an accepted order is being delivered.
 * The service is stopped as soon as the tracking document becomes completed/cancelled.
 */
class CustomerLocationService : Service() {
    private val fused by lazy { LocationServices.getFusedLocationProviderClient(this) }
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private var callback: LocationCallback? = null
    private var statusListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var token: String? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSharing()
            return START_NOT_STICKY
        }
        token = intent?.getStringExtra(EXTRA_TOKEN)
            ?: getSharedPreferences(PREFS, MODE_PRIVATE).getString(KEY_TOKEN, null)
        val currentToken = token?.takeIf { it.isNotBlank() } ?: run {
            stopSelf()
            return START_NOT_STICKY
        }

        // Android requires the foreground notification immediately after service start.
        startForeground(NOTIFICATION_ID, buildNotification())
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            stopSelf()
            return START_NOT_STICKY
        }
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString(KEY_TOKEN, currentToken).apply()
        listenForCompletion(currentToken)
        startLocationUpdates(currentToken)
        return START_STICKY
    }

    private fun listenForCompletion(currentToken: String) {
        statusListener?.remove()
        statusListener = firestore.collection("orderTracking").document(currentToken)
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null && snapshot.exists()) {
                    val status = snapshot.getString("status")
                    if (status == "completed" || status == "cancelled") stopSharing()
                }
            }
    }

    private fun startLocationUpdates(currentToken: String) {
        if (callback != null) return
        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                if (!location.latitude.isFinite() || !location.longitude.isFinite()) return
                firestore.collection("orderTracking").document(currentToken).update(
                    mapOf(
                        "customerLatitude" to location.latitude,
                        "customerLongitude" to location.longitude,
                        "customerLocationSharing" to true,
                        "customerLocationUpdatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
        }
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10_000L)
            .setMinUpdateDistanceMeters(10f)
            .build()
        try {
            fused.requestLocationUpdates(request, locationCallback, mainLooper)
            callback = locationCallback
        } catch (_: SecurityException) {
            stopSelf()
        }
    }

    private fun stopSharing() {
        callback?.let { fused.removeLocationUpdates(it) }
        callback = null
        statusListener?.remove()
        statusListener = null
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().remove(KEY_TOKEN).apply()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("Annapurna order location")
            .setContentText("Delivery partner साठी तुमचे live location share होत आहे")
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Order live location",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Live location sharing while your order is being delivered" }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        callback?.let { fused.removeLocationUpdates(it) }
        callback = null
        statusListener?.remove()
        statusListener = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "com.example.service.CustomerLocationService.START"
        const val ACTION_STOP = "com.example.service.CustomerLocationService.STOP"
        const val EXTRA_TOKEN = "tracking_token"
        private const val PREFS = "annapurna_customer_location_service"
        private const val KEY_TOKEN = "active_tracking_token"
        private const val CHANNEL_ID = "annapurna_order_location"
        private const val NOTIFICATION_ID = 7312
    }
}
