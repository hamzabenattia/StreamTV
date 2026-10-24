package com.streamtv.app.data

import android.text.Html
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.Locale
import java.util.concurrent.TimeUnit

data class ArchiveStream(val url: String, val subtitles: List<Subtitle>)

fun languageLabel(code: String): String =
    if (code == "und") "Subtitles" else Locale(code).getDisplayLanguage(Locale.ENGLISH).ifBlank { code }

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

    /** Finds the best playable video file inside an Internet Archive item, plus any subtitle files. */
    suspend fun resolveArchiveStream(id: String): ArchiveStream? = withContext(Dispatchers.IO) {
        val files = JSONObject(get("https://archive.org/metadata/$id")).optJSONArray("files")
            ?: return@withContext null
        val all = (0 until files.length()).map { files.getJSONObject(it) }
        val playable = listOf(".mp4", ".m4v", ".mkv", ".webm")
        val best = all
            .filter { f -> playable.any { f.optString("name").endsWith(it, ignoreCase = true) } }
            // Prefer H.264 MP4s (play everywhere), then the biggest (best quality) file.
            .sortedWith(
                compareByDescending<JSONObject> { it.optString("name").endsWith(".mp4", true) }
                    .thenByDescending { it.optString("size").toLongOrNull() ?: 0L }
            )
            .firstOrNull() ?: return@withContext null
        val subtitles = all
            .map { it.optString("name") }
            .filter { it.endsWith(".srt", true) || it.endsWith(".vtt", true) }
            .map { name ->
                val lang = guessLanguage(name)
                Subtitle(archiveFileUrl(id, name), lang, languageLabel(lang))
            }
        ArchiveStream(archiveFileUrl(id, best.getString("name")), subtitles)
    }

    /**
     * Looks up a subtitle on OpenSubtitles (needs a free API key from opensubtitles.com).
     * Returns null when nothing matches or the daily download quota is used up.
     */
    suspend fun findOpenSubtitle(apiKey: String, title: String, year: String?, language: String): Subtitle? =
        withContext(Dispatchers.IO) {
            // The API wants lowercase, alphabetically ordered parameters.
            val searchUrl = "https://api.opensubtitles.com/api/v1/subtitles".toHttpUrl().newBuilder()
                .addQueryParameter("languages", language)
                .addQueryParameter("query", title.lowercase())
                .apply { year?.let { addQueryParameter("year", it) } }
                .build()
            val search = Request.Builder().url(searchUrl).openSubtitlesHeaders(apiKey).build()
            val fileId = http.newCall(search).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext null
                val data = JSONObject(resp.body?.string().orEmpty()).optJSONArray("data") ?: return@withContext null
                if (data.length() == 0) return@withContext null
                data.getJSONObject(0).getJSONObject("attributes").getJSONArray("files").getJSONObject(0).getLong("file_id")
            }
            val download = Request.Builder()
                .url("https://api.opensubtitles.com/api/v1/download")
                .openSubtitlesHeaders(apiKey)
                .post("""{"file_id":$fileId}""".toRequestBody("application/json".toMediaType()))
                .build()
            val link = http.newCall(download).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext null
                JSONObject(resp.body?.string().orEmpty()).optString("link").ifBlank { null }
            } ?: return@withContext null
            Subtitle(link, language, "${languageLabel(language)} (OpenSubtitles)")
        }

    private fun Request.Builder.openSubtitlesHeaders(apiKey: String) = this
        .header("Api-Key", apiKey)
        .header("User-Agent", "StreamTV v1.0")
        .header("Accept", "application/json")

    private fun archiveFileUrl(id: String, name: String) =
        "https://archive.org/download".toHttpUrl().newBuilder()
            .addPathSegment(id)
            .addPathSegments(name)
            .build()
            .toString()

    private fun guessLanguage(fileName: String): String {
        val n = fileName.lowercase()
        fun has(vararg tags: String) = tags.any { Regex("(^|[^a-z])$it([^a-z]|$)").containsMatchIn(n) }
        return when {
            has("ar", "ara", "arabic") -> "ar"
            has("fr", "fre", "fra", "french") -> "fr"
            has("en", "eng", "english") -> "en"
            has("es", "spa", "spanish") -> "es"
            else -> "und"
        }
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
