package com.bastion.app.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.util.Log
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

    suspend fun checkForUpdate(serverUrl: String): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val url = URL("$serverUrl/update")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.requestMethod = "GET"

            val response = conn.inputStream.bufferedReader().readText()
            conn.disconnect()

            val json = JSONObject(response)
            if (!json.optBoolean("update", false)) return@withContext null

            val serverCode = json.getInt("versionCode")
            val context = com.bastion.app.BuildConfig::class.java
            val myCode = com.bastion.app.BuildConfig.VERSION_CODE

            RemoteLogger.i(TAG, "checkForUpdate: server=$serverCode local=$myCode")

            if (serverCode <= myCode) return@withContext null

            UpdateInfo(
                versionName = json.getString("versionName"),
                versionCode = serverCode,
                downloadUrl = json.getString("downloadUrl"),
                fileName = json.getString("fileName"),
                timestamp = json.getString("timestamp"),
                fileSize = json.optLong("fileSize", 0),
                changelog = json.optString("changelog", "")
            )
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

            log.i("download complete: ${outFile.length()} bytes")
            outFile
        } catch (e: Exception) {
            log.e("download failed: ${e.message}", e)
            null
        }
    }

    fun installApk(context: Context, apkFile: File) {
        val log = RemoteLogger.logger(TAG)
        try {
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
            log.i("launching installer for ${apkFile.name}")
            context.startActivity(intent)
        } catch (e: Exception) {
            log.e("install failed: ${e.message}", e)
        }
    }
}