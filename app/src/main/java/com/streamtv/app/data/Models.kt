package com.streamtv.app.data

/** A live channel parsed from an M3U playlist. */
data class Channel(
    val name: String,
    val url: String,
    val logo: String?,
    val group: String,
    val userAgent: String? = null,
    val referrer: String? = null,
)

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

/** Something the player can play; [isLive] enables channel zapping and hides the seek bar. */
data class PlayableItem(
    val title: String,
    val url: String,
    val isLive: Boolean,
    val userAgent: String? = null,
    val referrer: String? = null,
)
