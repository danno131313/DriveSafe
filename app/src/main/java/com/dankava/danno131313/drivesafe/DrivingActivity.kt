package com.dankava.danno131313.drivesafe

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_driving.*

class DrivingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driving)
        stopDrivingButton.setOnClickListener {
            stopDriving()
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
}
