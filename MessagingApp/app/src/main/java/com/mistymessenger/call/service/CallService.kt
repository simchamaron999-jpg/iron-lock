package com.mistymessenger.call.service

import android.app.Service
import android.content.Intent
import android.os.IBinder

// Phase 6: This service manages the WebRTC PeerConnection lifecycle as a foreground service
// so calls persist when the user navigates away from the call screen.
class CallService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY
}
