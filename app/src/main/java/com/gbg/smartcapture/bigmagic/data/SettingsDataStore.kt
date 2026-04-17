package com.gbg.smartcapture.bigmagic.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * DataStore-backed settings for document-capture tuning. API keys never live here —
 * those go through [ApiKeyStore] (EncryptedSharedPreferences backed by Android Keystore).
 */
class SettingsDataStore(private val context: Context) {

    companion object {
        private val KEY_MANUAL_CAPTURE_TOGGLE_DELAY = stringPreferencesKey(
            SettingsManualCaptureToggleDelayType.DATASTORE_KEY
        )
    }

    fun getSwitchFlow(setting: SettingsSwitch): Flow<Boolean> =
        context.settingsDataStore.data.map {
            it[booleanPreferencesKey(setting.name)] ?: setting.defaultValue
        }

    suspend fun setSwitch(setting: SettingsSwitch, value: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[booleanPreferencesKey(setting.name)] = value
        }
    }

    fun getManualCaptureToggleDelay(): Flow<SettingsManualCaptureToggleDelayType> =
        context.settingsDataStore.data.map { prefs ->
            SettingsManualCaptureToggleDelayType.getOption(
                prefs[KEY_MANUAL_CAPTURE_TOGGLE_DELAY]
                    ?: SettingsManualCaptureToggleDelayType.DEFAULT_OPTION.name
            )
        }

    suspend fun setManualCaptureToggleDelay(option: SettingsManualCaptureToggleDelayType) {
        context.settingsDataStore.edit { prefs ->
            prefs[KEY_MANUAL_CAPTURE_TOGGLE_DELAY] = option.name
        }
    }
}
