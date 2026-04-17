package com.gbg.smartcapture.bigmagic.ivs

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// ------------------------------------------------------------
// Step 1 — Create Session
// ------------------------------------------------------------

@JsonClass(generateAdapter = true)
data class CreateSessionRequest(
    val verificationType: String = "docAuth",
    val returnUrl: String = "gbgdemo://result",
    val faceMatchEnabled: Boolean = false,
    val sensorType: String = "Mobile",
    val referenceId: String? = null,
    val sdkVersion: String? = null
)

@JsonClass(generateAdapter = true)
data class CreateSessionResponse(
    val sessionId: String,
    val verifyUrl: String? = null,
    val expiresAt: String? = null
)

// ------------------------------------------------------------
// Step 3 — Submit Images
// ------------------------------------------------------------

@JsonClass(generateAdapter = true)
data class SubmitImagesRequest(
    val frontImage: String,
    val backImage: String,
    val sdkVersion: String? = null
)

@JsonClass(generateAdapter = true)
data class SubmitImagesResponse(
    val sessionId: String,
    val status: String
)

// ------------------------------------------------------------
// Step 4 — Poll Status
// ------------------------------------------------------------

@JsonClass(generateAdapter = true)
data class SessionStatusResponse(
    val sessionId: String,
    val status: String,
    val instanceId: String? = null,
    val result: VerificationResult? = null,
    val createdAt: String? = null,
    val completedAt: String? = null
) {
    fun statusEnum(): SessionStatus = SessionStatus.fromWire(status)

    /**
     * Server quirk: a freshly-submitted session briefly reports `status="failed"` with no engine
     * metadata before the engine completes and the same session transitions to `"completed"` with
     * a real `instanceId`/`completedAt`/`result`. Treat the empty variant as still pending so the
     * poll loop keeps trying instead of bailing to Terminal on the transient value.
     */
    fun isTerminalNow(): Boolean {
        val s = statusEnum()
        if (!s.isTerminal()) return false
        if (s == SessionStatus.Failed &&
            instanceId == null &&
            completedAt == null &&
            result == null
        ) return false
        return true
    }
}

@JsonClass(generateAdapter = true)
data class VerificationResult(
    val ivsOverallResult: String? = null,
    val decision: String? = null,
    val documentVerified: Boolean? = null,
    val documentExpired: Boolean? = null,
    val attentionNotices: List<String>? = null,
    val failureReason: String? = null
)

enum class SessionStatus(val wire: String) {
    Pending("pending"),
    Processing("processing"),
    Completed("completed"),
    Failed("failed"),
    Error("error"),
    TimedOut("timed_out"),
    Unknown("");

    fun isTerminal(): Boolean = when (this) {
        Completed, Failed, Error, TimedOut -> true
        else -> false
    }

    companion object {
        fun fromWire(raw: String): SessionStatus =
            entries.firstOrNull { it.wire.equals(raw, ignoreCase = true) } ?: Unknown
    }
}

// ------------------------------------------------------------
// Errors
// ------------------------------------------------------------

sealed class IvsError(open val message: String) {
    /** 400 — session expired (30 min lifetime); caller should silently create a new session. */
    data class SessionExpired(override val message: String = "Session expired") : IvsError(message)
    /** 400 — other bad request. */
    data class BadRequest(override val message: String) : IvsError(message)
    /** 401 — API key missing or invalid. */
    data class Unauthorized(override val message: String = "Unauthorized") : IvsError(message)
    /** 403 — session belongs to a different API key owner. */
    data class Forbidden(override val message: String = "Forbidden") : IvsError(message)
    /** 404 — session ID does not exist. */
    data class SessionNotFound(override val message: String = "Session not found") : IvsError(message)
    /** 409 — submit-once constraint hit; guide: treat as success on retry. */
    data class AlreadySubmitted(override val message: String = "Already submitted") : IvsError(message)
    /** 429 — rate limit exceeded (30 req/min). */
    data class RateLimited(override val message: String = "Rate limited") : IvsError(message)
    /** 5xx — server error. */
    data class ServerError(val code: Int, override val message: String) : IvsError(message)
    /** IOException — connection failed, timeout, etc. */
    data class Network(override val message: String) : IvsError(message)
    /** Anything else (parse failure, unexpected status, etc.). */
    data class Unexpected(override val message: String) : IvsError(message)
}

/** Result of an IVS network call. */
sealed class IvsResult<out T> {
    data class Success<T>(val value: T) : IvsResult<T>()
    data class Failure(val error: IvsError) : IvsResult<Nothing>()

    inline fun <R> map(transform: (T) -> R): IvsResult<R> = when (this) {
        is Success -> Success(transform(value))
        is Failure -> this
    }
}

// ------------------------------------------------------------
// Polling events emitted by IvsRepository.pollStatus()
// ------------------------------------------------------------

sealed class PollEvent {
    data class Attempt(val attemptNumber: Int, val status: SessionStatus) : PollEvent()
    data class Terminal(val response: SessionStatusResponse) : PollEvent()
    data class Exhausted(val lastStatus: SessionStatus) : PollEvent()
    data class Error(val error: IvsError) : PollEvent()
}

// Error payload shape is not guaranteed; parse best-effort.
@JsonClass(generateAdapter = true)
internal data class ApiErrorBody(
    @Json(name = "message") val message: String? = null,
    @Json(name = "error") val error: String? = null
)
