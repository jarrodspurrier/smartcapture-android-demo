package com.gbg.smartcapture.bigmagic.util

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

private const val TAG = "GallerySaver"
private const val ALBUM_NAME = "SmartCaptureDemo"

/**
 * Debug-only helper: writes the SDK-returned JPEG bytes into the device gallery under
 * `Pictures/SmartCaptureDemo/`. Safe to call from release code — it's a no-op if the toggle
 * is off at the call site — but intended for debug builds only.
 *
 * Returns the inserted content URI on success, or null on any failure. Never throws.
 */
suspend fun saveJpegToGallery(
    context: Context,
    bytes: ByteArray,
    displayName: String,
): Uri? = withContext(Dispatchers.IO) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveViaMediaStore(context, bytes, displayName)
        } else {
            saveViaLegacyFile(context, bytes, displayName)
        }
    } catch (t: Throwable) {
        Log.w(TAG, "Failed to save $displayName to gallery", t)
        null
    }
}

private fun saveViaMediaStore(context: Context, bytes: ByteArray, displayName: String): Uri? {
    val resolver = context.contentResolver
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/$ALBUM_NAME")
        put(MediaStore.Images.Media.IS_PENDING, 1)
    }
    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return null
    resolver.openOutputStream(uri)?.use { it.write(bytes) } ?: run {
        resolver.delete(uri, null, null)
        return null
    }
    val clearPending = ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }
    resolver.update(uri, clearPending, null, null)
    Log.i(TAG, "Saved $displayName to $uri")
    return uri
}

private fun saveViaLegacyFile(context: Context, bytes: ByteArray, displayName: String): Uri? {
    val granted = ContextCompat.checkSelfPermission(
        context, Manifest.permission.WRITE_EXTERNAL_STORAGE
    ) == PackageManager.PERMISSION_GRANTED
    if (!granted) {
        Log.w(TAG, "WRITE_EXTERNAL_STORAGE not granted — grant it in app settings to use the gallery toggle on API ${Build.VERSION.SDK_INT}")
        return null
    }
    @Suppress("DEPRECATION")
    val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
    val albumDir = File(picturesDir, ALBUM_NAME)
    if (!albumDir.exists() && !albumDir.mkdirs()) {
        Log.w(TAG, "Could not create $albumDir")
        return null
    }
    val file = File(albumDir, displayName)
    FileOutputStream(file).use { it.write(bytes) }
    var scannedUri: Uri? = null
    MediaScannerConnection.scanFile(
        context, arrayOf(file.absolutePath), arrayOf("image/jpeg")
    ) { _, uri -> scannedUri = uri }
    Log.i(TAG, "Saved ${file.absolutePath}")
    return scannedUri ?: Uri.fromFile(file)
}
