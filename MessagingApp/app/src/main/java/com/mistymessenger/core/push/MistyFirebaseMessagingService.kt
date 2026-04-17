package com.mistymessenger.core.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.mistymessenger.MainActivity
import com.mistymessenger.R

class MistyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        // Phase 2: POST token to backend for this user
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        val chatId = data["chatId"] ?: return
        val senderName = data["senderName"] ?: "New message"
        val content = data["content"] ?: ""
        val type = data["type"] ?: "text"

        showNotification(chatId, senderName, content, type)
    }

    private fun showNotification(chatId: String, title: String, body: String, type: String) {
        val channelId = "chat_$chatId"
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        nm.createNotificationChannel(
            NotificationChannel(channelId, title, NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Messages from $title"
            }
        )

        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("chatId", chatId)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pi = PendingIntent.getActivity(this, chatId.hashCode(), intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val displayBody = when (type) {
            "image" -> "Sent a photo"
            "video" -> "Sent a video"
            "audio" -> "Voice message"
            "document" -> "Sent a document"
            else -> body
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(displayBody)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        nm.notify(chatId.hashCode(), notification)
    }
}
