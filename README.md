# Call Scheduler (Android)

Schedule calls to fire automatically at a specific date/time, with notes that are
displayed on top of the dialer the moment the call is placed.

## What it does

- Save a contact name, phone number, date/time, and free-form notes ("Ask about Q3 budget").
- At the scheduled moment, the app:
  1. Wakes the device with an exact alarm (`AlarmManager.setExactAndAllowWhileIdle`).
  2. Places the call automatically via `Intent.ACTION_CALL`.
  3. Floats your notes on top of the dialer using a `SYSTEM_ALERT_WINDOW` overlay, so
     you can see exactly why you're calling that person.
- Survives reboots: a `BootReceiver` re-registers all upcoming alarms.

## Build & run

Open the project in Android Studio (Hedgehog or newer), let Gradle sync, and
run on a device or emulator with API 26+.

From CLI (after generating the wrapper once with `gradle wrapper --gradle-version 8.9`):

```bash
./gradlew assembleDebug
./gradlew installDebug
```

## Permissions you'll need to grant

The first time you save a schedule, Android will ask for:

- **Phone (CALL_PHONE)** — to place the call. If denied, the dialer opens
  pre-filled with the number instead.
- **Notifications (POST_NOTIFICATIONS, Android 13+)** — for the foreground
  service that hosts the notes overlay.
- **Schedule exact alarms (Android 12+)** — needed for time-precise firing;
  the app routes you to system settings if not granted.
- **Display over other apps (SYSTEM_ALERT_WINDOW)** — needed to overlay the
  notes on the dialer.

## How a scheduled call flows

1. `EditScheduleScreen` saves a `ScheduledCall` row to Room.
2. `CallsViewModel` calls `CallScheduler.schedule()`, which registers an exact
   `AlarmManager` PendingIntent for the chosen time.
3. At fire time, `CallAlarmReceiver.onReceive`:
   - Marks the row as `triggered`.
   - Starts `NotesOverlayService` (foreground + overlay window) showing the notes.
   - Fires `Intent.ACTION_CALL` to dial the number.
4. The notes float on top of the dialer; tap **Dismiss** when the call ends.

## Project layout

```
app/src/main/java/com/scheduler/calls/
├── SchedulerApp.kt              # Application + repository wiring
├── data/                        # Room: entity, DAO, database, repository
├── alarm/                       # AlarmManager scheduling + BroadcastReceivers
├── overlay/NotesOverlayService  # Foreground service rendering the overlay
└── ui/                          # Compose screens + ViewModel + MainActivity
```

## Notes on reliability

- Some OEMs (Xiaomi, Huawei, Samsung) aggressively kill background apps. To make
  scheduled calls reliable, exempt the app from battery optimizations in system
  settings.
- Auto-placing a call requires `CALL_PHONE`. If that's denied, the app falls
  back to opening the dialer pre-filled — you'll just need to tap call.
