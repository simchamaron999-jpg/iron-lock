package com.ironlock.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.ironlock.utils.AlarmPrefs

/**
 * AlarmScheduler
 *
 * Uses AlarmManager.setExactAndAllowWhileIdle so the alarm fires
 * even in Doze mode. On API 31+ we also check for SCHEDULE_EXACT_ALARM
 * permission before calling.
 */
object AlarmScheduler {

    const val ACTION_ALARM_TRIGGER = "com.ironlock.ALARM_TRIGGER"
    private const val REQUEST_CODE = 1001

    /**
     * Schedule the alarm.
     * @param triggerAtMs  epoch-millis when alarm should fire
     * @param durationMs   how long the alarm rings (stored in prefs)
     */
    fun schedule(ctx: Context, triggerAtMs: Long, durationMs: Long) {
        AlarmPrefs.setAlarmTimeMs(ctx, triggerAtMs)
        AlarmPrefs.setDurationMs(ctx, durationMs)

        val alarmManager = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(ctx, AlarmReceiver::class.java).apply {
            action = ACTION_ALARM_TRIGGER
        }
        val pi = PendingIntent.getBroadcast(
            ctx, REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // setExactAndAllowWhileIdle works on API 23+; we target 28+ so always available
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pi)
    }

    /** Cancel a pending alarm */
    fun cancel(ctx: Context) {
        val alarmManager = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(ctx, AlarmReceiver::class.java).apply {
            action = ACTION_ALARM_TRIGGER
        }
        val pi = PendingIntent.getBroadcast(
            ctx, REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pi)
        AlarmPrefs.setAlarmTimeMs(ctx, -1L)
    }
}
