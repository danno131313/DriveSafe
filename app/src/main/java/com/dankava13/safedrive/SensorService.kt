package com.dankava13.safedrive

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.IBinder
import android.os.Build
import android.support.v4.app.NotificationCompat
import android.R.attr.name
import android.app.NotificationChannel




class SensorService : Service(), SensorEventListener {
    // Minimum amount of acceleration to register as crash (any direction)
    private val threshold = 45 // m/s^2

    // This is the value of whether or not the sensor threshold has been reached
    private var triggered = false

    private lateinit var locationManager: LocationManager
    private lateinit var locationProvider: String

    // Updated while driving, last location value before a crash is sent to the API
    private lateinit var currLocation: Location

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            updateLocation(location)
        }

        override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}

        override fun onProviderEnabled(provider: String) {}

        override fun onProviderDisabled(provider: String) {}
    }

    // Static variable so activities can stop this service externally
    companion object {
        @Volatile var stopService = false
    }

    override fun onCreate() {
        super.onCreate()

        locationManager = getSystemService(android.content.Context.LOCATION_SERVICE) as android.location.LocationManager
        val enabledProviders = locationManager.getProviders(true)

        if(enabledProviders.contains(LocationManager.GPS_PROVIDER)) {
            locationProvider = LocationManager.GPS_PROVIDER
        } else {
            locationProvider = LocationManager.NETWORK_PROVIDER
        }

        // Permissions already checked in HomeActivity, necessary redundancy
        try {
            locationManager.requestLocationUpdates(locationProvider, 0, 0.toFloat(), locationListener)
        } catch (e: SecurityException) {
        }



        // Get linear accelerometer
        val sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor: Sensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)

        // Build foreground notification (to enable running in "background")
        val intent = Intent(applicationContext, DrivingActivity::class.java)
        val requestID = System.currentTimeMillis().toInt()
        val pendingIntent: PendingIntent = PendingIntent.getActivity(this, requestID, intent, 0)
        val notification = NotificationCompat.Builder(applicationContext, "safedrive")
                .setContentTitle(getText(R.string.app_name))
                .setContentText("SafeDrive is currently monitoring your safety.")
                .setSmallIcon(R.drawable.ic_status_icon)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setAutoCancel(true)
                .build()

        if (Build.VERSION.SDK_INT >= 26) {
            val mChannel = NotificationChannel("safedrive", "SafeDrive", NotificationManager.IMPORTANCE_LOW)
            val mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            mNotificationManager.createNotificationChannel(mChannel)
        }

        startForeground(1, notification)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        val sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor: Sensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        sensorManager.unregisterListener(this, sensor)
        locationManager.removeUpdates(locationListener)
        stopService = false // Reset static variable
        super.onDestroy()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (stopService) {
            stopSelf()
        } else {
            for (reading in event.values) {
                if ((reading > threshold || reading < (-threshold)) && !triggered) {
                    val intent = Intent(this, CrashActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    triggered = true

                    // Try to send current location to crash activity, or send last known location
                    try {
                        intent.putExtra("crashLocation", currLocation)
                    } catch (e: UninitializedPropertyAccessException) {
                        val currLocation: Location
                        try {
                            currLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                            intent.putExtra("crashLocation", currLocation)
                        } catch (e: SecurityException) {
                        }
                    }
                    startActivity(intent)
                }
            }
        }
    }

    private fun updateLocation(location: Location) {
        this.currLocation = location
    }
}