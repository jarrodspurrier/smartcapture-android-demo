package com.gbg.smartcapture.bigmagic.ivs

import android.util.Log
import com.gbg.smartcapture.bigmagic.data.ApiKeyStore
import com.gbg.smartcapture.bigmagic.data.DeviceInfoPayload
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Orchestrates the full IVS verification lifecycle on top of [IvsApi].
 *
 * Session ID lives here for the duration of a verification attempt. Callers do not need to
 * thread it through — they call [startSession], [submitCaptured], [pollStatus] / [retryPolling]
 * in order and the repository holds the state.
 */
class IvsRepository(
    apiKeyStore: ApiKeyStore,
    private val deviceInfoProvider: () -> DeviceInfoPayload? = { null },
    private val api: IvsApi = IvsApi(apiKeyProvider = { apiKeyStore.getOrSeedApiKey() })
) {

    @Volatile
    private var activeSessionId: String? = null

    val currentSessionId: String? get() = activeSessionId

    /** Step 1 — create a session. Stores the id for subsequent calls. */
    suspend fun startSession(referenceId: String? = null): IvsResult<String> {
        val outcome = api.createSession(deviceInfoProvider(), referenceId)
        if (outcome is IvsResult.Success) {
            activeSessionId = outcome.value.sessionId
            return IvsResult.Success(outcome.value.sessionId)
        }
        return (outcome as IvsResult.Failure)
    }

    /**
     * Step 3 — submit captured front/back JPEG bytes.
     *
     * Behaviour (per guide):
     * - 409 "Already submitted" is treated as success — it means the original submit landed.
     * - 400 "Session expired" triggers a silent retry: create a fresh session and resubmit once.
     *   On success the caller sees [SilentReseedOutcome.Reseeded] so it can toast the user.
     */
    suspend fun submitCaptured(
        frontJpeg: ByteArray,
        backJpeg: ByteArray
    ): IvsResult<SilentReseedOutcome> {
        val sessionId = activeSessionId
            ?: return IvsResult.Failure(IvsError.Unexpected("No active session"))

        val first = api.submitImages(sessionId, frontJpeg, backJpeg)
        when (first) {
            is IvsResult.Success -> return IvsResult.Success(SilentReseedOutcome.Original)
            is IvsResult.Failure -> when (first.error) {
                is IvsError.AlreadySubmitted -> return IvsResult.Success(SilentReseedOutcome.AlreadyAccepted)
                is IvsError.SessionExpired -> return reseedAndResubmit(frontJpeg, backJpeg)
                else -> return first
            }
        }
    }

    private suspend fun reseedAndResubmit(
        frontJpeg: ByteArray,
        backJpeg: ByteArray
    ): IvsResult<SilentReseedOutcome> {
        Log.i(TAG, "Session expired — silently creating new session and resubmitting")
        val newSession = startSession()
        if (newSession is IvsResult.Failure) return newSession
        val sessionId = activeSessionId
            ?: return IvsResult.Failure(IvsError.Unexpected("Reseed produced no session id"))
        return when (val retry = api.submitImages(sessionId, frontJpeg, backJpeg)) {
            is IvsResult.Success -> IvsResult.Success(SilentReseedOutcome.Reseeded)
            is IvsResult.Failure -> retry
        }
    }

    /**
     * Step 4 — poll every [IvsConfig.POLL_INTERVAL_MS] up to [IvsConfig.POLL_MAX_ATTEMPTS] times.
     *
     * Emits [PollEvent.Attempt] before each poll, then either [PollEvent.Terminal] on a terminal
     * status, [PollEvent.Exhausted] after the attempt budget is spent, or [PollEvent.Error] on
     * a non-retryable error. The returned Flow ends after any terminal event.
     */
    fun pollStatus(): Flow<PollEvent> = pollFlow(startAttempt = 1)

    /** Try Again — resets the attempt counter, keeps the same session id. */
    fun retryPolling(): Flow<PollEvent> = pollFlow(startAttempt = 1)

    /** Poll a specific session id (debug helper — independent of startSession). */
    fun pollStatusFor(sessionId: String): Flow<PollEvent> {
        activeSessionId = sessionId
        return pollFlow(startAttempt = 1)
    }

    private fun pollFlow(startAttempt: Int): Flow<PollEvent> = flow {
        val sessionId = activeSessionId
        if (sessionId == null) {
            emit(PollEvent.Error(IvsError.Unexpected("No active session to poll")))
            return@flow
        }

        var attempt = startAttempt
        var lastStatus = SessionStatus.Unknown

        while (attempt <= IvsConfig.POLL_MAX_ATTEMPTS) {
            emit(PollEvent.Attempt(attempt, lastStatus))
            when (val outcome = api.getSession(sessionId)) {
                is IvsResult.Success -> {
                    val response = outcome.value
                    val status = response.statusEnum()
                    val terminal = response.isTerminalNow()
                    Log.i(TAG, "poll attempt=$attempt rawStatus=${response.status} " +
                            "terminal=$terminal instanceId=${response.instanceId} " +
                            "completedAt=${response.completedAt}")
                    lastStatus = status
                    if (terminal) {
                        emit(PollEvent.Terminal(response))
                        return@flow
                    }
                }
                is IvsResult.Failure -> {
                    emit(PollEvent.Error(outcome.error))
                    return@flow
                }
            }
            if (attempt < IvsConfig.POLL_MAX_ATTEMPTS) {
                delay(IvsConfig.POLL_INTERVAL_MS)
            }
            attempt += 1
        }

        emit(PollEvent.Exhausted(lastStatus))
    }

    fun reset() {
        activeSessionId = null
    }

    enum class SilentReseedOutcome {
        /** Happy path: original submit accepted. */
        Original,
        /** 409 on the first attempt — guide says original submit already succeeded. */
        AlreadyAccepted,
        /** Session had expired (30-min limit); a fresh one was created transparently. */
        Reseeded
    }

    companion object {
        private const val TAG = "IvsRepository"
    }
}
