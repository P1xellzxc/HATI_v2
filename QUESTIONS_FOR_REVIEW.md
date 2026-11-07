# QUESTIONS_FOR_REVIEW.md

> These are decisions and open questions that require your input before proceeding.
> Created: April 9, 2026
> Updated: November 7, 2026

---

## ✅ RESOLVED: Database Encryption (Privacy)

**Decision:** SQLCipher integrated via `net.zetetic:sqlcipher-android:4.14.1`.

- The Room database is now encrypted at rest using AES-256.
- The encryption passphrase is generated randomly and stored encrypted in SharedPreferences using an Android Keystore-backed AES-GCM key.
- The Keystore key is hardware-backed (TEE/StrongBox) on most modern devices and is non-extractable.
- See `DatabaseKeyManager.kt` and `DatabaseModule.kt` for implementation.
- **Note:** Existing unencrypted databases will be replaced on first launch after this update (users on v1/v2 will lose data). A migration tool could be built if needed.

---

## ✅ RESOLVED: User Authentication / Identity

**Decision:** Biometric authentication with device credential (PIN/pattern/password) fallback.

- Uses `androidx.biometric:biometric:1.1.0` (stable).
- On app launch, `BiometricAuthGate` prompts for fingerprint/face unlock or device PIN.
- If the device has no biometric hardware or no enrolled credentials, the gate is skipped (the device lock screen serves as primary defense).
- See `BiometricAuthGate.kt` for implementation.

---

## ✅ RESOLVED: Room Schema Export

**Decision:** Enabled with `exportSchema = true` and `room.schemaLocation` KSP argument.

- Schema JSON files are exported to `app/schemas/` and should be committed to source control.
- Enables `@AutoMigration` for future schema changes and build-time migration validation.

---

## ✅ RESOLVED: CSV Export Security

**Decision:** Warning dialog before export.

- `ExportWarningDialog` informs the user that the CSV contains financial data and is not encrypted.
- Integrated in both `DashboardDetailScreen` and `HistoryScreen`.
- Error messages no longer leak internal details (fixed `e.printStackTrace()` and `e.message` exposure in HistoryScreen).

---

## ✅ RESOLVED: Android build was never green (AGP / Hilt / bulk upgrade)

**Decision (Nov 2026, hati-android-build skill):** restore the last dependency set that provably built, then upgrade one group at a time with CI as the check.

- Commit `4000388` upgraded 13 libraries at once without building. Hilt 2.59.2 alone requires AGP 9 while the project is on AGP 8.7.2, so CI had failed since it was added.
- Restored the pre-upgrade versions (the owner's `build_result.txt` showed BUILD SUCCESSFUL): Hilt 2.52, Room 2.6.1, Compose BOM 2025.01.01, core-ktx 1.15.0, lifecycle 2.8.7, activity-compose 1.10.0, navigation-compose 2.8.5, datastore 1.1.1, and the previous test libraries.
- Then fixed real code issues CI surfaced: SQLCipher API (`SupportOpenHelperFactory` for `net.zetetic:sqlcipher-android`) plus R8 keep rules, an API-27 theme attribute, lint-rejected backup excludes, mockable `MainViewModel` members, test classpath and matchers, `updateDashboard` silently dropping edits, and debt balances that now always sum to zero.
- Result: Lint + Unit Tests, Instrumented Tests (API 34) and the APK build are all green (PR #22).

Working toolchain: AGP 8.7.2, Gradle 8.10.2, Kotlin 2.0.21, KSP 2.0.21-1.0.28, JDK 17, compileSdk 35 / minSdk 26.

## Open: AGP 9 and Kotlin 2.3 (planned, not urgent)

AGP 9 needs Gradle 9.1, Kotlin 2.3.x and KSP 2.3.x together, and removes the `kotlin-android` plugin. It will be done as one coupled step, on its own branch, verified by CI. The `hati-android-build` skill owns this decision.

---

## ✅ RESOLVED: Cloud Sync — Removed

**Decision:** Cloud sync has been permanently removed. HATI² is a local-only app. The network security config has been removed as the app requires no internet access.

---

## ✅ RESOLVED: iOS support

**Decision:** No native iOS build. The installable web app (`web/` → `docs/app/`) serves iPhone users via Safari's Add to Home Screen, and Android users via Chrome's Install app. Share codes and backup files move a volume between phones; importing merges changes.

---

## Summary of What Was Done

| Change | Why |
|---|---|
| **SQLCipher encryption** | Encrypts Room database at rest (AES-256) |
| **Biometric auth gate** | Fingerprint/face unlock on app launch |
| **Input validation** | Length limits, sanitization, amount bounds |
| **Room schema export** | Migration safety + build-time validation |
| **CSV export warning** | User confirmation before exporting sensitive data |
| **Error message hardening** | Removed e.printStackTrace() and e.message leakage |
| **ProGuard rules** | Added rules for SQLCipher and Biometric |
| **Backup rules hardened** | Excluded encrypted DB passphrase from all backup |
| Dependency versions restored to last known-good set | The April bulk upgrade never built; see "Android build" above |
| Wada color themes | 13 palettes from Sanzo Wada (1918), contrast-tested |
| Installable web app | Android + iOS via home screen, offline, share codes with merge |
| R8 enabled for release | Code obfuscation + APK shrinking |
| Backup rules hardened | Database excluded from cloud backup |
| Network security config | Removed — app is local-only, no internet needed |
| Destructive migration scoped | Only v1→v2; future migrations must be explicit |
