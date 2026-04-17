package com.gbg.smartcapture.bigmagic.viewmodel

import com.gbg.smartcapture.bigmagic.data.SettingsGroup
import com.gbg.smartcapture.bigmagic.data.SettingsManualCaptureToggleDelayType
import com.gbg.smartcapture.bigmagic.data.SettingsSwitch
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/** One-shot capture request fired each time we need to launch DocumentCameraActivity. */
enum class CaptureSide { FRONT, BACK }

interface IRootViewModel {

    // ------------------------ Verification flow ------------------------

    val verificationState: StateFlow<VerificationUiState>

    /** Fires once per capture — the activity launches DocumentCameraActivity on each emission. */
    val captureRequests: SharedFlow<CaptureSide>

    /** One-shot messages (e.g. "Starting a fresh verification") for Toast/snackbar display. */
    val transientMessages: SharedFlow<String>

    /** True once the IVS API key is resolved from BuildConfig seed or Keystore cache. */
    val hasApiKey: StateFlow<Boolean>

    /** The most recently created session id — used to pre-fill the debug poll panel. */
    val lastSessionId: StateFlow<String?>

    fun onStart()
    fun onFrontCaptured(jpeg: ByteArray)
    fun onFlipContinue()
    fun onBackCaptured(jpeg: ByteArray)
    fun onCaptureCancelled()
    fun onPollRetry()
    fun onRefreshTerminal()
    fun onReset()

    /** Debug-only: poll a known session id without running capture/submit. */
    fun onDebugPoll(sessionId: String)

    // ------------------------ Settings ------------------------

    val manualCaptureToggleDelayState: StateFlow<SettingsManualCaptureToggleDelayType>
    val settings: SettingsGroup

    fun setManualCaptureToggleDelay(option: SettingsManualCaptureToggleDelayType)
    fun setSettingSwitch(switch: SettingsSwitch, value: Boolean)
}
