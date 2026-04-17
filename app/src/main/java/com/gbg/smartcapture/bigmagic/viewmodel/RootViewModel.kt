package com.gbg.smartcapture.bigmagic.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gbg.smartcapture.bigmagic.data.ApiKeyStore
import com.gbg.smartcapture.bigmagic.data.LastSessionStore
import com.gbg.smartcapture.bigmagic.data.SettingsDataStore
import com.gbg.smartcapture.bigmagic.data.SettingsGroup
import com.gbg.smartcapture.bigmagic.data.SettingsManualCaptureToggleDelayType
import com.gbg.smartcapture.bigmagic.data.SettingsSwitch
import com.gbg.smartcapture.bigmagic.ivs.IvsError
import com.gbg.smartcapture.bigmagic.ivs.IvsRepository
import com.gbg.smartcapture.bigmagic.ivs.IvsResult
import com.gbg.smartcapture.bigmagic.ivs.PollEvent
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RootViewModel(application: Application) : IRootViewModel, AndroidViewModel(application) {

    companion object {
        private const val TAG = "RootViewModel"
        private const val DATASTORE_SUBSCRIBE_TIMEOUT_MILLIS = 5_000L
    }

    private val settingStore = SettingsDataStore(getApplication())
    private val apiKeyStore = ApiKeyStore(getApplication())
    private val lastSessionStore = LastSessionStore(getApplication())
    private val repository = IvsRepository(apiKeyStore)

    private val _lastSessionId = MutableStateFlow(lastSessionStore.lastSessionId)
    override val lastSessionId: StateFlow<String?> = _lastSessionId.asStateFlow()

    private val _state = MutableStateFlow<VerificationUiState>(VerificationUiState.Idle)
    override val verificationState: StateFlow<VerificationUiState> = _state.asStateFlow()

    private fun setState(next: VerificationUiState) {
        Log.i(TAG, "state: ${_state.value::class.simpleName} -> ${next::class.simpleName}")
        _state.value = next
    }

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    override val transientMessages: SharedFlow<String> = _messages.asSharedFlow()

    private val _captureRequests = MutableSharedFlow<CaptureSide>(extraBufferCapacity = 4)
    override val captureRequests: SharedFlow<CaptureSide> = _captureRequests.asSharedFlow()

    override val hasApiKey: StateFlow<Boolean> = MutableStateFlow(apiKeyStore.hasApiKey()).asStateFlow()

    override val manualCaptureToggleDelayState: StateFlow<SettingsManualCaptureToggleDelayType> =
        settingStore.getManualCaptureToggleDelay().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(DATASTORE_SUBSCRIBE_TIMEOUT_MILLIS),
            initialValue = SettingsManualCaptureToggleDelayType.DEFAULT_OPTION
        )

    override val settings: SettingsGroup = SettingsGroup(
        manualCaptureToggle = switchFlow(SettingsSwitch.MANUAL_CAPTURE_TOGGLE),
        showCapturePreview = switchFlow(SettingsSwitch.SHOW_CAPTURE_PREVIEW),
    )

    private var pollJob: Job? = null

    // ---------------- Verification flow ----------------

    override fun onStart() {
        if (!apiKeyStore.hasApiKey()) {
            setState(
                VerificationUiState.Failed(
                    "IVS API key missing. Add `ivsApiKey=…` to local.properties and rebuild."
                )
            )
            return
        }
        setState(VerificationUiState.CreatingSession)
        viewModelScope.launch {
            when (val outcome = repository.startSession()) {
                is IvsResult.Success -> {
                    lastSessionStore.lastSessionId = outcome.value
                    _lastSessionId.value = outcome.value
                    setState(VerificationUiState.AwaitingFront)
                    _captureRequests.tryEmit(CaptureSide.FRONT)
                }
                is IvsResult.Failure -> setState(VerificationUiState.Failed(describe(outcome.error)))
            }
        }
    }

    override fun onFrontCaptured(jpeg: ByteArray) {
        val current = _state.value
        if (current !is VerificationUiState.AwaitingFront) {
            Log.w(TAG, "onFrontCaptured in unexpected state $current")
            return
        }
        setState(VerificationUiState.FlipPrompt(jpeg))
    }

    override fun onFlipContinue() {
        val current = _state.value
        if (current !is VerificationUiState.FlipPrompt) return
        setState(VerificationUiState.AwaitingBack(current.front))
        _captureRequests.tryEmit(CaptureSide.BACK)
    }

    override fun onBackCaptured(jpeg: ByteArray) {
        val current = _state.value
        if (current !is VerificationUiState.AwaitingBack) {
            Log.w(TAG, "onBackCaptured in unexpected state $current")
            return
        }
        val front = current.front
        setState(VerificationUiState.Submitting)
        viewModelScope.launch { submitAndPoll(front, jpeg) }
    }

    override fun onCaptureCancelled() {
        val current = _state.value
        if (current is VerificationUiState.AwaitingFront ||
            current is VerificationUiState.AwaitingBack ||
            current is VerificationUiState.FlipPrompt
        ) {
            repository.reset()
            setState(VerificationUiState.Idle)
        }
    }

    private suspend fun submitAndPoll(front: ByteArray, back: ByteArray) {
        when (val submitResult = repository.submitCaptured(front, back)) {
            is IvsResult.Success -> {
                if (submitResult.value == IvsRepository.SilentReseedOutcome.Reseeded) {
                    _messages.tryEmit("Starting a fresh verification")
                }
                startPolling(repository.pollStatus())
            }
            is IvsResult.Failure -> setState(VerificationUiState.Failed(describe(submitResult.error)))
        }
    }

    private fun startPolling(flow: Flow<PollEvent>) {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            flow.collect { event ->
                when (event) {
                    is PollEvent.Attempt -> setState(VerificationUiState.Polling(event.attemptNumber))
                    is PollEvent.Terminal -> setState(VerificationUiState.Terminal(event.response))
                    is PollEvent.Exhausted -> setState(
                        VerificationUiState.PollingExhausted(repository.currentSessionId.orEmpty())
                    )
                    is PollEvent.Error -> setState(VerificationUiState.Failed(describe(event.error)))
                }
            }
        }
    }

    override fun onPollRetry() {
        if (_state.value !is VerificationUiState.PollingExhausted) return
        startPolling(repository.retryPolling())
    }

    override fun onReset() {
        pollJob?.cancel()
        pollJob = null
        repository.reset()
        setState(VerificationUiState.Idle)
    }

    override fun onDebugPoll(sessionId: String) {
        if (sessionId.isBlank()) return
        pollJob?.cancel()
        setState(VerificationUiState.Polling(1))
        pollJob = viewModelScope.launch {
            repository.pollStatusFor(sessionId).collect { event ->
                when (event) {
                    is PollEvent.Attempt -> setState(VerificationUiState.Polling(event.attemptNumber))
                    is PollEvent.Terminal -> setState(VerificationUiState.Terminal(event.response))
                    is PollEvent.Exhausted -> setState(VerificationUiState.PollingExhausted(sessionId))
                    is PollEvent.Error -> setState(VerificationUiState.Failed(describe(event.error)))
                }
            }
        }
    }

    // ---------------- Settings ----------------

    override fun setManualCaptureToggleDelay(option: SettingsManualCaptureToggleDelayType) {
        viewModelScope.launch { settingStore.setManualCaptureToggleDelay(option) }
    }

    override fun setSettingSwitch(switch: SettingsSwitch, value: Boolean) {
        viewModelScope.launch { settingStore.setSwitch(switch, value) }
    }

    // ---------------- Helpers ----------------

    private fun switchFlow(switch: SettingsSwitch): StateFlow<Boolean> =
        settingStore.getSwitchFlow(switch).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(DATASTORE_SUBSCRIBE_TIMEOUT_MILLIS),
            initialValue = switch.defaultValue
        )

    private fun describe(error: IvsError): String = when (error) {
        is IvsError.SessionExpired -> "Session expired before we could retry automatically."
        is IvsError.BadRequest -> "Request rejected by IVS: ${error.message}"
        is IvsError.Unauthorized -> "API key was not accepted by IVS."
        is IvsError.SessionNotFound -> "Session could not be found on IVS."
        is IvsError.AlreadySubmitted -> "IVS reports images were already submitted."
        is IvsError.RateLimited -> "Rate limit hit — wait a moment and try again."
        is IvsError.ServerError -> "IVS server error (${error.code})."
        is IvsError.Network -> "Network error: ${error.message}"
        is IvsError.Unexpected -> "Unexpected error: ${error.message}"
    }
}
