package com.gbg.smartcapture.bigmagic.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.provider.Settings
import java.util.TimeZone

data class DeviceInfo(
    val deviceId: String,
    val manufacturer: String,
    val model: String,
    val osVersion: String,
    val timezone: String,
    val network: String,
)

object DeviceInfoCollector {

    fun snapshot(context: Context): DeviceInfo {
        val appContext = context.applicationContext
        return DeviceInfo(
            deviceId = Settings.Secure
                .getString(appContext.contentResolver, Settings.Secure.ANDROID_ID)
                .orEmpty(),
            manufacturer = Build.MANUFACTURER.orEmpty(),
            model = Build.MODEL.orEmpty(),
            osVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            timezone = TimeZone.getDefault().id,
            network = currentNetworkLabel(appContext),
        )
    }

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
}
