package com.dankava.danno131313.drivesafe

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import kotlinx.android.synthetic.main.activity_home.*
import org.json.JSONObject


class HomeActivity : AppCompatActivity() {
    private lateinit var locationManager: LocationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request location permissions for app (should only happen first time using app)
        if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED || checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 0)
        }

        // Disallow use of app if no location services are enabled
        locationManager = getSystemService(android.content.Context.LOCATION_SERVICE) as android.location.LocationManager
        if (!locationEnabled()) {
            showLocationAlert()
        }

        val prefs = getSharedPreferences("drivesafe", Context.MODE_PRIVATE)

        // If coming from authorization attempt, grab a new access token
        try {
            val authCode = intent.getData().getQueryParameters("code")[0] // Fails if not coming from authorization webview
            getNewToken(prefs, authCode)
        } catch (e: Exception) {

            // Otherwise use refresh_token to get a fresh one
            refreshToken(prefs)
        }

        setContentView(R.layout.activity_home)
        drivingButton.setOnClickListener {
            startDriving()
        }
    }

    override fun onResume() {
        super.onResume()
        drivingButton.setOnClickListener {
            startDriving()
        }
    }

    private fun startDriving() {
        val intent = Intent(this, DrivingActivity::class.java)
        val service = Intent(this, SensorService::class.java)
        SensorService.stopService = false
        startService(service)
        startActivity(intent)
    }

    private fun getNewToken(prefs: SharedPreferences, authCode: String) {
        val queue = Volley.newRequestQueue(this)
        val url = "https://login-sandbox.safetrek.io/oauth/token"

        val jsonObj = JSONObject("{ 'grant_type': 'authorization_code', 'code': '" + authCode + "', " +
                "'client_id': '" + getText(R.string.client_id) + "', " +
                "'client_secret': '" + getText(R.string.client_secret) + "', " +
                "'redirect_uri': 'drivingmode://localhost:3000/callback' }")

        val tokenRequest = JsonObjectRequest(Request.Method.POST, url, jsonObj,
                Response.Listener { response ->
                    val access_token = response.getString("access_token")
                    val refresh_token = response.getString("refresh_token")
                    prefs.edit().putString("access_token", access_token)
                            .putString("refresh_token", refresh_token)
                            .apply()
                },
                Response.ErrorListener { error ->
                    Log.d("ERROR", String(error.networkResponse.data))
                    showAPIError()
                }
        )

        queue.add(tokenRequest)
    }

    private fun refreshToken(prefs: SharedPreferences) {
        val queue = Volley.newRequestQueue(this)
        val url = "https://login-sandbox.safetrek.io/oauth/token"
        val refresh_token = prefs.getString("refresh_token", null)

        val jsonObj = JSONObject("{ 'grant_type': 'refresh_token', 'refresh_token': '" + refresh_token + "', " +
                "'client_id': '" + getText(R.string.client_id) + "', " +
                "'client_secret': '" + getText(R.string.client_secret) + "' }")

        Log.d("REFRESHTOKENJSON", jsonObj.toString())

        val tokenRequest = JsonObjectRequest(Request.Method.POST, url, jsonObj,
                Response.Listener { response ->
                    val access_token = response.getString("access_token")
                    prefs.edit().putString("access_token", access_token)
                            .apply()
                },
                Response.ErrorListener { error ->
                    Log.d("ERROR", String(error.networkResponse.data))
                    showAPIError()
                }
        )

        queue.add(tokenRequest)
    }

    private fun locationEnabled(): Boolean {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun showLocationAlert() {
        val dialog = AlertDialog.Builder(this)
        dialog.setTitle("Enable Location")
            .setMessage("Your Locations Settings is set to 'Off'.\nPlease Enable Location to " + "use this app")
            .setPositiveButton("Open Location Settings", DialogInterface.OnClickListener { paramDialogInterface, paramInt ->
                val myIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(myIntent)
            })
            .setNegativeButton("Close App", DialogInterface.OnClickListener { paramDialogInterface, paramInt ->
                val intent = Intent(Intent.ACTION_MAIN)
                intent.addCategory(Intent.CATEGORY_HOME)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
            })
        dialog.show()
    }

    private fun showAPIError() {
        val dialog = AlertDialog.Builder(this)
        dialog.setTitle("SafeTrek Unreachable")
                .setMessage("The SafeTrek API is currently unavailable.\nPlease try again soon.")
                .setPositiveButton("OK", DialogInterface.OnClickListener { paramDialogInterface, paramInt ->
                    val intent = Intent(Intent.ACTION_MAIN)
                    intent.addCategory(Intent.CATEGORY_HOME)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                    finish()
                })
        dialog.show()
    }
}
