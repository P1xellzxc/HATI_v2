---
title: "Android build"
tags: ["hati/tech"]
---

### Android app

Requirements: JDK 17 and Android Studio (or the Android SDK command-line tools).

```bash
./gradlew assembleDebug        # APK in app/build/outputs/apk/debug/
./gradlew lint test            # same checks as CI
```

Working toolchain (CI green since Nov 2026):

| AGP | Gradle | Kotlin | KSP | JDK | compileSdk / minSdk |
|---|---|---|---|---|---|
| 8.7.2 | 8.10.2 | 2.0.21 | 2.0.21-1.0.28 | 17 | 35 / 26 |

Libraries: Hilt 2.52, Room 2.6.1, Compose BOM 2025.01.01, core-ktx 1.15.0, lifecycle 2.8.7, activity-compose 1.10.0, navigation-compose 2.8.5, datastore 1.1.1, SQLCipher 4.14.1, biometric 1.1.0. These are the last versions known to build. Upgrades go one group at a time, each confirmed by CI.


Owner: [[hati-android-build]] (restore known-good, upgrade one group at a time, CI as the compiler). Back to [[HATI Brain]]
