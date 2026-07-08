package com.bastion.app.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.bastion.app.logging.RemoteLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val versionName: String,
    val versionCode: Int,
    val downloadUrl: String,
    val fileName: String,
    val timestamp: String,
    val fileSize: Long,
    val changelog: String
)

object UpdateChecker {
    private const val TAG = "UpdateChecker"

    /**
     * Pure parser: turns the /update JSON response into an [UpdateInfo] when the server
     * offers a newer versionCode than [localCode]. Returns null otherwise. Extracted from
     * IO so it can be unit-tested without a network connection (see UpdateCheckerTest).
     */
    fun parseUpdateResponse(response: String, localCode: Int): UpdateInfo? {
        val json = JSONObject(response)
        if (!json.optBoolean("update", false)) return null
        val serverCode = json.getInt("versionCode")
        if (serverCode <= localCode) return null
        return UpdateInfo(
            versionName = json.getString("versionName"),
            versionCode = serverCode,
            downloadUrl = json.getString("downloadUrl"),
            fileName = json.getString("fileName"),
            timestamp = json.optString("timestamp", ""),
            fileSize = json.optLong("fileSize", 0),
            changelog = json.optString("changelog", "")
        )
    }

    /**
     * Pure integrity check: a download is valid when we received a non-empty file whose size
     * matches the expected size. When [expectedSize] is unknown (<= 0) we only require > 0 bytes.
     */
    fun isValidDownload(actualLength: Long, expectedSize: Long): Boolean {
        if (actualLength <= 0L) return false
        if (expectedSize <= 0L) return true
        return actualLength == expectedSize
    }

    /**
     * Pure decision for F1: is the app allowed to launch the package installer?
     * Below Android 8 no runtime permission is needed; from Android 8 on, the
     * "Install unknown apps" permission must be granted. Extracted for unit testing.
     */
    fun isInstallPermissionGranted(sdkInt: Int, canRequestPackageInstalls: Boolean): Boolean =
        sdkInt < Build.VERSION_CODES.O || canRequestPackageInstalls

    suspend fun checkForUpdate(serverUrl: String): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val url = URL("$serverUrl/update")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.requestMethod = "GET"

            val response = conn.inputStream.bufferedReader().readText()
            conn.disconnect()

            val myCode = com.bastion.app.BuildConfig.VERSION_CODE
            val info = parseUpdateResponse(response, myCode)
            RemoteLogger.i(TAG, "checkForUpdate: server=${info?.versionCode ?: "none"} local=$myCode")
            info
        } catch (e: Exception) {
            RemoteLogger.e(TAG, "check failed: ${e.message}", e)
            null
        }
    }

    suspend fun downloadApk(
        context: Context,
        updateInfo: UpdateInfo,
        onProgress: (Int) -> Unit = {}
    ): File? = withContext(Dispatchers.IO) {
        val log = RemoteLogger.logger(TAG)
        try {
            val updatesDir = File(context.cacheDir, "updates")
            updatesDir.mkdirs()
            // F4: remove stale APKs from previous updates so only the fresh one remains,
            // otherwise a leftover old APK can be picked up and installed by mistake.
            updatesDir.listFiles()?.forEach { it.delete() }
            val outFile = File(updatesDir, updateInfo.fileName)

            log.i("downloading ${updateInfo.downloadUrl}")
            val url = URL(updateInfo.downloadUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 10_000
            conn.readTimeout = 30_000
            conn.connect()

            val total = conn.contentLength
            val input = conn.inputStream
            val output = FileOutputStream(outFile)

            val buf = ByteArray(8192)
            var downloaded = 0
            var read: Int
            while (input.read(buf).also { read = it } != -1) {
                output.write(buf, 0, read)
                downloaded += read
                if (total > 0) {
                    val pct = (downloaded * 100) / total
                    onProgress(pct)
                }
            }
            output.flush()
            output.close()
            input.close()
            conn.disconnect()

            // F3: verify integrity. A server restart mid-download (or any interruption) leaves a
            // truncated APK that fails to install with a confusing "parse error". Detect it here.
            if (!isValidDownload(outFile.length(), updateInfo.fileSize)) {
                log.e("download size mismatch: got ${outFile.length()} expected ${updateInfo.fileSize} — deleting")
                outFile.delete()
                return@withContext null
            }

            log.i("download complete: ${outFile.length()} bytes")
            outFile
        } catch (e: Exception) {
            log.e("download failed: ${e.message}", e)
            null
        }
    }

    /**
     * Launches the system package installer for [apkFile].
     * @return true if the installer was launched; false if it was blocked (missing permission,
     *         missing/empty file, or an exception) — in the permission case the user is sent to
     *         the "Install unknown apps" settings screen so they can grant it and retry.
     */
    fun installApk(context: Context, apkFile: File): Boolean {
        val log = RemoteLogger.logger(TAG)

        if (!apkFile.exists() || apkFile.length() == 0L) {
            log.e("install aborted: apk missing or empty (${apkFile.name})")
            return false
        }

        // F1: on Android 8+ installing an APK requires the "Install unknown apps" permission for
        // this app. If it is revoked, the installer intent is silently blocked and returns to the
        // app with no exception — which is exactly why "Update now" appeared to do nothing.
        val canRequest = Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
            context.packageManager.canRequestPackageInstalls()
        if (!isInstallPermissionGranted(Build.VERSION.SDK_INT, canRequest)) {
            log.w("install blocked: canRequestPackageInstalls=false, opening settings")
            try {
                val settings = Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}")
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(settings)
            } catch (e: Exception) {
                log.e("open unknown-sources settings failed: ${e.message}", e)
            }
            return false
        }

        return try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            log.i("launching installer for ${apkFile.name} (${apkFile.length()} bytes)")
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            log.e("install failed: ${e.message}", e)
            false
        }
    }
}
