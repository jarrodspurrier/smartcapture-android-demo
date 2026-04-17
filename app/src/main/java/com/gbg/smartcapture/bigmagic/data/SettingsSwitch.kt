package com.gbg.smartcapture.bigmagic.data

import com.gbg.smartcapture.bigmagic.R

/**
 * Don't change the enum *names* here — they are used as keys in the DataStore.
 * Adding or removing entries is fine.
 */
enum class SettingsSwitch(
    val title: Int,
    val defaultValue: Boolean = false,
) {
    MANUAL_CAPTURE_TOGGLE(R.string.settings_manual_capture_toggle, true),
    SHOW_CAPTURE_PREVIEW(R.string.settings_show_capture_preview, false),
}
