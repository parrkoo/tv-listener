package com.example.tvlistener

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import android.content.Context

class MainActivity : AppCompatActivity() {

    private lateinit var edtIp: EditText
    private lateinit var btnSave: Button
    private lateinit var btnStart: Button
    private lateinit var btnStop: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        edtIp = findViewById(R.id.edtServerIp)
        btnSave = findViewById(R.id.btnSave)
        btnStart = findViewById(R.id.btnStart)
        btnStop = findViewById(R.id.btnStop)

        val prefs = getSharedPreferences("tvlistener", Context.MODE_PRIVATE)
        edtIp.setText(prefs.getString("server_ip", ""))

        btnSave.setOnClickListener {
            prefs.edit().putString("server_ip", edtIp.text.toString().trim()).apply()
        }

        btnStart.setOnClickListener {
            startService(Intent(this, ListenerService::class.java))
        }

        btnStop.setOnClickListener {
            stopService(Intent(this, ListenerService::class.java))
        }
    }
}
