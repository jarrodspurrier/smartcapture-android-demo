package com.gbg.smartcapture.bigmagic.data

import com.gbg.smartcapture.bigmagic.R

/**
 * Enum "Name" of each option is used to save - don't change the spelling. Other options can be added.
 * The title can be changed any time as well as the value.
 */
enum class SettingsManualCaptureToggleDelayType(
    val valueMillis: Long = 0,
    val titleResource: Int = 0,
) {
    OPTION_15_SECONDS(valueMillis = 15000, titleResource = R.string.setting_manual_capture_toggle_delay_15_seconds),
    OPTION_10_SECONDS(valueMillis = 10000, titleResource = R.string.setting_manual_capture_toggle_delay_10_seconds),
    OPTION_5_SECONDS(valueMillis = 5000, titleResource = R.string.setting_manual_capture_toggle_delay_5_seconds),
    OPTION_ALWAYS_ON(valueMillis = 0, titleResource = R.string.setting_manual_capture_toggle_delay_always_on);

    companion object {
        val DEFAULT_OPTION: SettingsManualCaptureToggleDelayType = OPTION_ALWAYS_ON
        val DATASTORE_KEY: String = this::class.java.name
        val ALL: List<SettingsManualCaptureToggleDelayType> = entries.toList()

        fun getOption(name: String): SettingsManualCaptureToggleDelayType =
            entries.firstOrNull { it.name == name } ?: DEFAULT_OPTION

    }

}