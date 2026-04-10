package com.hativ2.data.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Manages the encryption passphrase for the SQLCipher-encrypted Room database.
 *
 * Architecture decision: The passphrase is generated once and stored encrypted
 * in SharedPreferences. The encryption key lives in the Android Keystore (TEE),
 * which is hardware-backed on most devices and never leaves the secure enclave.
 *
 * Why Android Keystore + AES-GCM instead of a hardcoded passphrase:
 *   - A hardcoded string can be extracted from the APK via reverse engineering.
 *   - Keystore keys are non-extractable — even root cannot read the raw key material
 *     on devices with a hardware-backed Keystore (StrongBox / TEE).
 *   - AES-GCM provides authenticated encryption, detecting tampering.
 *
 * Why not EncryptedSharedPreferences:
 *   - It adds the full Tink dependency (~500KB) for a single key-value pair.
 *   - Direct Keystore usage is lighter and gives us explicit control over the
 *     cipher parameters.
 */
object DatabaseKeyManager {
    private const val KEYSTORE_ALIAS = "hati_db_key"
    private const val PREFS_NAME = "hati_db_prefs"
    private const val PREF_ENCRYPTED_PASSPHRASE = "encrypted_passphrase"
    private const val PREF_IV = "passphrase_iv"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val GCM_TAG_LENGTH = 128

    /**
     * Returns the database passphrase as a [ByteArray].
     * On first call, generates a random passphrase and stores it encrypted.
     * On subsequent calls, decrypts and returns the stored passphrase.
     */
    fun getPassphrase(context: Context): ByteArray {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val storedEncrypted = prefs.getString(PREF_ENCRYPTED_PASSPHRASE, null)
        val storedIv = prefs.getString(PREF_IV, null)

        return if (storedEncrypted != null && storedIv != null) {
            decryptPassphrase(
                android.util.Base64.decode(storedEncrypted, android.util.Base64.NO_WRAP),
                android.util.Base64.decode(storedIv, android.util.Base64.NO_WRAP)
            )
        } else {
            val passphrase = generateRandomPassphrase()
            val (encrypted, iv) = encryptPassphrase(passphrase)
            prefs.edit()
                .putString(
                    PREF_ENCRYPTED_PASSPHRASE,
                    android.util.Base64.encodeToString(encrypted, android.util.Base64.NO_WRAP)
                )
                .putString(
                    PREF_IV,
                    android.util.Base64.encodeToString(iv, android.util.Base64.NO_WRAP)
                )
                .apply()
            passphrase
        }
    }

    private fun generateRandomPassphrase(): ByteArray {
        // 32 bytes = 256-bit passphrase. SQLCipher derives the actual key via PBKDF2.
        val bytes = ByteArray(32)
        java.security.SecureRandom().nextBytes(bytes)
        return bytes
    }

    private fun getOrCreateKeystoreKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)

        keyStore.getEntry(KEYSTORE_ALIAS, null)?.let { entry ->
            return (entry as KeyStore.SecretKeyEntry).secretKey
        }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )
        keyGenerator.init(
            KeyGenParameterSpec.Builder(
                KEYSTORE_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return keyGenerator.generateKey()
    }

    private fun encryptPassphrase(passphrase: ByteArray): Pair<ByteArray, ByteArray> {
        val key = getOrCreateKeystoreKey()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val encrypted = cipher.doFinal(passphrase)
        return Pair(encrypted, cipher.iv)
    }

    private fun decryptPassphrase(encrypted: ByteArray, iv: ByteArray): ByteArray {
        val key = getOrCreateKeystoreKey()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))
        return cipher.doFinal(encrypted)
    }
}
