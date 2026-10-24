package com.streamtv.app.data

import android.content.Context

class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("streamtv", Context.MODE_PRIVATE)

    var playlistUrl: String
        get() = sp.getString(KEY_PLAYLIST, null)
            // Upgrade the old news-only default to the full list, which supports country filters.
            ?.takeIf { it != OLD_DEFAULT_PLAYLIST } ?: DEFAULT_PLAYLIST
        set(value) = sp.edit().putString(KEY_PLAYLIST, value).apply()

    /** ISO 639-1 code of the preferred subtitle language, or "" for subtitles off. */
    var subtitleLanguage: String
        get() = sp.getString(KEY_SUB_LANG, "ar") ?: "ar"
        set(value) = sp.edit().putString(KEY_SUB_LANG, value).apply()

    /** Free key from opensubtitles.com, used to fetch subtitles for movies. */
    var openSubtitlesKey: String
        get() = sp.getString(KEY_OPENSUBS, "") ?: ""
        set(value) = sp.edit().putString(KEY_OPENSUBS, value.trim()).apply()

    var favorites: Set<String>
        get() = sp.getStringSet(KEY_FAVORITES, emptySet())?.toSet() ?: emptySet()
        set(value) = sp.edit().putStringSet(KEY_FAVORITES, value).apply()

    /** "Continue watching", most recent first. */
    var recentMovies: List<Movie>
        get() = sp.getString(KEY_RECENT, "")!!.lineSequence().mapNotNull { line ->
            val parts = line.split('\t')
            if (parts.size < 2) null
            else Movie(id = parts[0], title = parts[1], year = parts.getOrNull(2)?.ifBlank { null }, description = null)
        }.toList()
        set(value) = sp.edit().putString(
            KEY_RECENT,
            value.take(20).joinToString("\n") { m ->
                listOf(m.id, m.title, m.year.orEmpty()).joinToString("\t") { it.replace(Regex("[\t\n]"), " ") }
            },
        ).apply()

    companion object {
        private const val KEY_PLAYLIST = "playlist_url"
        private const val KEY_FAVORITES = "favorites"
        private const val KEY_RECENT = "recent_movies"
        private const val KEY_SUB_LANG = "subtitle_language"
        private const val KEY_OPENSUBS = "opensubtitles_key"
        private const val OLD_DEFAULT_PLAYLIST = "https://iptv-org.github.io/iptv/categories/news.m3u"
        const val DEFAULT_PLAYLIST = "https://iptv-org.github.io/iptv/index.m3u"
    }
}
