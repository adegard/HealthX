# Health Monitor

A privacy-friendly Android step tracker that runs entirely on the phone — no watch, no account, no cloud (optional self-hosted Home Assistant sync).

- **Steps** are counted from the phone's own hardware: the built-in `TYPE_STEP_COUNTER`, with an accelerometer peak-detector fallback on devices that lack one. It never reads steps from a watch.
- **Targets & achievements** are computed from age, sex, height, weight and activity level: daily step goal, stride length, distance, calories, BMI, max heart rate (208 − 0.7 × age) and heart rate zones.
- **Home Assistant sync** (optional) pushes each day's progress to your own Home Assistant instance once per day over your home Wi-Fi.

## Features

| Area | Details |
| --- | --- |
| Dashboard | Today's steps, goal progress, distance, calories, active minutes, BMI, HR zones, current streak |
| Achievements | 11 badges: first steps, 1k / 5k / 10k / 20k steps, 5 km / 10 km, 500 kcal, healthy BMI, 3-day and 7-day streaks |
| Profile | Age, sex, height, weight, activity level, optional custom step goal; live targets preview |
| History | Last 60 days in a scrollable list plus a 14-day bar chart, with per-day steps, distance and calories |
| Wellness | Sedentary reminders (optional notifications to stand up and move), walking advice, a 5-minute morning routine and gentle no-equipment exercises for skeleton and muscle |
| Backup | Export the whole SQLite database to a file (SAF) and restore it later or on another device |
| Background | Foreground service keeps the step counter alive in the background and flushes data to SQLite every 30 s (battery-friendly) |
| Home Assistant | Single sensor `sensor.healthx_steps` (state = latest day, `history` attribute = every day); retries while away and catches up missed days |

## Data & privacy

- All data stays on-device in a local SQLite database (`health_monitor.db`).
- No network calls, no analytics, no ads, no account.
- You can export/restore the database yourself via the Profile tab.

## Home Assistant sync

1. In Home Assistant, click your username → **Security** → **Long-lived access tokens** → **Create token** and copy it.
2. In the app: **Profile → Home Assistant sync**, enter your server URL (e.g. `http://192.168.8.17:8123`) and the token, then enable the switch.
3. The app maintains ONE entity: `sensor.healthx_steps`. Its state is the latest day's step count; attributes include `date`, `distance_km`, `calories` and a `history` map (`{"2026-08-24": 7796, ...}`) with the last 30 days. Opening the app also refreshes the sensor.

**How it works:** shortly after midnight a daily alarm fires. If the phone is on Wi-Fi, the sensor is updated with all data that has not been sent yet (older versions created one entity per day; those are deleted automatically). If Wi-Fi is unavailable — for example you are away — the app retries every 30 minutes and catches up later, so skipped days are not lost: when you are back home each missing day appears in the history attribute under its own date.

**Past days / statistics:** besides the state update, every sync also imports all days into Home Assistant's long-term statistics via `recorder.import_statistics` (`statistic_id: sensor.healthx_steps`, source `healthx`). That means past days show up immediately: check **Developer tools → Statistics**, add a **Statistics Graph** card (period: day), or open the entity's more-info dialog.

## Permissions

- **Activity recognition** — required on Android 10+ to read the built-in step counter.
- **Notifications** — Android 13+ notification permission for the background tracking service and sedentary reminders.
- **Internet / network state** — only used for the optional Home Assistant sync on your local network.

## Target / build

- Kotlin, Android Gradle Plugin 8.2.2, Gradle 8.2
- `minSdk 26` (Android 8.0), `targetSdk 34` (Android 14)
- ViewBinding, CameraX, Material 3

Build a debug APK:

```bash
./gradlew assembleDebug
# output: app/build/outputs/apk/debug/app-debug.apk
```

This repository also contains a GitHub Actions workflow (`.github/workflows/build-debug.yml`) that builds the debug APK on every push and uploads it as the `health-monitor-debug-apk` artifact.

## Project layout

```
app/src/main/java/com/example/healthmonitor/
├── MainActivity.kt          # Entry point; wires stores/tracker, starts the service
├── HealthApp.kt             # Application; DB-backed singletons + legacy prefs migration
├── data/                    # SQLite layer, stores, calculations, achievements
│   ├── HealthDatabase.kt    # SQLiteOpenHelper: daily_stats, achievements, profile
│   ├── StatsStore.kt        # Steps / HR / history / backup
│   ├── ProfileStore.kt      # User profile
│   ├── TargetsCalculator.kt # Goals, stride, distance, calories, BMI, HR zones
│   ├── Achievements.kt      # Badge definitions + check/earn logic
│   └── DailyStat.kt, UserProfile.kt
├── sensor/
│   ├── StepTracker.kt       # Step counter + accelerometer fallback
│   └── StepService.kt       # Foreground service, 30 s flush tick
├── advice/
│   ├── SedentaryReminder.kt # Alarm scheduling for sit-less reminders
│   └── SedentaryReminderReceiver.kt # Moves-you reminder notification
├── sync/
│   ├── SyncManager.kt       # Catch-up logic: sends every unsynced day over Wi-Fi
│   ├── SyncScheduler.kt     # Daily ~00:15 alarm + retry alarm
│   ├── DailySyncReceiver.kt # Fires the sync from alarms
│   ├── BootReceiver.kt      # Re-arms alarms after reboot
│   └── HomeAssistantClient.kt # REST POST /api/states/<entity>
└── ui/
    ├── DashboardFragment.kt
    ├── HistoryFragment.kt
    ├── AdviceFragment.kt
    ├── AchievementsFragment.kt
    ├── ProfileFragment.kt
    └── view/                # StepsBarChartView
```

## Disclaimer

Heart rate values are estimates for wellness tracking only. This app is not a medical device and must not be used to diagnose, treat or monitor medical conditions.
