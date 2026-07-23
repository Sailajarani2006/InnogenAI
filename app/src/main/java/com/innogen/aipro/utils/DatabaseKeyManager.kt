package com.innogen.aipro.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

/**
 * FIX-09 (MED-005): Manages the SQLCipher encryption key for the Room database.
 *
 * The key is generated once using a cryptographically secure RNG, then stored in
 * EncryptedSharedPreferences (backed by AES-256-GCM via Android Keystore).
 * This ensures:
 *  - The database file is unreadable without the device's Keystore unlock.
 *  - The key itself is never stored in plaintext.
 *  - The same key is recovered across app restarts.
 */
@Singleton
class DatabaseKeyManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val keyAlias = "innogen_db_key"

    /**
     * Returns the database passphrase as a CharArray.
     * Generates and persists a new key on first call.
     */
    fun getDatabaseKey(): CharArray {
        val prefs = buildEncryptedPrefs()
        return prefs.getString(keyAlias, null)?.toCharArray()
            ?: generateAndStoreKey(prefs)
    }

    // ── Private helpers ─────────────────────────────────────────────────────

    private fun buildEncryptedPrefs(): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            "innogen_db_key_store",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private fun generateAndStoreKey(prefs: SharedPreferences): CharArray {
        val key = generateSecureKey()
        prefs.edit().putString(keyAlias, String(key)).apply()
        return key
    }

    /**
     * Generates a 32-byte cryptographically random passphrase, base64-encoded.
     * Result length: 44 characters (43 printable + 1 padding = 256-bit entropy).
     */
    private fun generateSecureKey(): CharArray {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.NO_WRAP).toCharArray()
    }
}
