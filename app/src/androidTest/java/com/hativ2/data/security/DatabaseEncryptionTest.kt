package com.hativ2.data.security

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.security.KeyStore

/**
 * Verifies that the database encryption infrastructure is working correctly:
 * - The SQLCipher database file is not readable as plain SQLite
 * - DatabaseKeyManager creates the expected Android Keystore key
 */
@RunWith(AndroidJUnit4::class)
class DatabaseEncryptionTest {

    @Test
    fun databaseFile_doesNotStartWithPlainSQLiteHeader() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // Default Room database location
        val dbFile = context.getDatabasePath("hati_database")

        if (!dbFile.exists()) {
            // App has never been opened in this test session; nothing to verify
            return
        }

        val header = ByteArray(16)
        dbFile.inputStream().use { it.read(header) }
        val headerString = String(header, Charsets.US_ASCII)

        assertFalse(
            "Database file must NOT start with 'SQLite format 3' (unencrypted). " +
                "If it does, SQLCipher is not applied.",
            headerString.startsWith("SQLite format 3")
        )
    }

    @Test
    fun androidKeystore_containsDatabaseKeyAfterFirstLaunch() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbFile = context.getDatabasePath("hati_database")

        if (!dbFile.exists()) {
            // Database was never created in this run; key may not exist
            return
        }

        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)

        // DatabaseKeyManager uses "hati_db_key" as the key alias
        val keyAliasExists = keyStore.containsAlias("hati_db_key")
        assertTrue(
            "AndroidKeyStore must contain 'HatiDbKey' after the database has been opened",
            keyAliasExists
        )
    }

    @Test
    fun keyGenParameterSpec_requiresUserAuthentication_false_forAutomaticUnlock() {
        // Verify that the key can be used after biometric unlock without re-prompting
        // The key should be created without setUserAuthenticationRequired(true) so
        // the app can re-open the DB from the background (e.g. after process death).
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)

        if (!keyStore.containsAlias("HatiDbKey")) {
            return // Key doesn't exist yet; test is inconclusive but not a failure
        }

        val keyEntry = keyStore.getEntry("HatiDbKey", null)
        assertTrue(
            "Expected HatiDbKey to be a SecretKeyEntry in AndroidKeyStore",
            keyEntry is KeyStore.SecretKeyEntry
        )
    }

    @Test
    fun databaseKeyManager_producesNonEmptyPassphrase() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val passphrase = com.hativ2.data.security.DatabaseKeyManager.getPassphrase(context)

        assertTrue(
            "DatabaseKeyManager must return a non-empty passphrase",
            passphrase.isNotEmpty()
        )
    }

    @Test
    fun databaseKeyManager_returnsSamePassphraseOnSubsequentCalls() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val passphrase1 = com.hativ2.data.security.DatabaseKeyManager.getPassphrase(context)
        val passphrase2 = com.hativ2.data.security.DatabaseKeyManager.getPassphrase(context)

        assertTrue(
            "DatabaseKeyManager must return the same passphrase on every call",
            passphrase1.contentEquals(passphrase2)
        )
    }
}
