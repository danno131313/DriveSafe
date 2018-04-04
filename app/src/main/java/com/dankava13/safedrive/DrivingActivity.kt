package com.dankava13.safedrive

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import kotlinx.android.synthetic.main.activity_driving.*
import org.json.JSONObject

class DrivingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driving)
        stopDrivingButton.setOnClickListener {
            stopDriving()
        }

        emergencyButton.setOnClickListener {
            val dialog = AlertDialog.Builder(this, R.style.Theme_AppCompat_Light_Dialog_Alert)
            dialog.setTitle("Send Emergency Services")
                .setMessage("This will send your location and contact SafeTrek\nto send emergency services.\nAre you sure?")
                .setPositiveButton("Yes", { _, _ ->
                    sendCrashReport()
                })
                .setNegativeButton("No", { _, _ ->
                })
            dialog.show()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        stopDriving()
    }

    private fun stopDriving() {
        SensorService.stopService = true
        finish()
    }

    private fun sendCrashReport() {
        val locationManager = getSystemService(android.content.Context.LOCATION_SERVICE) as android.location.LocationManager

        val locationProvider: String
        val enabledProviders = locationManager.getProviders(true)

        if(enabledProviders.contains(LocationManager.GPS_PROVIDER)) {
            locationProvider = LocationManager.GPS_PROVIDER
        } else {
            locationProvider = LocationManager.NETWORK_PROVIDER
        }

        val crashLocation = locationManager.getLastKnownLocation(locationProvider) // Permission is already checked

        val queue = Volley.newRequestQueue(baseContext)
        val prefs = getSharedPreferences("safedrive", Context.MODE_PRIVATE)

        val access_token = prefs.getString("access_token", null)
        val url = "https://api-sandbox.safetrek.io/v1/alarms"

        val lat = crashLocation.latitude
        val lng = crashLocation.longitude
        val accuracy = crashLocation.accuracy.toInt()

        val jsonObj = JSONObject("{ " +
                "'location.coordinates': {'lat': $lat, 'lng': $lng, 'accuracy': $accuracy } " +
                "}")

        val tokenRequest = object : JsonObjectRequest(Request.Method.POST, url, jsonObj,
                Response.Listener { response ->
                    val intent = Intent(baseContext, EmergencyActivity::class.java)
                    intent.putExtra("alarmId", response.get("id").toString())
                    intent.putExtra("crashLocation", crashLocation)
                    startActivity(intent)
                },
                Response.ErrorListener { _ ->
                }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Content-Type"] = "application/json"
                headers["Authorization"] = "Bearer $access_token"
                return headers
            }
        }

        tokenRequest.retryPolicy = DefaultRetryPolicy(7000, 10, 0.toFloat())

        queue.add(tokenRequest)
    }
}
