package com.gbg.smartcapture.bigmagic.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Remembers the most recently created IVS session id so the debug poll panel (and the
 * `gbgdemo://poll` deep link) can reuse it without the user typing it in.
 *
 * Not a secret — plain SharedPreferences is fine. Separate from [ApiKeyStore] which uses
 * Keystore-backed encryption.
 */
class LastSessionStore(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    var lastSessionId: String?
        get() = prefs.getString(KEY_LAST_SESSION, null)?.takeIf { it.isNotBlank() }
        set(value) {
            prefs.edit().apply {
                if (value.isNullOrBlank()) remove(KEY_LAST_SESSION) else putString(KEY_LAST_SESSION, value)
            }.apply()
        }

    companion object {
        private const val FILE_NAME = "ivs_debug_prefs"
        private const val KEY_LAST_SESSION = "last_session_id"
    }
}
