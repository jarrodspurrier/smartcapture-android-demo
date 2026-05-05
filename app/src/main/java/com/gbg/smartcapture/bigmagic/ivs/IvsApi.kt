package com.gbg.smartcapture.bigmagic.ivs

import android.util.Base64
import android.util.Log
import com.gbg.smartcapture.bigmagic.BuildConfig
import com.gbg.smartcapture.bigmagic.data.DeviceInfoPayload
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Thin OkHttp wrapper over the three IVS endpoints.
 *
 * Every request attaches `Authorization: Bearer <apiKey>`. The API key is resolved lazily per
 * call via [apiKeyProvider] so rotation doesn't require rebuilding the client.
 */
class IvsApi(
    private val apiKeyProvider: () -> String?,
    private val moshi: Moshi = defaultMoshi(),
    private val client: OkHttpClient = defaultClient()
) {

    private val createReqAdapter: JsonAdapter<CreateSessionRequest> =
        moshi.adapter(CreateSessionRequest::class.java)
    private val createResAdapter: JsonAdapter<CreateSessionResponse> =
        moshi.adapter(CreateSessionResponse::class.java)
    private val submitReqAdapter: JsonAdapter<SubmitImagesRequest> =
        moshi.adapter(SubmitImagesRequest::class.java)
    private val submitResAdapter: JsonAdapter<SubmitImagesResponse> =
        moshi.adapter(SubmitImagesResponse::class.java)
    private val statusAdapter: JsonAdapter<SessionStatusResponse> =
        moshi.adapter(SessionStatusResponse::class.java)
    private val errorAdapter: JsonAdapter<ApiErrorBody> =
        moshi.adapter(ApiErrorBody::class.java)

    // --------------------------- Public API ---------------------------

    suspend fun createSession(
        deviceInfo: DeviceInfoPayload?,
        referenceId: String? = null,
    ): IvsResult<CreateSessionResponse> {
        val body = createReqAdapter.toJson(
            CreateSessionRequest(
                referenceId = referenceId,
                sdkVersion = BuildConfig.SMARTCAPTURE_VERSION,
                deviceInfo = deviceInfo,
            )
        )
        val request = authorized(Request.Builder().url(url(IvsConfig.CREATE_SESSION_PATH)))
            ?.post(body.toRequestBody(JSON))
            ?.build()
            ?: return IvsResult.Failure(IvsError.Unauthorized("API key missing"))

        return execute(request) { res ->
            val payload = res.body?.string().orEmpty()
            createResAdapter.fromJson(payload)
                ?.let { IvsResult.Success(it) }
                ?: IvsResult.Failure(IvsError.Unexpected("Malformed session response"))
        }
    }

    /**
     * Submit front + back JPEG bytes. Bytes are base64-encoded inline with the
     * `data:image/jpeg;base64,` prefix — no recompression.
     */
    suspend fun submitImages(
        sessionId: String,
        frontJpeg: ByteArray,
        backJpeg: ByteArray
    ): IvsResult<SubmitImagesResponse> {
        Log.d(TAG, "submitImages front=${frontJpeg.size}B head=${hexHead(frontJpeg)} " +
                "back=${backJpeg.size}B head=${hexHead(backJpeg)}")
        val body = submitReqAdapter.toJson(
            SubmitImagesRequest(
                frontImage = dataUriFor(frontJpeg),
                backImage = dataUriFor(backJpeg),
                sdkVersion = BuildConfig.SMARTCAPTURE_VERSION
            )
        )
        val request = authorized(Request.Builder().url(url(IvsConfig.submitImagesPath(sessionId))))
            ?.post(body.toRequestBody(JSON))
            ?.build()
            ?: return IvsResult.Failure(IvsError.Unauthorized("API key missing"))

        return execute(request) { res ->
            val payload = res.body?.string().orEmpty()
            submitResAdapter.fromJson(payload)
                ?.let { IvsResult.Success(it) }
                ?: IvsResult.Failure(IvsError.Unexpected("Malformed submit response"))
        }
    }

    suspend fun getSession(sessionId: String): IvsResult<SessionStatusResponse> {
        val request = authorized(Request.Builder().url(url(IvsConfig.getSessionPath(sessionId))))
            ?.get()
            ?.build()
            ?: return IvsResult.Failure(IvsError.Unauthorized("API key missing"))

        return execute(request) { res ->
            val payload = res.body?.string().orEmpty()
            Log.d(TAG, "getSession body: $payload")
            statusAdapter.fromJson(payload)
                ?.let { IvsResult.Success(it) }
                ?: IvsResult.Failure(IvsError.Unexpected("Malformed status response"))
        }
    }

    // --------------------------- Internals ---------------------------

    private fun authorized(builder: Request.Builder): Request.Builder? {
        val key = apiKeyProvider() ?: return null
        return builder
            .header("Authorization", "Bearer $key")
            .header("Accept", "application/json")
    }

    private fun url(path: String): String = IvsConfig.BASE_URL + path

    private fun dataUriFor(bytes: ByteArray): String {
        // NO_WRAP: base64 without newlines. NO_CLOSE/NO_PADDING defaults are fine.
        val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
        return "data:image/jpeg;base64,$b64"
    }

    private fun hexHead(bytes: ByteArray, n: Int = 8): String =
        bytes.take(n).joinToString(" ") { "%02X".format(it) }

    private suspend fun <T> execute(
        request: Request,
        onSuccess: (Response) -> IvsResult<T>
    ): IvsResult<T> {
        val response = try {
            await(client.newCall(request))
        } catch (io: IOException) {
            return IvsResult.Failure(IvsError.Network(io.message ?: "Network error"))
        } catch (t: Throwable) {
            return IvsResult.Failure(IvsError.Unexpected(t.message ?: t.javaClass.simpleName))
        }

        return response.use {
            if (it.isSuccessful) {
                onSuccess(it)
            } else {
                IvsResult.Failure(mapErrorResponse(it))
            }
        }
    }

    private fun mapErrorResponse(response: Response): IvsError {
        val code = response.code
        val rawBody = runCatching { response.peekBody(MAX_ERROR_BODY).string() }.getOrNull()
        val parsedMessage = rawBody
            ?.takeIf { it.isNotBlank() }
            ?.let {
                runCatching { errorAdapter.fromJson(it) }
                    .getOrNull()
                    ?.let { err -> err.message ?: err.error }
            }
            ?: response.message.ifBlank { "HTTP $code" }

        return when (code) {
            400 -> {
                val lowered = parsedMessage.lowercase()
                when {
                    "already processed" in lowered ||
                        "already submitted" in lowered -> IvsError.AlreadySubmitted(parsedMessage)
                    "expired" in lowered ||
                        "expire" in lowered -> IvsError.SessionExpired(parsedMessage)
                    else -> IvsError.BadRequest(parsedMessage)
                }
            }
            401 -> IvsError.Unauthorized(parsedMessage)
            403 -> IvsError.Forbidden(parsedMessage)
            404 -> IvsError.SessionNotFound(parsedMessage)
            409 -> IvsError.AlreadySubmitted(parsedMessage)
            429 -> IvsError.RateLimited(parsedMessage)
            in 500..599 -> IvsError.ServerError(code, parsedMessage)
            else -> IvsError.Unexpected("HTTP $code: $parsedMessage")
        }
    }

    private suspend fun await(call: Call): Response =
        suspendCancellableCoroutine { cont ->
            cont.invokeOnCancellation { runCatching { call.cancel() } }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (cont.isActive) cont.resumeWithException(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    if (cont.isActive) {
                        cont.resume(response)
                    } else {
                        runCatching { response.close() }
                    }
                }
            })
        }

    companion object {
        private const val TAG = "IvsApi"
        private const val MAX_ERROR_BODY: Long = 4 * 1024 // 4 KiB cap on error bodies
        private val JSON = "application/json; charset=utf-8".toMediaType()

        fun defaultMoshi(): Moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        fun defaultClient(): OkHttpClient {
            val logging = HttpLoggingInterceptor { line -> Log.d(TAG, line) }
                .apply { level = HttpLoggingInterceptor.Level.BASIC }
            return OkHttpClient.Builder()
                .addInterceptor(logging)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build()
        }
    }
}
