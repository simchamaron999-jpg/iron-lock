package com.ironlock.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ironlock.utils.AlarmPrefs

/**
 * BootReceiver
 *
 * AlarmManager alarms are wiped on reboot.
 * This receiver fires on BOOT_COMPLETED and reschedules
 * any alarm that was set before the reboot.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        // Clear any "firing" flag that may have been stuck before reboot
        AlarmPrefs.setAlarmFiring(context, false)

        // If the user had an alarm scheduled, reschedule it
        val alarmTimeMs = AlarmPrefs.getAlarmTimeMs(context)
        val durationMs  = AlarmPrefs.getDurationMs(context)

        if (alarmTimeMs > System.currentTimeMillis()) {
            // Alarm is still in the future — reschedule it
            AlarmScheduler.schedule(context, alarmTimeMs, durationMs)
        } else if (alarmTimeMs > 0) {
            // Alarm time passed while device was off — clear it
            AlarmPrefs.setAlarmTimeMs(context, -1L)
        }
    }
}
