package com.ironlock.admin

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.ironlock.service.AlarmService
import com.ironlock.utils.AlarmPrefs

/**
 * IronLockAdminReceiver
 *
 * Handles Device Admin lifecycle events.
 * The critical protection: if the alarm is currently firing,
 * we block the disable request and re-arm admin immediately.
 */
class IronLockAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        Toast.makeText(context, "IronLock: Device Admin ACTIVATED 🔒", Toast.LENGTH_LONG).show()
    }

    /**
     * Called when the user tries to remove IronLock as a device admin.
     * If an alarm is currently firing — block it by immediately re-locking the device.
     */
    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        return if (AlarmPrefs.isAlarmCurrentlyFiring(context)) {
            // Re-lock immediately so the settings screen becomes inaccessible
            val dpm = getManager(context)
            dpm.lockNow()
            "⛔ IronLock cannot be disabled while an alarm is active. Wait for the alarm to finish."
        } else {
            "Are you sure you want to disable IronLock Device Admin? Alarm enforcement will stop."
        }
    }

    override fun onDisabled(context: Context, intent: Intent) {
        Toast.makeText(context, "IronLock: Device Admin removed.", Toast.LENGTH_LONG).show()
        AlarmPrefs.clearAdminActive(context)
    }

    override fun onPasswordFailed(context: Context, intent: Intent) {
        // Optional: log failed unlock attempts during alarm
    }
}
