package com.ironlock.ui

import android.app.TimePickerDialog
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.ironlock.R
import com.ironlock.admin.IronLockAdminReceiver
import com.ironlock.alarm.AlarmScheduler
import com.ironlock.utils.AlarmPrefs
import java.util.*

/**
 * MainActivity — Dashboard
 *
 * Handles:
 *  • Device Admin activation flow
 *  • Time picker for alarm
 *  • Duration selection
 *  • Arming/disarming the alarm
 *  • Showing current admin feature status
 */
class MainActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_ENABLE_ADMIN = 1
    }

    private lateinit var dpm: DevicePolicyManager
    private lateinit var adminComponent: ComponentName

    // UI references
    private lateinit var btnActivateAdmin: Button
    private lateinit var tvAdminStatus: TextView
    private lateinit var tvAlarmTime: TextView
    private lateinit var btnPickTime: Button
    private lateinit var rgDuration: RadioGroup
    private lateinit var btnArmAlarm: Button
    private lateinit var tvNextAlarm: TextView
    private lateinit var switchBlockPower: Switch
    private lateinit var switchScreenOn: Switch

    private var selectedHour   = 6
    private var selectedMinute = 30
    private var selectedDurationMs = 5 * 60 * 1000L // default 5 min

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dpm            = getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager
        adminComponent = ComponentName(this, IronLockAdminReceiver::class.java)

        bindViews()
        setupListeners()
        refreshUI()
    }

    override fun onResume() {
        super.onResume()
        refreshUI()
    }

    // ── View binding ─────────────────────────────────────────────

    private fun bindViews() {
        btnActivateAdmin = findViewById(R.id.btn_activate_admin)
        tvAdminStatus    = findViewById(R.id.tv_admin_status)
        tvAlarmTime      = findViewById(R.id.tv_alarm_time)
        btnPickTime      = findViewById(R.id.btn_pick_time)
        rgDuration       = findViewById(R.id.rg_duration)
        btnArmAlarm      = findViewById(R.id.btn_arm_alarm)
        tvNextAlarm      = findViewById(R.id.tv_next_alarm)
        switchBlockPower = findViewById(R.id.switch_block_power)
        switchScreenOn   = findViewById(R.id.switch_screen_on)
    }

    // ── Listeners ────────────────────────────────────────────────

    private fun setupListeners() {

        btnActivateAdmin.setOnClickListener {
            if (dpm.isAdminActive(adminComponent)) {
                Toast.makeText(this, "Admin already active!", Toast.LENGTH_SHORT).show()
            } else {
                val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                    putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                    putExtra(
                        DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                        "IronLock needs Device Admin to lock the device and enforce unstoppable alarms."
                    )
                }
                startActivityForResult(intent, REQUEST_ENABLE_ADMIN)
            }
        }

        btnPickTime.setOnClickListener {
            TimePickerDialog(this, { _, hour, minute ->
                selectedHour   = hour
                selectedMinute = minute
                tvAlarmTime.text = String.format("%02d:%02d", hour, minute)
            }, selectedHour, selectedMinute, true).show()
        }

        rgDuration.setOnCheckedChangeListener { _, checkedId ->
            selectedDurationMs = when (checkedId) {
                R.id.rb_1min  -> 1 * 60 * 1000L
                R.id.rb_3min  -> 3 * 60 * 1000L
                R.id.rb_5min  -> 5 * 60 * 1000L
                R.id.rb_10min -> 10 * 60 * 1000L
                else          -> 5 * 60 * 1000L
            }
        }

        btnArmAlarm.setOnClickListener {
            if (!dpm.isAdminActive(adminComponent)) {
                Toast.makeText(this, "⚠️ Activate Device Admin first!", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            armAlarm()
        }

        switchBlockPower.setOnCheckedChangeListener { _, checked ->
            AlarmPrefs.setBlockPowerOff(this, checked)
        }

        switchScreenOn.setOnCheckedChangeListener { _, checked ->
            AlarmPrefs.setForceScreenOn(this, checked)
        }
    }

    // ── Alarm arming ─────────────────────────────────────────────

    private fun armAlarm() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, selectedHour)
            set(Calendar.MINUTE, selectedMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            // If time already passed today, schedule for tomorrow
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        AlarmScheduler.schedule(this, cal.timeInMillis, selectedDurationMs)
        Toast.makeText(this, "✅ Alarm armed for ${String.format("%02d:%02d", selectedHour, selectedMinute)}", Toast.LENGTH_LONG).show()
        refreshUI()
    }

    // ── UI refresh ───────────────────────────────────────────────

    private fun refreshUI() {
        val adminActive = dpm.isAdminActive(adminComponent)

        tvAdminStatus.text = if (adminActive) "🟢 Device Admin: ACTIVE" else "🔴 Device Admin: NOT ACTIVE"
        btnActivateAdmin.text = if (adminActive) "Admin Active ✓" else "Activate Device Admin"
        btnActivateAdmin.isEnabled = !adminActive

        switchBlockPower.isChecked = AlarmPrefs.shouldBlockPowerOff(this)
        switchScreenOn.isChecked   = AlarmPrefs.shouldForceScreenOn(this)

        val nextAlarmMs = AlarmPrefs.getAlarmTimeMs(this)
        tvNextAlarm.text = if (nextAlarmMs > System.currentTimeMillis()) {
            val cal = Calendar.getInstance().apply { timeInMillis = nextAlarmMs }
            "Next alarm: ${String.format("%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))}"
        } else {
            "No alarm set"
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ENABLE_ADMIN) {
            refreshUI()
            if (dpm.isAdminActive(adminComponent)) {
                AlarmPrefs.setAdminActive(this, true)
                Toast.makeText(this, "🔒 IronLock is now a Device Admin!", Toast.LENGTH_LONG).show()
            }
        }
    }
}
