package com.dankava.danno131313.drivesafe

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity


class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val prefs = getSharedPreferences("drivesafe", Context.MODE_PRIVATE)

        if (!prefs.contains("access_token")) {
            val audience = "audience=https://api-sandbox.safetrek.io&"
            val client_id = "client_id=" + getText(R.string.client_id) + "&"
            val scope = "scope=openid phone offline_access&"
            val state = "state=statecode&"
            val redirect_uri = "redirect_uri=drivingmode://localhost:3000/callback"

            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://account-sandbox.safetrek.io/authorize?" + audience + client_id + "response_type=code&" + scope + state + redirect_uri))
            startActivity(intent)
            finish()
        } else {
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
