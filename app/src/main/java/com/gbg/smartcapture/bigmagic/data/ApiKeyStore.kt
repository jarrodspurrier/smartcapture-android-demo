package com.gbg.smartcapture.bigmagic.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.gbg.smartcapture.bigmagic.BuildConfig

/**
 * Android Keystore-backed storage for the IVS API key.
 *
 * The IVS integration guide forbids plaintext SharedPreferences for this secret; values here
 * are written via [EncryptedSharedPreferences] with a [MasterKey] rooted in Android Keystore.
 *
 * On first launch, if the store is empty we seed from `BuildConfig.IVS_API_KEY_SEED`
 * (populated from `ivsApiKey` in `local.properties`). Subsequent launches read from the
 * encrypted store only — `BuildConfig` is used purely as a one-time seed.
 */
class ApiKeyStore(context: Context) {

    private val appContext = context.applicationContext
    private val prefs: SharedPreferences by lazy { openPrefs() }

    fun getOrSeedApiKey(): String? {
        val cached = prefs.getString(KEY_API_KEY, null)?.takeIf { it.isNotBlank() }
        if (cached != null) return cached

        val seed = BuildConfig.IVS_API_KEY_SEED.takeIf { it.isNotBlank() } ?: return null
        prefs.edit().putString(KEY_API_KEY, seed).apply()
        return seed
    }

    fun hasApiKey(): Boolean = getOrSeedApiKey() != null

    private fun openPrefs(): SharedPreferences {
        val masterKey = MasterKey.Builder(appContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return try {
            createEncrypted(masterKey)
        } catch (t: Throwable) {
            // Recover from key-corruption (e.g. device restored from backup) by wiping once.
            Log.w(TAG, "EncryptedSharedPreferences open failed, resetting", t)
            appContext.deleteSharedPreferences(FILE_NAME)
            createEncrypted(masterKey)
        }
    }

    private fun createEncrypted(masterKey: MasterKey): SharedPreferences =
        EncryptedSharedPreferences.create(
            appContext,
            FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

    companion object {
        private const val TAG = "ApiKeyStore"
        private const val FILE_NAME = "ivs_secure_prefs"
        private const val KEY_API_KEY = "ivs_api_key"
    }
}
