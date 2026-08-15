# Health Monitor

A privacy-friendly Android step tracker and on-demand heart rate monitor that runs entirely on the phone — no watch, no account, no cloud.

- **Steps** are counted from the phone's own hardware: the built-in `TYPE_STEP_COUNTER`, with an accelerometer peak-detector fallback on devices that lack one. It never reads steps from a watch.
- **Heart rate** is measured *on request* by covering the back camera with a fingertip. The flashlight (torch) lights the fingertip and the app analyses the red channel of the camera frames (photoplethysmography). This is a wellness estimate only — not a medical device.
- **Targets & achievements** are computed from age, sex, height, weight and activity level: daily step goal, stride length, distance, calories, BMI, max heart rate (208 − 0.7 × age) and heart rate zones. 12 badges are awarded automatically.

## Features

| Area | Details |
| --- | --- |
| Dashboard | Today's steps, goal progress, distance, calories, active minutes, last heart rate, BMI, HR zones, current streak |
| Heart rate | Camera + torch PPG measurement with live waveform, session average and confidence; wellness-only disclaimer |
| Achievements | 12 badges: first steps, 1k / 5k / 10k / 20k steps, 5 km / 10 km, 500 kcal, healthy BMI, first pulse, 3-day and 7-day streaks |
| Profile | Age, sex, height, weight, activity level, optional custom step goal; live targets preview |
| History | Last 60 days in a scrollable list plus a 14-day bar chart, with per-day steps, distance, calories and average heart rate |
| Backup | Export the whole SQLite database to a file (SAF) and restore it later or on another device |
| Background | Foreground service keeps the step counter alive in the background and flushes data to SQLite every 30 s (battery-friendly) |

## Data & privacy

- All data stays on-device in a local SQLite database (`health_monitor.db`).
- No network calls, no analytics, no ads, no account.
- You can export/restore the database yourself via the Profile tab.

## Permissions

- **Activity recognition** — required on Android 10+ to read the built-in step counter.
- **Camera** — required for the heart rate measurement (also used for the torch).
- **Notifications** — Android 13+ notification permission for the background tracking service.

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
├── camera/
│   └── HeartRateAnalyzer.kt # PPG frame analysis
└── ui/
    ├── DashboardFragment.kt
    ├── HeartRateFragment.kt
    ├── HistoryFragment.kt
    ├── AchievementsFragment.kt
    ├── ProfileFragment.kt
    └── view/                # StepsBarChartView, WaveformView
```

## Disclaimer

Heart rate values are estimates for wellness tracking only. This app is not a medical device and must not be used to diagnose, treat or monitor medical conditions.
