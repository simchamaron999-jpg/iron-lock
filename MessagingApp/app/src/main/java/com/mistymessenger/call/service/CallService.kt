package com.mistymessenger.call.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.mistymessenger.MainActivity
import com.mistymessenger.R

/**
 * Foreground service that keeps the call session alive when the app is
 * backgrounded. Holds no state itself — WebRtcManager owns the PeerConnection.
 */
class CallService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val peerName = intent?.getStringExtra(EXTRA_PEER_NAME).orEmpty().ifBlank { "Ongoing call" }
        val isVideo = intent?.getBooleanExtra(EXTRA_IS_VIDEO, false) == true

        startForegroundNotification(peerName, isVideo)
        return START_STICKY
    }

    private fun startForegroundNotification(peerName: String, isVideo: Boolean) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Ongoing calls", NotificationManager.IMPORTANCE_LOW
            )
            nm.createNotificationChannel(channel)
        }

        val openIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(if (isVideo) "Video call with $peerName" else "Voice call with $peerName")
            .setContentText("Tap to return to call")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setContentIntent(openIntent)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val type = ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE or
                (if (isVideo) ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA else 0)
            startForeground(NOTIFICATION_ID, notification, type)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    companion object {
        private const val CHANNEL_ID = "misty_call_channel"
        private const val NOTIFICATION_ID = 42
        const val EXTRA_PEER_NAME = "peer_name"
        const val EXTRA_IS_VIDEO = "is_video"

        fun start(context: Context, peerName: String, isVideo: Boolean) {
            val intent = Intent(context, CallService::class.java).apply {
                putExtra(EXTRA_PEER_NAME, peerName)
                putExtra(EXTRA_IS_VIDEO, isVideo)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, CallService::class.java))
        }
    }
}
