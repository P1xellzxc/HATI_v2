# QUESTIONS_FOR_REVIEW.md

> These are decisions and open questions that require your input before proceeding.
> Created: April 9, 2026
> Updated: April 10, 2026

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

## 1. AGP 9.0 Migration (High Impact)

**Current:** AGP 8.7.2 + Gradle 8.10.2
**Latest stable:** AGP 9.0.1 + Gradle 9.1.0

AGP 9.0 is the latest stable Android Gradle Plugin, but migrating from 8.x to 9.x is a **major breaking change** that includes:
- **Built-in Kotlin support** — the `kotlin-android` plugin must be removed (AGP bundles Kotlin now)
- **Gradle 9.1.0 minimum** — requires upgrading the Gradle wrapper
- **Kotlin 2.3.x required** — to match the bundled Kotlin in AGP 9
- **KSP version must also update** to 2.3.6 to match Kotlin 2.3.20
- Several deprecated gradle.properties flags are removed

**Recommended next step:** Create a separate branch, follow the [official AGP 9 migration guide](https://developer.android.com/build/releases/agp-9-0-0-release-notes), and test thoroughly in Android Studio before merging.

---

## 2. Kotlin Version Upgrade Path

**Current:** Kotlin 2.0.21

The latest stable Kotlin is **2.3.20**. Kept at 2.0.21 because:
- Upgrading Kotlin requires matching KSP version (2.3.6)
- The Compose compiler plugin version is tied to Kotlin
- This is safest to do alongside the AGP 9 migration

**Question:** Would you like to upgrade Kotlin to 2.3.20 in a separate PR, independent of the AGP 9 migration?

---

## 3. Cloud Sync (Supabase) — Future Feature

The README mentions **opt-in Supabase synchronization** as a planned feature. If you proceed with this:
- The `network_security_config.xml` already enforces HTTPS-only
- You'll need to add proper API key management (NOT hardcoded — use `BuildConfig` or a secrets gradle plugin)
- Consider end-to-end encryption for synced financial data
- Implement proper auth (Supabase Auth with email/OAuth)

**Question:** Is cloud sync still on the roadmap? If so, what timeline?

---

## 4. Compose BOM 2026.03.01 Compatibility

The Compose BOM was upgraded to `2026.03.01`. While Compose UI libraries are generally binary-compatible across Kotlin versions, there's a small risk that the newer Compose runtime has minimum Kotlin requirements.

**Action needed:** After syncing this branch in Android Studio, run a clean build. If you see errors like `Incompatible Kotlin version`, you may need to either:
- Downgrade Compose BOM to `2025.05.01` (known compatible with Kotlin 2.0.x)
- Or upgrade Kotlin to 2.1.x+ (also requires KSP update)

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
| Room 2.6.1 → 2.8.4 | Bug fixes, performance improvements, stable |
| Hilt 2.52 → 2.59.2 | Bug fixes, AGP compatibility |
| Compose BOM → 2026.03.01 | Latest stable UI libraries |
| AndroidX libraries upgraded | Security patches, bug fixes |
| R8 enabled for release | Code obfuscation + APK shrinking |
| Backup rules hardened | Database excluded from cloud backup |
| Network security config | HTTPS-only enforced |
| Destructive migration scoped | Only v1→v2; future migrations must be explicit |
