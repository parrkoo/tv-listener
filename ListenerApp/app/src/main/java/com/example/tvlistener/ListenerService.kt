package com.example.tvlistener

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.Context
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import okhttp3.*
import okio.ByteString
import java.util.concurrent.TimeUnit

class ListenerService : Service() {

    private var ws: WebSocket? = null
    private lateinit var prefs: android.content.SharedPreferences
    private val client by lazy {
        OkHttpClient.Builder()
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .build()
    }

    override fun onCreate() {
        super.onCreate()
        prefs = getSharedPreferences("tvlistener", Context.MODE_PRIVATE)
        startForegroundServiceNotification()
        connect()
    }

    private fun startForegroundServiceNotification() {
        val channelId = "tv_listener_channel"
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val chan = NotificationChannel(channelId, "TV Listener", NotificationManager.IMPORTANCE_LOW)
            nm.createNotificationChannel(chan)
        }

        val intent = Intent(this, MainActivity::class.java)
        val pi = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("TV Listener")
            .setContentText("Connected to server")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pi)
            .build()

        startForeground(1, notification)
    }

    private fun connect() {
        val serverIp = prefs.getString("server_ip", "") ?: ""
        if (serverIp.isEmpty()) return
        val url = if (serverIp.startsWith("ws://") || serverIp.startsWith("wss://")) serverIp else "ws://$serverIp:6789"

        val request = Request.Builder().url(url).build()
        ws = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                // connected
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleCommand(text)
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                // ignore
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                // schedule reconnect
                webSocket.cancel()
                reconnectWithDelay()
            }
        })
    }

    private fun reconnectWithDelay() {
        val handler = android.os.Handler(mainLooper)
        handler.postDelayed({ connect() }, 5000)
    }

    private fun handleCommand(cmd: String) {
        when (cmd.trim().lowercase()) {
            "youtube" -> {
                val pm = packageManager
                val launch = pm.getLaunchIntentForPackage("com.google.android.youtube.tv")
                    ?: pm.getLaunchIntentForPackage("com.google.android.youtube")
                if (launch != null) {
                    launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(launch)
                } else {
                    val i = Intent(Intent.ACTION_VIEW)
                    i.data = android.net.Uri.parse("https://www.youtube.com")
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(i)
                }
            }
            else -> {
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (ws == null) connect()
        return START_STICKY
    }

    override fun onDestroy() {
        ws?.close(1000, null)
        client.dispatcher.executorService.shutdown()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
