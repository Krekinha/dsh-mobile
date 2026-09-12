package com.dsh.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

data class ReleaseInfo(
    val tagName: String,
    val downloadUrl: String,
    val assetSize: Long,
    val body: String
)

sealed class UpdateState {
    object Checking : UpdateState()
    data class UpdateAvailable(val release: ReleaseInfo) : UpdateState()
    object UpToDate : UpdateState()
    data class Error(val message: String) : UpdateState()
}

class UpdateManager(private val context: Context) {

    private val executor = Executors.newSingleThreadExecutor()
    private val repoUrl = "https://api.github.com/repos/Krekinha/dsh-mobile/releases/latest"

    fun checkForUpdates(currentVersion: String, onResult: (UpdateState) -> Unit) {
        onResult(UpdateState.Checking)
        executor.execute {
            try {
                val url = URL(repoUrl)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 10000
                    readTimeout = 10000
                    setRequestProperty("Accept", "application/vnd.github.v3+json")
                    setRequestProperty("User-Agent", "DSH-Mobile-App")
                }

                if (conn.responseCode != 200) {
                    onResult(UpdateState.Error("GitHub respondeu com código ${conn.responseCode}"))
                    return@execute
                }

                val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(jsonStr)

                val tagName = json.optString("tag_name", "")
                val body = json.optString("body", "")
                val assets = json.optJSONArray("assets")

                var downloadUrl: String? = null
                var assetSize: Long = 0

                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val name = asset.optString("name", "")
                        if (name.endsWith(".apk")) {
                            downloadUrl = asset.optString("browser_download_url")
                            assetSize = asset.optLong("size", 0)
                            break
                        }
                    }
                }

                if (downloadUrl == null) {
                    onResult(UpdateState.Error("Nenhum APK encontrado na release do GitHub."))
                    return@execute
                }

                val release = ReleaseInfo(tagName, downloadUrl, assetSize, body)
                if (VersionHelper.isNewer(tagName, currentVersion)) {
                    onResult(UpdateState.UpdateAvailable(release))
                } else {
                    onResult(UpdateState.UpToDate)
                }

            } catch (e: Exception) {
                onResult(UpdateState.Error("Erro ao verificar atualização: ${e.message}"))
            }
        }
    }

    fun downloadApk(
        release: ReleaseInfo,
        onProgress: (bytesRead: Long, totalBytes: Long, percent: Int) -> Unit,
        onComplete: (Result<File>) -> Unit
    ) {
        executor.execute {
            try {
                val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
                val apkFile = File(updatesDir, "dsh-update.apk")
                if (apkFile.exists()) apkFile.delete()

                var currentUrl = release.downloadUrl
                var conn: HttpURLConnection

                // Follow redirects (GitHub Releases redirect to AWS S3 CDN)
                while (true) {
                    val url = URL(currentUrl)
                    conn = (url.openConnection() as HttpURLConnection).apply {
                        connectTimeout = 15000
                        readTimeout = 15000
                        instanceFollowRedirects = false
                        setRequestProperty("User-Agent", "DSH-Mobile-App")
                    }

                    when (conn.responseCode) {
                        HttpURLConnection.HTTP_MOVED_PERM,
                        HttpURLConnection.HTTP_MOVED_TEMP,
                        307, 308 -> {
                            val newUrl = conn.getHeaderField("Location")
                            conn.disconnect()
                            currentUrl = newUrl
                        }
                        HttpURLConnection.HTTP_OK -> break
                        else -> {
                            onComplete(Result.failure(Exception("HTTP ${conn.responseCode} ao baixar APK")))
                            return@execute
                        }
                    }
                }

                val totalBytes = if (conn.contentLengthLong > 0) conn.contentLengthLong else release.assetSize
                var bytesReadTotal: Long = 0

                conn.inputStream.use { input ->
                    FileOutputStream(apkFile).use { output ->
                        val buffer = ByteArray(8192)
                        var read: Int
                        while (input.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                            bytesReadTotal += read
                            val percent = if (totalBytes > 0) {
                                ((bytesReadTotal * 100) / totalBytes).toInt().coerceIn(0, 100)
                            } else 0
                            onProgress(bytesReadTotal, totalBytes, percent)
                        }
                        output.flush()
                    }
                }

                onComplete(Result.success(apkFile))

            } catch (e: Exception) {
                onComplete(Result.failure(e))
            }
        }
    }

    fun installApk(apkFile: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return
            }
        }

        val authority = "${context.packageName}.fileprovider"
        val apkUri = FileProvider.getUriForFile(context, authority, apkFile)

        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(installIntent)
    }
}
