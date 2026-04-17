package com.gbg.smartcapture.bigmagic.ivs

import com.gbg.smartcapture.bigmagic.BuildConfig

object IvsConfig {
    val BASE_URL: String = BuildConfig.IVS_BASE_URL.removeSuffix("/")

    const val CREATE_SESSION_PATH = "/api/verification/sessions"
    fun submitImagesPath(sessionId: String) = "/api/verification/sessions/$sessionId/images"
    fun getSessionPath(sessionId: String) = "/api/verification/sessions/$sessionId"

    const val POLL_INTERVAL_MS: Long = 4_000L
    const val POLL_MAX_ATTEMPTS: Int = 10
}
