package com.streamtv.app.data

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.streamtv.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.IOException

data class UpdateInfo(val versionCode: Int, val versionName: String, val apkUrl: String, val notes: String)

/** Checks the GitHub repo's latest release and installs it over the running app. */
class Updater(private val context: Context, private val http: OkHttpClient) {

    /** Returns the newer release, or null when the app is already up to date. */
    suspend fun check(): UpdateInfo? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://api.github.com/repos/$GITHUB_REPO/releases/latest")
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "StreamTV")
            .build()
        val json = http.newCall(request).execute().use { resp ->
            if (resp.code == 404) throw IOException("No published releases found. Is the GitHub repo public?")
            if (!resp.isSuccessful) throw IOException("GitHub answered HTTP ${resp.code}")
            JSONObject(resp.body?.string().orEmpty())
        }
        val code = json.optString("tag_name").removePrefix("v").toIntOrNull()
            ?: throw IOException("Unexpected release tag \"${json.optString("tag_name")}\"")
        val assets = json.optJSONArray("assets")
        val apkUrl = (0 until (assets?.length() ?: 0))
            .map { assets!!.getJSONObject(it) }
            .firstOrNull { it.optString("name").endsWith(".apk") }
            ?.optString("browser_download_url")
            ?: throw IOException("The latest release has no APK attached")
        if (code <= BuildConfig.VERSION_CODE) null
        else UpdateInfo(code, json.optString("name").ifBlank { "v$code" }, apkUrl, json.optString("body"))
    }

    suspend fun download(info: UpdateInfo, onProgress: (Float) -> Unit): File = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val file = File(dir, "StreamTV-${info.versionCode}.apk")
        val request = Request.Builder().url(info.apkUrl).header("User-Agent", "StreamTV").build()
        http.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("Download failed: HTTP ${resp.code}")
            val body = resp.body ?: throw IOException("Empty download")
            val total = body.contentLength()
            body.byteStream().use { input ->
                file.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var done = 0L
                    while (true) {
                        val n = input.read(buffer)
                        if (n < 0) break
                        output.write(buffer, 0, n)
                        done += n
                        if (total > 0) onProgress(done.toFloat() / total)
                    }
                }
            }
        }
        file
    }

    /**
     * Opens the system installer. Returns false if the user first has to allow
     * "Install unknown apps" for StreamTV (we open that settings page for them).
     */
    fun install(file: File): Boolean {
        if (Build.VERSION.SDK_INT >= 26 && !context.packageManager.canRequestPackageInstalls()) {
            val settings = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                context.startActivity(settings)
            } catch (_: ActivityNotFoundException) {
                // Some TVs (e.g. Fire TV) don't have this screen; the toast in the UI explains where to look.
            }
            return false
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.updates", file)
        context.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        return true
    }

    companion object {
        const val GITHUB_REPO = "hamzabenattia/StreamTV"
    }
}
