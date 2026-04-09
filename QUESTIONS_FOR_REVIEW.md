# QUESTIONS_FOR_REVIEW.md

> These are decisions and open questions that require your input before proceeding.
> Created: April 9, 2026

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

**Why I didn't do it:** I cannot run the Android build to verify the migration works. This migration needs to be done in Android Studio where you can incrementally fix compile errors and verify the app runs.

**Recommended next step:** Create a separate branch, follow the [official AGP 9 migration guide](https://developer.android.com/build/releases/agp-9-0-0-release-notes), and test thoroughly before merging.

---

## 2. Database Encryption (Privacy)

**Current:** The Room database (`hati_database`) stores all financial data (expenses, debts, person names, settlements) in **plain-text SQLite**.

**Risk:** Any app with root access, or anyone with physical access to the device, can read all financial data.

**Option A: SQLCipher** — Encrypt the entire database using `net.zetetic:android-database-sqlcipher`. This is the most comprehensive solution but adds ~3MB to APK size.

**Option B: EncryptedSharedPreferences** — Only encrypt sensitive preferences (not the database). Less comprehensive but lighter.

**Question:** Do you want me to integrate SQLCipher for full database encryption? This would require:
- Adding the SQLCipher dependency
- Modifying `AppDatabase.kt` and `DatabaseModule.kt` to use `SupportFactory`
- A one-time migration to re-encrypt existing data (or clear and start fresh)

---

## 3. User Authentication / Identity

**Current:** The app uses a hardcoded `"user-current"` string as the user ID everywhere. There is no authentication, no PIN lock, and no biometric protection.

**Risk:** Anyone who picks up the unlocked phone can see all financial data.

**Options:**
- **PIN/Pattern lock** — Require a PIN before opening the app
- **Biometric auth** — Use `androidx.biometric` for fingerprint/face unlock
- **No auth** — Keep it simple (acceptable for a personal expense tracker)

**Question:** Which approach do you prefer? Biometric + PIN fallback is the most secure option for a finance app.

---

## 4. Room Schema Export

**Current:** `exportSchema = false` in the `@Database` annotation.

**Recommendation:** Set `exportSchema = true` and configure `room.schemaLocation` in the KSP arguments. This enables:
- Automated migration validation at build time
- Schema version history in source control
- `@AutoMigration` annotations for future schema changes

**Question:** Should I enable schema export? The schema JSON files would be committed to `app/schemas/` directory.

---

## 5. Kotlin Version Upgrade Path

**Current:** Kotlin 2.0.21

The latest stable Kotlin is **2.3.20**. I kept Kotlin at 2.0.21 because:
- Upgrading Kotlin requires matching KSP version (2.3.6)
- The Compose compiler plugin version is tied to Kotlin
- This is safest to do alongside the AGP 9 migration

**Question:** Would you like me to upgrade Kotlin to 2.3.20 in a separate PR, independent of the AGP 9 migration? This can be done on AGP 8.7.2 but needs testing.

---

## 6. CSV Export Security

**Current improvement:** Error messages no longer leak internal details.

**Additional options:**
- **Password-protect exports** — Zip the CSV with a user-provided password
- **Restrict to app-private directory** — Instead of using the system file picker (which allows saving anywhere), save to app-private storage and share via `FileProvider`
- **Add a warning dialog** — Show "This file contains your financial data" before exporting

**Question:** Which level of protection do you want for CSV exports?

---

## 7. Cloud Sync (Supabase) — Future Feature

The README mentions **opt-in Supabase synchronization** as a planned feature. If you proceed with this:
- The `network_security_config.xml` I added already enforces HTTPS-only
- You'll need to add proper API key management (NOT hardcoded — use `BuildConfig` or a secrets gradle plugin)
- Consider end-to-end encryption for synced financial data
- Implement proper auth (Supabase Auth with email/OAuth)

**Question:** Is cloud sync still on the roadmap? If so, what timeline?

---

## 8. Compose BOM 2026.03.01 Compatibility

I upgraded the Compose BOM from `2025.01.01` to `2026.03.01`. While Compose UI libraries are generally binary-compatible across Kotlin versions, there's a small risk that the newer Compose runtime has minimum Kotlin requirements.

**Action needed:** After syncing this branch in Android Studio, run a clean build. If you see errors like `Incompatible Kotlin version`, you may need to either:
- Downgrade Compose BOM to `2025.05.01` (known compatible with Kotlin 2.0.x)
- Or upgrade Kotlin to 2.1.x+ (also requires KSP update)

---

## Summary of What Was Done (No Questions)

These changes were made without needing your input:

| Change | Why |
|---|---|
| Room 2.6.1 → 2.8.4 | Bug fixes, performance improvements, stable |
| Hilt 2.52 → 2.59.2 | Bug fixes, AGP compatibility |
| Compose BOM → 2026.03.01 | Latest stable UI libraries |
| AndroidX libraries upgraded | Security patches, bug fixes |
| R8 enabled for release | Code obfuscation + APK shrinking |
| ProGuard rules added | Prevents R8 from breaking Hilt/Room/Compose |
| Backup rules hardened | Database excluded from cloud backup |
| Network security config | HTTPS-only enforced |
| Destructive migration scoped | Only v1→v2; future migrations must be explicit |
| Error message sanitized | No more stack traces or internal paths in Toast |
| Stale files removed | Build logs, test outputs cleaned from repo |
| Gradle properties cleaned | Removed deprecated/unnecessary flags |
