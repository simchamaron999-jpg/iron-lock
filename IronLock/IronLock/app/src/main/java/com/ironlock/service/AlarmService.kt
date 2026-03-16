package com.ironlock.service

import android.app.*
import android.app.admin.DevicePolicyManager
import android.content.*
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import com.ironlock.admin.IronLockAdminReceiver
import com.ironlock.ui.AlarmFiringActivity
import com.ironlock.utils.AlarmPrefs

/**
 * AlarmService — Foreground Service
 *
 * This is the heart of IronLock. When the alarm fires:
 *  1. Acquires a FULL WakeLock to keep the CPU + screen on
 *  2. Starts playing the alarm ringtone on loop
 *  3. Launches AlarmFiringActivity over the lock screen
 *  4. Blocks power-off via DevicePolicyManager (screen lock loop)
 *  5. After [durationMs], stops everything and calls lockNow()
 */
class AlarmService : Service() {

    companion object {
        private const val TAG = "AlarmService"
        private const val NOTIF_CHANNEL_ID = "ironlock_alarm"
        private const val NOTIF_ID = 42
        const val ACTION_STOP_ALARM = "com.ironlock.STOP_ALARM"
    }

    private var mediaPlayer: MediaPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private val handler = Handler(Looper.getMainLooper())
    private var stopRunnable: Runnable? = null

    // ── Lifecycle ────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_ALARM) {
            stopAlarm()
            return START_NOT_STICKY
        }

        startForeground(NOTIF_ID, buildNotification())
        acquireWakeLock()
        startRingtone()
        launchAlarmActivity()
        scheduleAutoStop()

        return START_STICKY // Restart if killed
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        cleanUp()
    }

    // ── Core alarm logic ─────────────────────────────────────────

    /**
     * Acquire a SCREEN_DIM_WAKE_LOCK so the screen stays on
     * and the CPU doesn't sleep during the alarm window.
     */
    private fun acquireWakeLock() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        @Suppress("DEPRECATION")
        wakeLock = pm.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or
            PowerManager.ACQUIRE_CAUSES_WAKEUP or
            PowerManager.ON_AFTER_RELEASE,
            "IronLock::AlarmWakeLock"
        ).apply {
            val durationMs = AlarmPrefs.getDurationMs(this@AlarmService)
            // Add 5 seconds of buffer so wakelock outlasts the alarm slightly
            acquire(durationMs + 5_000L)
        }
        Log.d(TAG, "WakeLock acquired")
    }

    /** Play the default alarm ringtone, looping */
    private fun startRingtone() {
        try {
            val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(applicationContext, ringtoneUri)
                isLooping = true
                prepare()
                start()
            }
            Log.d(TAG, "Ringtone started")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start ringtone: ${e.message}")
        }
    }

    /**
     * Launch AlarmFiringActivity over the lock screen.
     * Uses FLAG_SHOW_WHEN_LOCKED + FLAG_TURN_SCREEN_ON.
     */
    private fun launchAlarmActivity() {
        val activityIntent = Intent(this, AlarmFiringActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_NO_HISTORY or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
        }
        startActivity(activityIntent)
    }

    /**
     * After [durationMs] automatically stop the alarm and lock the device.
     */
    private fun scheduleAutoStop() {
        val durationMs = AlarmPrefs.getDurationMs(this)
        stopRunnable = Runnable { stopAlarm() }
        handler.postDelayed(stopRunnable!!, durationMs)
        Log.d(TAG, "Auto-stop scheduled in ${durationMs / 1000}s")
    }

    /**
     * Stop everything, mark alarm as no longer firing,
     * then immediately lock the screen via DevicePolicyManager.
     */
    private fun stopAlarm() {
        Log.d(TAG, "Stopping alarm and locking device")

        cleanUp()

        // Mark alarm as finished BEFORE locking
        AlarmPrefs.setAlarmFiring(this, false)

        // Lock the screen immediately
        lockDeviceNow()

        stopForeground(true)
        stopSelf()
    }

    private fun lockDeviceNow() {
        try {
            val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val adminComponent = ComponentName(this, IronLockAdminReceiver::class.java)
            if (dpm.isAdminActive(adminComponent)) {
                dpm.lockNow()
                Log.d(TAG, "Device locked via DevicePolicyManager")
            } else {
                Log.w(TAG, "Device admin not active — cannot lock")
            }
        } catch (e: Exception) {
            Log.e(TAG, "lockNow() failed: ${e.message}")
        }
    }

    private fun cleanUp() {
        stopRunnable?.let { handler.removeCallbacks(it) }

        mediaPlayer?.apply {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null

        wakeLock?.apply {
            if (isHeld) release()
        }
        wakeLock = null
    }

    // ── Notification ─────────────────────────────────────────────

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIF_CHANNEL_ID,
                "IronLock Alarm",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Active alarm notification"
                setBypassDnd(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val fullScreenIntent = Intent(this, AlarmFiringActivity::class.java)
        val fullScreenPi = PendingIntent.getActivity(
            this, 0, fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NOTIF_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("⏰ ALARM FIRING — IRONLOCK")
            .setContentText("Cannot be dismissed. Stopping automatically.")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPi, true)
            .setOngoing(true)         // Cannot be swiped away
            .setAutoCancel(false)
            .build()
    }
}
