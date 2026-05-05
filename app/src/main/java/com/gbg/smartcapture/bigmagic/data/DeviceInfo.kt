package com.gbg.smartcapture.bigmagic.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.provider.Settings
import com.gbg.smartcapture.bigmagic.BuildConfig
import com.squareup.moshi.JsonClass
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

data class DeviceInfo(
    val deviceId: String,
    val manufacturer: String,
    val model: String,
    val osVersion: String,
    val timezone: String,
    val network: String,
)

/**
 * Wire shape for the `deviceInfo` field on IVS createSession.
 * [deviceIdHash] is HMAC-SHA256 of ANDROID_ID keyed with the IVS API key,
 * prefixed `hmac_sha256:` so the server knows the hashing scheme.
 */
@JsonClass(generateAdapter = true)
data class DeviceInfoPayload(
    val deviceIdHash: String,
    val manufacturer: String,
    val model: String,
    val osVersion: String,
    val network: String,
    val other: Map<String, String>,
    val capturedAt: String,
    val source: String,
)

object DeviceInfoCollector {

    fun snapshot(context: Context): DeviceInfo {
        val appContext = context.applicationContext
        return DeviceInfo(
            deviceId = androidId(appContext),
            manufacturer = Build.MANUFACTURER.orEmpty(),
            model = Build.MODEL.orEmpty(),
            osVersion = osVersionLabel(),
            timezone = TimeZone.getDefault().id,
            network = currentNetworkLabel(appContext),
        )
    }

    fun payload(context: Context, hmacKey: String?): DeviceInfoPayload {
        val appContext = context.applicationContext
        return DeviceInfoPayload(
            deviceIdHash = "hmac_sha256:" + hmacSha256Hex(androidId(appContext), hmacKey.orEmpty()),
            manufacturer = Build.MANUFACTURER.orEmpty(),
            model = Build.MODEL.orEmpty(),
            osVersion = osVersionLabel(),
            network = currentNetworkLabel(appContext),
            other = mapOf("appVersion" to BuildConfig.VERSION_NAME),
            capturedAt = nowIso8601Utc(),
            source = "api",
        )
    }

    private fun androidId(context: Context): String =
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID).orEmpty()

    private fun osVersionLabel(): String =
        "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"

    private fun currentNetworkLabel(context: Context): String {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return "None"
        val active = cm.activeNetwork ?: return "None"
        val caps = cm.getNetworkCapabilities(active) ?: return "None"
        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WiFi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            else -> "None"
        }
    }

    private fun hmacSha256Hex(data: String, secret: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        return mac.doFinal(data.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }

    private fun nowIso8601Utc(): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        return fmt.format(Date())
    }
}
