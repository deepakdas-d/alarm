# 🕐 Neon Alarm — Project Overview

> **Package:** `com.deepak.alarm`  
> **Framework:** Flutter (Dart) + Kotlin (Android native)  
> **State Management:** Provider  
> **Min SDK:** Dart ^3.12.1  
> **Version:** 1.0.0+1

---

## What Is This App?

**Neon Alarm** is a full-featured Android alarm clock app built with Flutter for the UI and **native Kotlin** for the actual alarm scheduling engine. It uses Android's `AlarmManager` to schedule exact alarms that survive app closure, device reboot, timezone changes, and battery optimization (Doze mode).

The app has a dark-themed "neon" design aesthetic with a deep slate background (`#0F172A`) and blue accent tones.

---

## Architecture at a Glance

```
┌─────────────────────────────────────────────────┐
│                  Flutter (Dart)                  │
│                                                  │
│  ┌──────────┐  ┌──────────┐  ┌───────────────┐  │
│  │  Screens  │  │ Providers│  │    Models     │  │
│  │ (4 pages) │──│ (4 files)│──│ (Alarm data)  │  │
│  └──────────┘  └──────────┘  └───────────────┘  │
│         │                                        │
│         ▼                                        │
│  ┌─────────────────────────────────────────────┐ │
│  │  AlarmService (MethodChannel bridge)        │ │
│  │  Channel: "com.deepak.alarm/alarms"         │ │
│  └──────────────────┬──────────────────────────┘ │
├─────────────────────┼───────────────────────────-┤
│                     ▼    Kotlin (Android Native)  │
│  ┌──────────────────────────────────────────────┐│
│  │  AlarmMethodChannel (handles Flutter calls)  ││
│  └──────────────┬───────────────────────────────┘│
│     ┌───────────┼────────────────┐               │
│     ▼           ▼                ▼               │
│  ┌──────┐  ┌──────────┐  ┌────────────────┐     │
│  │ Data │  │  Engine   │  │   Receivers    │     │
│  │ Layer│  │(Scheduler │  │(Boot, Time,    │     │
│  │(SQLite│  │ WakeLock  │  │ Timezone,      │     │
│  │ DB)  │  │ Battery)  │  │ PackageUpdate) │     │
│  └──────┘  └──────────┘  └────────────────┘     │
│                 │                                 │
│                 ▼                                 │
│  ┌──────────────────────────────────────────────┐│
│  │  Service Layer                               ││
│  │  (ForegroundService, MediaPlayer,            ││
│  │   Notifications, AlarmActivity UI)           ││
│  └──────────────────────────────────────────────┘│
└──────────────────────────────────────────────────┘
```

---

## Folder Structure

```
alarm/
├── lib/                          # Flutter (Dart) source code
│   ├── main.dart                 # App entry point, sets up Provider & theme
│   ├── models/
│   │   └── alarm_model.dart      # Alarm & AlarmInstance data classes
│   ├── providers/
│   │   ├── alarm_provider.dart          # CRUD operations + state for alarm list
│   │   ├── alarm_editor_provider.dart   # Editing state (time, days, snooze, etc.)
│   │   ├── settings_provider.dart       # Permission status tracking
│   │   └── statistics_provider.dart     # Alarm history stats
│   ├── screens/
│   │   ├── alarm_list_screen.dart       # Home screen — lists all alarms
│   │   ├── alarm_editor_screen.dart     # Create/edit alarm form
│   │   ├── settings_screen.dart         # Permission management dashboard
│   │   └── statistics_screen.dart       # Alarm event statistics
│   ├── services/
│   │   └── alarm_service.dart    # MethodChannel bridge to native Kotlin
│   └── theme/
│       └── app_theme.dart        # Dark theme colors, typography, widget styles
│
├── android/app/src/main/
│   ├── AndroidManifest.xml       # Permissions, receivers, services, activities
│   └── kotlin/com/deepak/alarm/
│       ├── MainActivity.kt                  # Flutter activity host
│       ├── bridge/
│       │   └── AlarmMethodChannel.kt        # Handles all MethodChannel calls
│       ├── data/
│       │   ├── AlarmDbHelper.kt             # SQLite database creation & migration
│       │   ├── AlarmEntity.kt               # Alarm table schema
│       │   ├── AlarmInstanceEntity.kt       # Alarm instance table schema
│       │   └── AlarmRepository.kt           # Database CRUD operations
│       ├── engine/
│       │   ├── AlarmScheduler.kt            # AlarmManager exact alarm scheduling
│       │   ├── AutoStartHelper.kt           # OEM auto-start settings launcher
│       │   ├── BatteryOptimizationHelper.kt # Doze mode bypass
│       │   ├── ExactAlarmPermissionManager.kt # Android 12+ permission handling
│       │   └── WakeLockManager.kt           # CPU wakelock for alarm firing
│       ├── receivers/
│       │   ├── AlarmReceiver.kt             # Fires when AlarmManager triggers
│       │   ├── BootReceiver.kt              # Re-schedules alarms after reboot
│       │   ├── PackageUpdateReceiver.kt     # Re-schedules after app update
│       │   ├── TimeChangeReceiver.kt        # Re-schedules on manual time change
│       │   └── TimeZoneReceiver.kt          # Re-schedules on timezone change
│       ├── service/
│       │   ├── AlarmForegroundService.kt    # Foreground service for alarm sound
│       │   ├── MediaPlayerManager.kt        # Plays ringtone audio
│       │   └── NotificationHelper.kt        # Creates alarm notifications
│       └── ui/
│           └── AlarmActivity.kt             # Full-screen alarm ringing UI
│
├── assets/image/
│   └── logo.png                  # App icon (1.2 MB)
├── pubspec.yaml                  # Dependencies & asset config
└── analysis_options.yaml         # Lint rules
```

---

## Key Concepts

### 1. Data Model (`Alarm`)

| Field             | Type   | Description                                                |
|-------------------|--------|------------------------------------------------------------|
| `id`              | int    | Auto-generated primary key                                 |
| `label`           | String | User-friendly alarm name (optional)                        |
| `hour`            | int    | Hour (0-23, 24h format internally)                         |
| `minute`          | int    | Minute (0-59)                                              |
| `enabled`         | bool   | Whether the alarm is active                                |
| `repeatDays`      | int    | **Bitmask**: 1=Mon, 2=Tue, 4=Wed, 8=Thu, 16=Fri, 32=Sat, 64=Sun |
| `soundUri`        | String | URI of selected ringtone (empty = default)                 |
| `vibrationEnabled`| bool   | Whether to vibrate on trigger                              |
| `snoozeMinutes`   | int    | Snooze duration (5, 10, 15, 20, or 30 min)                 |
| `createdAt`       | int    | Creation timestamp                                         |
| `updatedAt`       | int    | Last update timestamp                                      |

**Repeat Days Bitmask Examples:**
- `0` = One-time alarm (fires once)
- `31` (1+2+4+8+16) = Weekdays (Mon–Fri)
- `96` (32+64) = Weekends (Sat–Sun)
- `127` = Every day

### 2. `AlarmInstance`

Tracks individual trigger events of an alarm with statuses: `scheduled`, `triggered`, `snoozed`, `dismissed`, `missed`.

---

## Data Flow

### Creating an Alarm:
```
User taps "+" → AlarmEditorScreen → fills time/label/days/sound/etc.
    → taps ✓ (save)
    → AlarmEditorProvider builds Alarm object
    → AlarmProvider.addAlarm() called
    → AlarmService.createAlarm() → MethodChannel → Kotlin
    → AlarmMethodChannel → AlarmRepository (SQLite INSERT)
    → AlarmScheduler schedules via AlarmManager
    → Returns alarm ID back to Flutter
    → Local state updated → UI refreshes
```

### Alarm Fires:
```
AlarmManager triggers → AlarmReceiver (BroadcastReceiver)
    → Acquires WakeLock
    → Starts AlarmForegroundService
        → Plays sound via MediaPlayerManager
        → Shows notification via NotificationHelper
    → Launches AlarmActivity (full-screen over lock screen)
    → User taps Dismiss or Snooze
        → Dismiss: stops service, cancels notification
        → Snooze: reschedules alarm for +N minutes
```

---

## Flutter ↔ Native Bridge

The entire native communication goes through a **single MethodChannel**: `com.deepak.alarm/alarms`

### Available Methods:

| Method                       | Direction       | Description                                      |
|------------------------------|-----------------|--------------------------------------------------|
| `getAllAlarms`                | Flutter → Kotlin | Fetch all alarms from SQLite                    |
| `getInstances`               | Flutter → Kotlin | Get trigger instances for a specific alarm       |
| `createAlarm`                | Flutter → Kotlin | Insert alarm + schedule with AlarmManager        |
| `updateAlarm`                | Flutter → Kotlin | Update alarm + reschedule                        |
| `deleteAlarm`                | Flutter → Kotlin | Delete alarm + cancel scheduled triggers         |
| `toggleAlarm`                | Flutter → Kotlin | Enable/disable alarm                             |
| `checkExactAlarmPermission`  | Flutter → Kotlin | Check Android 12+ exact alarm permission         |
| `requestExactAlarmPermission`| Flutter → Kotlin | Open system settings for exact alarm             |
| `checkBatteryOptimization`   | Flutter → Kotlin | Check if battery optimizations are ignored       |
| `requestBatteryOptimization` | Flutter → Kotlin | Request Doze mode bypass                         |
| `isAutoStartAvailable`       | Flutter → Kotlin | Check if OEM has auto-start (Xiaomi, Oppo, etc.) |
| `getAutoStartInfo`           | Flutter → Kotlin | Get manufacturer-specific instructions           |
| `requestAutoStart`           | Flutter → Kotlin | Open OEM auto-start settings                     |
| `pickAlarmSound`             | Flutter → Kotlin | Open Android ringtone picker                     |
| `getStatistics`              | Flutter → Kotlin | Get alarm event counts by status                 |

---

## State Management (Provider)

| Provider               | Scope    | Purpose                                           |
|------------------------|----------|---------------------------------------------------|
| `AlarmProvider`        | Global   | Manages list of alarms, CRUD with optimistic updates |
| `AlarmEditorProvider`  | Per-screen | Holds editing state for alarm create/edit form   |
| `SettingsProvider`     | Per-screen | Loads and manages permission grant statuses      |
| `StatisticsProvider`   | Per-screen | Loads alarm event statistics from native         |

**Note:** `AlarmProvider` uses **optimistic updates** — the UI updates immediately, and if the native call fails, it reverts by reloading all alarms.

---

## Screens

### 1. Alarm List (`alarm_list_screen.dart`) — Home
- Lists all alarms in scrollable cards
- Each card shows: time (12h), label, repeat pattern, enable/disable toggle
- **Swipe left to delete** (Dismissible)
- **Tap to edit** → navigates to editor
- **FAB (+)** → create new alarm
- App bar has **Statistics** (📊) and **Settings** (⚙️) buttons
- On first launch, sequentially requests: Notification → System Alert Window → Exact Alarm → Battery Optimization → Auto-start

### 2. Alarm Editor (`alarm_editor_screen.dart`) — Create/Edit
- Large tappable time display (opens Material TimePicker)
- Label text field
- Repeat day selector (circular toggles: M T W T F S S)
- Sound picker (opens native Android RingtonePicker)
- Vibration toggle switch
- Snooze duration dropdown (5/10/15/20/30 min)
- Delete button (only when editing existing alarm)

### 3. Settings (`settings_screen.dart`) — Permissions
- Shows permission status cards with Grant/Fix buttons:
  - Exact Alarms, Notifications, Display Over Other Apps
  - Battery Optimization, Auto-Start (OEM-specific)
- Each card has colored icon indicators (green = granted, red = not granted)

### 4. Statistics (`statistics_screen.dart`) — Analytics
- Shows alarm event counts: Total, Dismissed, Snoozed, Missed, Scheduled, Triggered

---

## Android Native Components

### Broadcast Receivers (re-schedule alarms on system events):
| Receiver               | Trigger                                | Action                          |
|------------------------|----------------------------------------|---------------------------------|
| `AlarmReceiver`        | AlarmManager fires at scheduled time   | Wakes device, starts service    |
| `BootReceiver`         | Device boot / locked boot              | Re-schedules all enabled alarms |
| `PackageUpdateReceiver`| App gets updated (MY_PACKAGE_REPLACED) | Re-schedules all enabled alarms |
| `TimeChangeReceiver`   | User manually changes system time      | Re-schedules all enabled alarms |
| `TimeZoneReceiver`     | Timezone changes                       | Re-schedules all enabled alarms |

### Services:
| Component                | Purpose                                                  |
|--------------------------|----------------------------------------------------------|
| `AlarmForegroundService` | Foreground service (mediaPlayback type) plays alarm sound |
| `MediaPlayerManager`     | Manages ringtone audio playback                          |
| `NotificationHelper`     | Creates/manages alarm notifications with actions         |

### Engine Helpers:
| Component                      | Purpose                                          |
|--------------------------------|--------------------------------------------------|
| `AlarmScheduler`               | Schedules exact alarms via AlarmManager           |
| `WakeLockManager`              | Acquires/releases CPU wakelock during alarm fire  |
| `ExactAlarmPermissionManager`  | Handles Android 12+ SCHEDULE_EXACT_ALARM          |
| `BatteryOptimizationHelper`    | Requests ignore battery optimization (Doze bypass)|
| `AutoStartHelper`              | Handles OEM-specific auto-start (Xiaomi, Oppo, etc.)|

### Data Layer:
| Component           | Purpose                                       |
|---------------------|-----------------------------------------------|
| `AlarmDbHelper`     | SQLite database creation and schema migrations |
| `AlarmEntity`       | Alarm table definition                         |
| `AlarmInstanceEntity`| Alarm trigger instance table definition       |
| `AlarmRepository`   | All database CRUD operations                   |

### UI:
| Component       | Purpose                                                      |
|-----------------|--------------------------------------------------------------|
| `AlarmActivity` | Full-screen native activity shown over lock screen when alarm fires |

---

## Permissions Required

| Permission                              | Why                                                    |
|-----------------------------------------|--------------------------------------------------------|
| `RECEIVE_BOOT_COMPLETED`               | Re-schedule alarms after device reboot                  |
| `WAKE_LOCK`                            | Keep CPU alive while alarm fires                        |
| `SCHEDULE_EXACT_ALARM`                 | Schedule alarms at exact times (Android 12+)            |
| `USE_EXACT_ALARM`                      | Alternative exact alarm permission                      |
| `USE_FULL_SCREEN_INTENT`              | Show alarm screen over lock screen                      |
| `POST_NOTIFICATIONS`                   | Show alarm notifications (Android 13+)                  |
| `FOREGROUND_SERVICE`                   | Run foreground service for audio playback               |
| `FOREGROUND_SERVICE_MEDIA_PLAYBACK`    | Specific foreground service type                        |
| `VIBRATE`                              | Vibrate the device                                      |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Request Doze mode bypass                                |
| `SYSTEM_ALERT_WINDOW`                  | Display alarm screen over other apps / wake screen      |
| Custom `ALARM_TRIGGER` (signature)     | Protects AlarmReceiver from external apps               |

---

## Dependencies

| Package              | Version     | Purpose                           |
|----------------------|-------------|-----------------------------------|
| `provider`           | ^6.1.5+1    | State management                  |
| `permission_handler` | ^12.0.3     | Runtime permission requests       |
| `cupertino_icons`    | ^1.0.8      | iOS-style icons                   |

### Dev Dependencies:
| Package                  | Version  | Purpose                    |
|--------------------------|----------|----------------------------|
| `flutter_test`           | SDK      | Unit testing               |
| `flutter_lints`          | ^6.0.0   | Lint rules                 |
| `flutter_launcher_icons` | ^0.14.4  | Generate adaptive app icons |

---

## Theme & Design

- **Background:** `#0F172A` (very dark slate)
- **Surface:** `#1E293B` (slightly lighter slate)
- **Accent:** `#3B82F6` (deep blue) with gradient to `#60A5FA` (light blue)
- **Text:** `#F8FAFC` (primary) / `#94A3B8` (secondary)
- **Error:** `#EF4444` (red) | **Success:** `#10B981` (green) | **Warning:** `#F59E0B` (amber)
- **Font:** Roboto
- **Corner radius:** 16px–24px for cards, circular for day toggles
- **App Icon:** Custom `logo.png` with adaptive icon (monochrome + `#0A1028` background)

---

## How to Run

```bash
# Get dependencies
flutter pub get

# Run on connected device/emulator
flutter run

# Generate app icons
flutter pub run flutter_launcher_icons
```

---

## Key Design Decisions

1. **Native Kotlin for scheduling** — Flutter plugins can't reliably handle exact alarms, wakelocks, and boot receivers. The entire alarm engine is native.
2. **Single MethodChannel** — All Flutter↔Native communication goes through one channel (`com.deepak.alarm/alarms`) for simplicity.
3. **Optimistic UI updates** — The alarm list updates instantly on user actions, reverting via `loadAlarms()` if the native call fails.
4. **Bitmask for repeat days** — Efficient integer storage for weekday selection (7 bits = 7 days).
5. **Signature-level permission** — `AlarmReceiver` is protected by a custom signature permission so only this app can trigger alarms.
6. **Full resilience** — Alarms survive: app close, reboot, timezone change, time change, app update — each has a dedicated BroadcastReceiver.
