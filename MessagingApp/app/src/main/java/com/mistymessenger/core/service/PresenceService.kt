package com.mistymessenger.core.service

import com.mistymessenger.core.db.dao.UserDao
import com.mistymessenger.core.network.SocketManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PresenceService @Inject constructor(
    private val socketManager: SocketManager,
    private val userDao: UserDao
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun start() {
        // Listen to presence broadcasts
        scope.launch {
            socketManager.listenFlow("user:presence").collect { json ->
                val userId = json.optString("userId") ?: return@collect
                val isOnline = json.optBoolean("isOnline", false)
                val lastSeenStr = json.optString("lastSeen")
                val lastSeenMs = parseIsoToMs(lastSeenStr)
                userDao.updatePresence(userId, isOnline, lastSeenMs)
            }
        }

        // Heartbeat every 30 seconds to keep lastSeen fresh
        scope.launch {
            while (isActive) {
                delay(30_000)
                if (socketManager.isConnected) {
                    socketManager.emit("heartbeat")
                }
            }
        }
    }

    fun stop() {
        scope.cancel()
    }

    private fun parseIsoToMs(iso: String?): Long {
        if (iso.isNullOrBlank()) return System.currentTimeMillis()
        return try {
            java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
                .parse(iso)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) { System.currentTimeMillis() }
    }
}
