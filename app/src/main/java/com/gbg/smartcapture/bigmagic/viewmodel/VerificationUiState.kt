package com.gbg.smartcapture.bigmagic.viewmodel

import com.gbg.smartcapture.bigmagic.ivs.SessionStatusResponse

/**
 * Drives the entire verification flow. Each state is one screen (or one "launch the SDK"
 * trigger) in the user-facing journey.
 *
 * Terminal states are [Terminal] (IVS responded with a result — may be pass/attention/fail)
 * and [Failed] (infrastructure-level error before IVS returned anything useful).
 */
sealed class VerificationUiState {
    data object Idle : VerificationUiState()
    data object CreatingSession : VerificationUiState()

    /** Trigger: activity should launch DocumentCamera for the front. */
    data object AwaitingFront : VerificationUiState()

    /** Front bytes captured; show "flip the document" prompt. */
    @Suppress("ArrayInDataClass")
    data class FlipPrompt(val front: ByteArray) : VerificationUiState()

    /** Trigger: activity should launch DocumentCamera for the back. */
    @Suppress("ArrayInDataClass")
    data class AwaitingBack(val front: ByteArray) : VerificationUiState()

    data object Submitting : VerificationUiState()
    data class Polling(val attempt: Int) : VerificationUiState()
    data class PollingExhausted(val sessionId: String) : VerificationUiState()

    /** IVS returned a final status (completed/failed/error/timed_out). */
    data class Terminal(val response: SessionStatusResponse) : VerificationUiState()

    /** Failed before IVS could respond: network, 401, malformed payload, etc. */
    data class Failed(val reason: String) : VerificationUiState()
}
