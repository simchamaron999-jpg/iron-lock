package com.ironlock.utils

import android.content.Context

/**
 * AlarmPrefs
 * Central store for all alarm & admin state, persisted across reboots.
 */
object AlarmPrefs {

    private const val PREFS_NAME = "ironlock_prefs"

    // Keys
    private const val KEY_ALARM_FIRING   = "alarm_firing"
    private const val KEY_ALARM_TIME_MS  = "alarm_time_ms"
    private const val KEY_DURATION_MS    = "alarm_duration_ms"
    private const val KEY_ADMIN_ACTIVE   = "admin_active"
    private const val KEY_BLOCK_POWER    = "block_power_off"
    private const val KEY_SCREEN_ON      = "force_screen_on"

    private fun prefs(ctx: Context) =
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ── Alarm firing state ──────────────────────────────────────

    fun setAlarmFiring(ctx: Context, firing: Boolean) =
        prefs(ctx).edit().putBoolean(KEY_ALARM_FIRING, firing).apply()

    fun isAlarmCurrentlyFiring(ctx: Context): Boolean =
        prefs(ctx).getBoolean(KEY_ALARM_FIRING, false)

    // ── Scheduled alarm ─────────────────────────────────────────

    /** Store alarm trigger time in epoch millis */
    fun setAlarmTimeMs(ctx: Context, timeMs: Long) =
        prefs(ctx).edit().putLong(KEY_ALARM_TIME_MS, timeMs).apply()

    fun getAlarmTimeMs(ctx: Context): Long =
        prefs(ctx).getLong(KEY_ALARM_TIME_MS, -1L)

    /** Store ring duration in millis (default 5 minutes) */
    fun setDurationMs(ctx: Context, durationMs: Long) =
        prefs(ctx).edit().putLong(KEY_DURATION_MS, durationMs).apply()

    fun getDurationMs(ctx: Context): Long =
        prefs(ctx).getLong(KEY_DURATION_MS, 5 * 60 * 1000L)

    // ── Admin state ──────────────────────────────────────────────

    fun setAdminActive(ctx: Context, active: Boolean) =
        prefs(ctx).edit().putBoolean(KEY_ADMIN_ACTIVE, active).apply()

    fun clearAdminActive(ctx: Context) = setAdminActive(ctx, false)

    fun isAdminActive(ctx: Context): Boolean =
        prefs(ctx).getBoolean(KEY_ADMIN_ACTIVE, false)

    // ── Feature toggles ──────────────────────────────────────────

    fun setBlockPowerOff(ctx: Context, block: Boolean) =
        prefs(ctx).edit().putBoolean(KEY_BLOCK_POWER, block).apply()

    fun shouldBlockPowerOff(ctx: Context): Boolean =
        prefs(ctx).getBoolean(KEY_BLOCK_POWER, true)

    fun setForceScreenOn(ctx: Context, force: Boolean) =
        prefs(ctx).edit().putBoolean(KEY_SCREEN_ON, force).apply()

    fun shouldForceScreenOn(ctx: Context): Boolean =
        prefs(ctx).getBoolean(KEY_SCREEN_ON, true)
}
