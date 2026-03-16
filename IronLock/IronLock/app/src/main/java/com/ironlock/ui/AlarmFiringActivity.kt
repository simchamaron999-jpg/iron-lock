package com.ironlock.ui

import android.os.*
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.ironlock.R
import com.ironlock.utils.AlarmPrefs
import java.util.Locale

/**
 * AlarmFiringActivity
 *
 * Displayed over the lock screen while the alarm is ringing.
 * Shows a live countdown. Back button and home gesture are blocked.
 * The activity finishes only when AlarmService stops it.
 */
class AlarmFiringActivity : AppCompatActivity() {

    private lateinit var tvTime: TextView
    private lateinit var tvCountdown: TextView
    private lateinit var tvStatus: TextView

    private val handler = Handler(Looper.getMainLooper())
    private var countdownRunnable: Runnable? = null
    private var endTimeMs: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Show over lock screen and turn screen on
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }

        // Always keep screen on
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContentView(R.layout.activity_alarm_firing)

        tvTime      = findViewById(R.id.tv_alarm_time)
        tvCountdown = findViewById(R.id.tv_countdown)
        tvStatus    = findViewById(R.id.tv_status)

        // Display the time this alarm was set for
        val alarmTimeMs = AlarmPrefs.getAlarmTimeMs(this)
        tvTime.text = formatTime(alarmTimeMs)

        // Calculate end time
        val durationMs = AlarmPrefs.getDurationMs(this)
        endTimeMs = System.currentTimeMillis() + durationMs

        startCountdown()
    }

    private fun startCountdown() {
        countdownRunnable = object : Runnable {
            override fun run() {
                val remaining = endTimeMs - System.currentTimeMillis()
                if (remaining <= 0) {
                    tvCountdown.text = "00:00"
                    tvStatus.text = "Locking device..."
                    // AlarmService will call finish / lock soon
                    return
                }
                val mins = (remaining / 60000).toInt()
                val secs = ((remaining % 60000) / 1000).toInt()
                tvCountdown.text = String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
                handler.postDelayed(this, 500)
            }
        }
        handler.post(countdownRunnable!!)
    }

    // ── Block back / home navigation ─────────────────────────────

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Do nothing — back is blocked during alarm
    }

    override fun onUserLeaveHint() {
        // Re-launch ourselves if user tries to go home
        if (AlarmPrefs.isAlarmCurrentlyFiring(this)) {
            startActivity(intent)
        }
    }

    override fun onPause() {
        super.onPause()
        // If alarm is still firing and we lose focus, come back to front
        if (AlarmPrefs.isAlarmCurrentlyFiring(this)) {
            val bringToFront = intent.apply {
                addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            }
            startActivity(bringToFront)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        countdownRunnable?.let { handler.removeCallbacks(it) }
    }

    // ── Helpers ──────────────────────────────────────────────────

    private fun formatTime(epochMs: Long): String {
        if (epochMs <= 0) return "--:--"
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = epochMs }
        val h = cal.get(java.util.Calendar.HOUR_OF_DAY)
        val m = cal.get(java.util.Calendar.MINUTE)
        return String.format(Locale.getDefault(), "%02d:%02d", h, m)
    }
}
