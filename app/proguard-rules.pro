# ============================================================================
# HATI v2 - ProGuard / R8 Rules
# ============================================================================
# Why: R8 code shrinking + obfuscation is enabled for release builds to:
#   1. Reduce APK size by removing unused code
#   2. Obfuscate class/method names to deter reverse engineering
#   3. Protect business logic (debt calculation, export) from extraction
# ============================================================================

# --- Room ---
# Room uses reflection for database creation and entity mapping.
# Without these rules, R8 would strip or rename classes Room needs at runtime.
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-dontwarn androidx.room.paging.**

# --- Hilt / Dagger ---
# Hilt generates code at compile time but uses reflection for module discovery.
# Keeping generated Hilt components prevents runtime DI failures.
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
-dontwarn dagger.hilt.internal.aggregatedroot.codegen.**

# --- Kotlin ---
# Kotlin metadata is used by serialization and reflection libraries.
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**

# --- Compose ---
# Compose runtime relies on specific class structures for recomposition.
-dontwarn androidx.compose.**

# --- Coroutines ---
# Coroutines use internal classes that R8 may incorrectly remove.
-dontwarn kotlinx.coroutines.**
-keep class kotlinx.coroutines.** { *; }

# --- Data classes used in Room entities ---
# Ensures all entity fields are preserved for SQLite column mapping.
-keepclassmembers class com.hativ2.data.entity.** { *; }
-keepclassmembers class com.hativ2.domain.model.** { *; }

# --- General Android ---
# Prevents stripping of Parcelable implementations used in navigation.
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# --- SQLCipher ---
# SQLCipher uses native libraries and reflection for database factory creation.
# Without these rules, R8 strips the JNI bindings and the SupportFactory class.
-keep class net.sqlcipher.** { *; }
-keep class net.sqlcipher.database.** { *; }
-dontwarn net.sqlcipher.**

# --- Biometric ---
# Biometric library uses fragment transactions internally.
-dontwarn androidx.biometric.**
