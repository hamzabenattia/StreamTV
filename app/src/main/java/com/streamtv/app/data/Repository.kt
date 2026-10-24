package com.streamtv.app.data

import android.text.Html
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class Repository {
    val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    suspend fun loadPlaylist(url: String): List<Channel> = withContext(Dispatchers.IO) {
        M3uParser.parse(get(url))
    }

    /** Searches the Internet Archive for playable films/TV, most popular first. */
    suspend fun searchArchive(query: String, rows: Int = 40): List<Movie> = withContext(Dispatchers.IO) {
        val url = "https://archive.org/advancedsearch.php".toHttpUrl().newBuilder()
            .addQueryParameter("q", "($query) AND mediatype:movies")
            .addQueryParameter("fl[]", "identifier")
            .addQueryParameter("fl[]", "title")
            .addQueryParameter("fl[]", "year")
            .addQueryParameter("fl[]", "description")
            .addQueryParameter("sort[]", "downloads desc")
            .addQueryParameter("rows", rows.toString())
            .addQueryParameter("page", "1")
            .addQueryParameter("output", "json")
            .build()
        val docs = JSONObject(get(url.toString())).getJSONObject("response").getJSONArray("docs")
        (0 until docs.length()).map { i ->
            val d = docs.getJSONObject(i)
            Movie(
                id = d.getString("identifier"),
                title = d.optString("title").ifBlank { d.getString("identifier") },
                year = d.opt("year")?.toString()?.take(4),
                description = d.textOrNull("description")?.let(::stripHtml),
            )
        }
    }

    suspend fun collection(name: String, rows: Int = 40) = searchArchive("collection:$name", rows)

    /** Finds the best playable video file inside an Internet Archive item. */
    suspend fun resolveArchiveStream(id: String): String? = withContext(Dispatchers.IO) {
        val files = JSONObject(get("https://archive.org/metadata/$id")).optJSONArray("files")
            ?: return@withContext null
        val playable = listOf(".mp4", ".m4v", ".mkv", ".webm")
        val best = (0 until files.length())
            .map { files.getJSONObject(it) }
            .filter { f -> playable.any { f.optString("name").endsWith(it, ignoreCase = true) } }
            // Prefer H.264 MP4s (play everywhere), then the biggest (best quality) file.
            .sortedWith(
                compareByDescending<JSONObject> { it.optString("name").endsWith(".mp4", true) }
                    .thenByDescending { it.optString("size").toLongOrNull() ?: 0L }
            )
            .firstOrNull() ?: return@withContext null
        "https://archive.org/download".toHttpUrl().newBuilder()
            .addPathSegment(id)
            .addPathSegments(best.getString("name"))
            .build()
            .toString()
    }

    private fun get(url: String): String {
        val request = Request.Builder().url(url).header("User-Agent", "StreamTV/1.0").build()
        http.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("HTTP ${resp.code} for $url")
            return resp.body?.string() ?: throw IOException("Empty response from $url")
        }
    }

    private fun JSONObject.textOrNull(key: String): String? = when (val v = opt(key)) {
        is String -> v
        is JSONArray -> if (v.length() > 0) v.optString(0) else null
        else -> null
    }

    private fun stripHtml(s: String) =
        Html.fromHtml(s, Html.FROM_HTML_MODE_COMPACT).toString().replace(Regex("\\s+"), " ").trim()
}
