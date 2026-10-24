package com.streamtv.app.data

/** A live channel parsed from an M3U playlist. */
data class Channel(
    val name: String,
    val url: String,
    val logo: String?,
    val group: String,
    val userAgent: String? = null,
    val referrer: String? = null,
    /** ISO country code (lowercase), taken from iptv-org style ids like "ElWatania1.tn@SD". */
    val country: String? = null,
    val categories: List<String> = listOf(group),
) {
    fun toPlayable() = PlayableItem(name, url, isLive = true, userAgent = userAgent, referrer = referrer)
}

/** A movie or TV episode from the Internet Archive's public-domain collections. */
data class Movie(
    val id: String,
    val title: String,
    val year: String?,
    val description: String?,
) {
    val poster: String get() = "https://archive.org/services/img/$id"
}

/** One horizontal row on the home screen. */
data class Shelf(val title: String, val movies: List<Movie>)

/** A side-loaded subtitle file (.srt or .vtt). */
data class Subtitle(val url: String, val language: String, val label: String)

/** Something the player can play; [isLive] enables channel zapping and hides the seek bar. */
data class PlayableItem(
    val title: String,
    val url: String,
    val isLive: Boolean,
    val userAgent: String? = null,
    val referrer: String? = null,
    val subtitles: List<Subtitle> = emptyList(),
)
