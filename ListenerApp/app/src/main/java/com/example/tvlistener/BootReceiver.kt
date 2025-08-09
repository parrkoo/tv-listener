package com.example.tvlistener

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            val prefs: SharedPreferences = context.getSharedPreferences("tvlistener", Context.MODE_PRIVATE)
            val ip = prefs.getString("server_ip", "") ?: ""
            if (ip.isNotEmpty()) {
                val svc = Intent(context, ListenerService::class.java)
                context.startForegroundService(svc)
            }
        }
    }
}
