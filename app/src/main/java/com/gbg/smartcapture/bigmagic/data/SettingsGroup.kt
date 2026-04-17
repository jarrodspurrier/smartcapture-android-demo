package com.gbg.smartcapture.bigmagic.data

import kotlinx.coroutines.flow.StateFlow

data class SettingsGroup(
    val manualCaptureToggle: StateFlow<Boolean>,
    val showCapturePreview: StateFlow<Boolean>,
)
