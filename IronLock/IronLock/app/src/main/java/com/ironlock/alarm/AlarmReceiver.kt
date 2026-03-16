package com.ironlock.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.ironlock.service.AlarmService
import com.ironlock.utils.AlarmPrefs

/**
 * AlarmReceiver
 *
 * Woken up by AlarmManager when the scheduled time arrives.
 * Immediately hands off to AlarmService (foreground service) which
 * handles audio, wake lock, screen-on, and the countdown.
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AlarmScheduler.ACTION_ALARM_TRIGGER) return

        // Mark alarm as currently firing — this is what blocks admin removal
        AlarmPrefs.setAlarmFiring(context, true)

        val serviceIntent = Intent(context, AlarmService::class.java)
        // Start as foreground service so Android won't kill it
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }
}
