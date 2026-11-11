@file:Suppress("DEPRECATION")

package com.fearmikey.garage.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Opens an [EncryptedSharedPreferences] file, recovering if it can't be decrypted.
 *
 * The file's keyset is encrypted with a key in the Android Keystore, which never leaves
 * the device. If the file arrives without that key (an older backup/restore or device
 * transfer, or a Keystore reset), opening it throws on every launch. In that case the
 * contents are unrecoverable anyway, so the file is deleted and recreated empty; the user
 * just has to re-enter their credentials.
 */
internal object SecurePreferences {

    private const val TAG = "SecurePreferences"

    /** Files opened through this helper; excluded from device transfer in data_extraction_rules.xml. */
    const val LUBELOGGER_CREDENTIALS = "lubelogger_credentials"
    const val CLOUD_BACKUP_SECURE_PREFS = "cloud_backup_secure_prefs"

    fun open(context: Context, name: String): SharedPreferences =
        try {
            create(context, name)
        } catch (e: Exception) {
            Log.w(TAG, "Encrypted preferences '$name' are unreadable; resetting them", e)
            context.deleteSharedPreferences(name)
            create(context, name)
        }

    private fun create(context: Context, name: String): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            name,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }
}
