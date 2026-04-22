package com.gbg.smartcapture.bigmagic.viewmodel

import com.gbg.smartcapture.bigmagic.data.DeviceInfo
import com.gbg.smartcapture.bigmagic.data.SettingsGroup
import com.gbg.smartcapture.bigmagic.data.SettingsManualCaptureToggleDelayType
import com.gbg.smartcapture.bigmagic.data.SettingsSwitch
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/** Preview-only implementation used by @Preview composables. */
class MockedRootViewModel(
    initialState: VerificationUiState = VerificationUiState.Idle,
    hasKey: Boolean = true
) : IRootViewModel {

    override val verificationState: StateFlow<VerificationUiState> =
        MutableStateFlow(initialState).asStateFlow()

    override val transientMessages: SharedFlow<String> =
        MutableSharedFlow<String>().asSharedFlow()

    override val captureRequests: SharedFlow<CaptureSide> =
        MutableSharedFlow<CaptureSide>().asSharedFlow()

    override val hasApiKey: StateFlow<Boolean> =
        MutableStateFlow(hasKey).asStateFlow()

    override val lastSessionId: StateFlow<String?> =
        MutableStateFlow<String?>("vs_preview_session_id").asStateFlow()

    override val manualCaptureToggleDelayState: StateFlow<SettingsManualCaptureToggleDelayType> =
        MutableStateFlow(SettingsManualCaptureToggleDelayType.OPTION_10_SECONDS).asStateFlow()

    override val settings: SettingsGroup = SettingsGroup(
        manualCaptureToggle = MutableStateFlow(true).asStateFlow(),
        showCapturePreview = MutableStateFlow(false).asStateFlow(),
        saveRawImagesToGallery = MutableStateFlow(false).asStateFlow(),
    )

    override fun onStart() {}
    override fun onFrontCaptured(jpeg: ByteArray) {}
    override fun onFlipContinue() {}
    override fun onBackCaptured(jpeg: ByteArray) {}
    override fun onCaptureCancelled() {}
    override fun onPollRetry() {}
    override fun onRefreshTerminal() {}
    override fun onReset() {}
    override fun onDebugPoll(sessionId: String) {}
    override fun setManualCaptureToggleDelay(option: SettingsManualCaptureToggleDelayType) {}
    override fun setSettingSwitch(switch: SettingsSwitch, value: Boolean) {}

    override fun getDeviceInfo(): DeviceInfo = DeviceInfo(
        deviceId = "a1b2c3d4e5f60718",
        manufacturer = "Preview",
        model = "Preview Device",
        osVersion = "Android 13 (API 33)",
        timezone = "America/Chicago",
        network = "WiFi",
    )
}
