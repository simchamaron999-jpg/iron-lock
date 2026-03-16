# IronLock — Android Device Admin Alarm App

> **No escape. No excuses.**

An Android alarm app powered by Device Admin APIs that makes alarms
truly unstoppable for a configurable duration.

---

## Features

| Feature | Details |
|---|---|
| 🔒 Device Admin | Registered via `DevicePolicyManager` |
| ⏰ Brutal Alarm | Rings for 1–10 minutes, cannot be dismissed |
| 🚫 Admin removal blocked | `onDisableRequested` re-locks device if alarm firing |
| 💡 Screen stays on | `SCREEN_BRIGHT_WAKE_LOCK` + `FLAG_KEEP_SCREEN_ON` |
| 🔁 Reboot safe | `BootReceiver` reschedules alarms after reboot |
| 🔐 Auto re-lock | `lockNow()` called after alarm duration ends |

---

## Project Structure

```
app/src/main/
├── AndroidManifest.xml
├── java/com/ironlock/
│   ├── admin/
│   │   └── IronLockAdminReceiver.kt   ← Device Admin lifecycle
│   ├── alarm/
│   │   ├── AlarmScheduler.kt          ← AlarmManager scheduling
│   │   ├── AlarmReceiver.kt           ← BroadcastReceiver on trigger
│   │   └── BootReceiver.kt            ← Reschedule on reboot
│   ├── service/
│   │   └── AlarmService.kt            ← Core foreground service
│   ├── ui/
│   │   ├── MainActivity.kt            ← Dashboard + alarm setup
│   │   └── AlarmFiringActivity.kt     ← Fullscreen alarm lockdown UI
│   └── utils/
│       └── AlarmPrefs.kt              ← SharedPreferences helper
└── res/
    ├── layout/
    │   ├── activity_main.xml
    │   └── activity_alarm_firing.xml
    ├── xml/
    │   └── device_admin_policies.xml  ← Declares admin capabilities
    └── values/
        ├── themes.xml
        └── strings.xml
```

---

## Setup in Android Studio

1. **Open** → `File > Open` → select the `IronLock` folder
2. Let Gradle sync
3. **Run** on a real device (Device Admin does NOT work on emulators reliably)
4. Tap **"Activate Device Admin"** — Android will show a system prompt
5. Grant admin, set your alarm time, tap **ARM ALARM**

---

## How the "Unstoppable" mechanism works

```
AlarmManager fires at set time
        ↓
AlarmReceiver.onReceive()
  → AlarmPrefs.setAlarmFiring(true)   ← THIS IS THE GUARD
  → startForegroundService(AlarmService)
        ↓
AlarmService
  → acquires SCREEN_BRIGHT_WAKE_LOCK
  → starts ringtone on loop (AudioAttributes.USAGE_ALARM)
  → launches AlarmFiringActivity over lock screen
  → schedules auto-stop after [duration]
        ↓
If user tries to remove Device Admin:
  IronLockAdminReceiver.onDisableRequested()
    → checks AlarmPrefs.isAlarmCurrentlyFiring()
    → calls dpm.lockNow()             ← device re-locks immediately
    → returns warning message
        ↓
After [duration]:
  AlarmService.stopAlarm()
    → stops ringtone + releases WakeLock
    → AlarmPrefs.setAlarmFiring(false)
    → dpm.lockNow()                   ← screen locks on wake
    → stopSelf()
```

---

## Important Notes

- **Real device required** — Device Admin features are unreliable on emulators
- **Android 9+ (API 28)** minimum target
- The `SCHEDULE_EXACT_ALARM` permission may require user approval on API 31+
  (Settings → Apps → IronLock → Alarms & Reminders)
- `lockNow()` blocks power-off indirectly by locking the screen before
  the power menu can be acted upon — true power-off prevention requires
  root or system-level access beyond standard Device Admin

---

## Next Steps / Extensions

- [ ] Add math puzzle / challenge to unlock early
- [ ] Remote lock/wipe via Firebase Cloud Messaging
- [ ] Multiple recurring alarms
- [ ] Snooze with penalty (adds more ring time)
- [ ] Admin panel screen in UI
