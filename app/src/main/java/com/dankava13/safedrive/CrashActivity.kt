package com.dankava13.safedrive

import android.content.Context
import android.content.Intent
import android.location.Location
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Vibrator
import android.support.v7.app.AppCompatActivity
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import kotlinx.android.synthetic.main.activity_crash.*
import org.json.JSONObject

class CrashActivity : AppCompatActivity() {
    lateinit private var vibrator: Vibrator
    lateinit private var alertNoise: MediaPlayer
    lateinit private var am: AudioManager

    private val timerLength = 20 // seconds

    private var currVolume = 0

    private var timer: CountDownTimer? = object: CountDownTimer(((timerLength*1000) + 1000).toLong(), 1000) {
        override fun onTick(time: Long) {
            vibrator.vibrate(500)
            alertNoise.start()
            countDownTextView.text = getString(R.string.timeRemaining, time/1000)
        }

        override fun onFinish() {
            tryRequest()
        }

        private fun tryRequest() {
            runOnUiThread({
                countDownTextView.text = "Contacting SafeTrek..."
                secondsTextView.text = ""
                contactingTextView.text = ""
                crashButton.isEnabled = false

                val queue = Volley.newRequestQueue(baseContext)
                val prefs = getSharedPreferences("safedrive", Context.MODE_PRIVATE)

                val access_token = prefs.getString("access_token", null)
                val url = "https://api-sandbox.safetrek.io/v1/alarms"

                val crashLocation =intent.getParcelableExtra<Location>("crashLocation")

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
            })
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        stopService(Intent(this, SensorService::class.java))
        setContentView(R.layout.activity_crash)

        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        // Get audio manager and request focus on this app ONLY
        am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        am.requestAudioFocus({}, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)

        // Record current volume, then set it to maximum
        currVolume = am.getStreamVolume(AudioManager.STREAM_MUSIC)
        am.setStreamVolume(AudioManager.STREAM_MUSIC, am.getStreamMaxVolume(AudioManager.STREAM_MUSIC), AudioManager.FLAG_VIBRATE)

        alertNoise = MediaPlayer.create(this, R.raw.alert)


        crashButton.setOnClickListener {
            stopCrash()
        }

        startCrash()
    }

    override fun onBackPressed() {
        stopCrash()
    }

    private fun stopCrash() {
        SensorService.stopService = true
        am.setStreamVolume(AudioManager.STREAM_MUSIC, currVolume, 0)

        timer?.cancel()
        timer = null

        val intent = Intent(this, HomeActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
    }

    private fun startCrash() {
        timer?.start()
    }
}
