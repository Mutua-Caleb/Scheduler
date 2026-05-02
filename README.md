# Call Scheduler

Schedule calls to fire automatically at a specific date/time, with notes
displayed on top of the dialer the moment the call is placed.

This repo contains two pieces:

- **`app/`** — the Android app (Kotlin / Jetpack Compose).
- **`server/`** — a small FastAPI desktop server with a web UI for fast
  keyboard-driven entry. The phone syncs with this server.

See [`server/README.md`](server/README.md) for desktop setup. Quick start:
`cd server && ./run.sh`, then open `http://localhost:8765/`. In the Android
app, tap the **Settings** icon and enter your laptop's LAN URL (e.g.
`http://192.168.1.10:8765`).

## What it does

- Save a contact (via system contact picker), date/time, recurrence, and
  free-form notes ("Ask about Q3 budget").
- At the scheduled moment a heads-up confirmation notification appears with
  three actions: **Call now**, **Snooze 10m**, **Cancel**. After 30 seconds
  with no response, the call is auto-placed.
- When the call is placed, the app:
  1. Floats your notes on top of the dialer using a `SYSTEM_ALERT_WINDOW`
     overlay (or a heads-up notification fallback if overlay perm is denied).
  2. Auto-dismisses the overlay when the call ends (`TelephonyCallback`
     `CALL_STATE_IDLE`).
- Recurring schedules: NONE / DAILY / WEEKDAYS / WEEKLY. After a recurring
  call fires, the next occurrence is automatically computed and re-armed.
- A history screen logs every fired call with its outcome
  (CALLED / AUTO_FIRED / SNOOZED / CANCELLED).
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

The first time you launch and save a schedule, Android will ask for:

- **Phone (CALL_PHONE)** — to place the call. If denied, the dialer opens
  pre-filled with the number instead.
- **Read phone state (READ_PHONE_STATE)** — so the overlay can auto-dismiss
  when the call ends.
- **Notifications (POST_NOTIFICATIONS, Android 13+)** — for the call
  confirmation and the foreground service that hosts the notes overlay.
- **Schedule exact alarms (Android 12+)** — needed for time-precise firing;
  the app routes you to system settings if not granted.
- **Display over other apps (SYSTEM_ALERT_WINDOW)** — to overlay the notes on
  the dialer. If denied, the notes appear as a heads-up notification.
- **Battery-optimization exemption** — a one-time dialog routes you to
  settings. Without this, OEMs (Xiaomi, Huawei, Samsung, OPPO) may delay or
  skip your scheduled calls.

## How a scheduled call flows

1. `EditScheduleScreen` saves a `ScheduledCall` row to Room.
2. `CallsViewModel` calls `CallScheduler.schedule()`, which registers an exact
   `AlarmManager` PendingIntent for the chosen time.
3. At fire time, `CallAlarmReceiver` (action `FIRE_CALL`) shows the
   confirmation notification and arms a second alarm 30s out.
4. The user taps **Call now**, **Snooze 10m**, or **Cancel** (handled by
   `CallActionReceiver`), or the 30s alarm fires and auto-places the call.
5. `CallExecutor` cancels the confirmation, places the call via
   `Intent.ACTION_CALL`, starts `NotesOverlayService`, logs a `CallEvent`,
   marks the row triggered, and advances any recurrence.
6. `NotesOverlayService` displays a draggable card with the notes and
   auto-stops when the call ends.

## Project layout

```
app/src/main/java/com/scheduler/calls/
├── SchedulerApp.kt                  # Application + repository wiring
├── data/                            # Room: entities, DAOs, repository, recurrence
├── alarm/
│   ├── CallScheduler                # AlarmManager wrappers (fire + auto-place)
│   ├── CallAlarmReceiver            # Handles fire & auto-place alarms
│   ├── CallActionReceiver           # Handles Call now / Snooze / Cancel
│   ├── CallNotifications            # Channels + confirmation notification
│   ├── CallExecutor                 # Places the call, logs event, advances recurrence
│   └── BootReceiver                 # Re-arms upcoming alarms after reboot
├── overlay/NotesOverlayService      # Foreground overlay + telephony auto-close
└── ui/                              # Compose screens, ViewModel, MainActivity
```

## Reliability notes

- Battery-optimization exemption is requested on first launch via a dialog +
  `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`. This is the most important step
  for reliable firing.
- Database is included in `data_extraction_rules.xml` and `backup_rules.xml`,
  so reinstall/transfer preserves your schedule.

## Timezone semantics

Calls are stored as `(LocalDateTime, ZoneId)` rather than as a raw UTC
instant. The `ZoneId` is captured from the device at save time. The
absolute fire time is derived as `localDateTime.atZone(zoneId).toInstant()`.

Practical effects:

- **DST transitions** are handled automatically — "8 AM daily" stays at
  8 AM wall-clock time across spring-forward/fall-back.
- **Travel** keeps calls anchored to the original zone (calendar-style):
  scheduling 9 AM Pacific while in LA means the call still fires at 9 AM
  Pacific even if you're in NYC at fire time.
- **Boot recovery** recomputes the instant from the stored zone in case
  DST changed while the device was off.

## Tests

JVM unit tests cover the recurrence math and timezone-aware millis
computation:

```bash
./gradlew :app:testDebugUnitTest
```

## Theming

`SchedulerTheme` uses Material3 dynamic color on Android 12+ (matches the
user's wallpaper) and falls back to a hand-tuned light/dark scheme.
The Activity theme is `Theme.Material3.DayNight.NoActionBar`, so the
status bar follows the system light/dark setting.
