package com.dankava13.safedrive

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_login.*


class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val prefs = getSharedPreferences("safedrive", Context.MODE_PRIVATE)

        if (prefs.contains("access_token")) {
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
            finish()
        }

        safetrekConnectBtn.setOnClickListener {
            val client_id = "client_id=" + getText(R.string.client_id)
            val scope = "scope=openid phone offline_access"
            val state = "state=statecode"
            val redirect_uri = "redirect_uri=safedrive://localhost:3000/callback"

            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://account-sandbox.safetrek.io/authorize?response_type=code&$client_id&$scope&$state&$redirect_uri"))
            startActivity(intent)
            finish()
        }
    }
}
