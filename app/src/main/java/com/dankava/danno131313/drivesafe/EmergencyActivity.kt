package com.dankava.danno131313.drivesafe

import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import kotlinx.android.synthetic.main.activity_emergency.*
import org.json.JSONObject

class EmergencyActivity : AppCompatActivity() {
    lateinit var alarmId: String
    lateinit var currLocation: Location
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_emergency)
        alarmId = intent.getStringExtra("alarmId")
        currLocation = intent.getParcelableExtra<Location>("crashLocation")
        slideView.setOnSlideCompleteListener {
            cancelAlarm()
        }
    }

    private fun cancelAlarm() {
        val queue = Volley.newRequestQueue(baseContext)
        val prefs = getSharedPreferences("drivesafe", Context.MODE_PRIVATE)
        val access_token = prefs.getString("access_token", null)
        val url = "https://api-sandbox.safetrek.io/v1/alarms/" + alarmId + "/status"

        val jsonObj = JSONObject("{ 'status': 'CANCELED' }")

        val tokenRequest = object : JsonObjectRequest(Request.Method.PUT, url, jsonObj,
                Response.Listener { response ->
                    Log.d("API Cancel Alarm Response", response.toString())
                },
                Response.ErrorListener { error ->
                    Log.d("ERROR", error.message)
                }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                var headers = HashMap<String, String>()
                headers["Content-Type"] = "application/json"
                headers["Authorization"] = "Bearer " + access_token
                return headers
            }
        }

        queue.add(tokenRequest)

        val intent = Intent(this, HomeActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
    }
}
