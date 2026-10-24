package com.streamtv.app.data

/**
 * Parses extended M3U playlists, e.g.
 *
 *   #EXTINF:-1 tvg-id="x" tvg-logo="http://..." group-title="News",Channel Name
 *   #EXTVLCOPT:http-user-agent=Mozilla/5.0
 *   http://example.com/stream.m3u8
 */
object M3uParser {
    private val attrRegex = Regex("""([\w-]+)="([^"]*)"""")

    fun parse(text: String): List<Channel> {
        val channels = mutableListOf<Channel>()
        var name: String? = null
        var attrs = emptyMap<String, String>()
        var extGroup: String? = null
        var userAgent: String? = null
        var referrer: String? = null

        for (raw in text.lineSequence()) {
            val line = raw.trim()
            when {
                line.isEmpty() -> Unit
                line.startsWith("#EXTINF", ignoreCase = true) -> {
                    val matches = attrRegex.findAll(line).toList()
                    attrs = matches.associate { it.groupValues[1].lowercase() to it.groupValues[2] }
                    // The title follows the first comma after the last attribute (attributes may contain commas).
                    val searchFrom = matches.lastOrNull()?.range?.last ?: line.indexOf(':')
                    val comma = line.indexOf(',', searchFrom.coerceAtLeast(0))
                    name = (if (comma >= 0) line.substring(comma + 1).trim() else "")
                        .ifEmpty { attrs["tvg-name"] ?: "Unknown channel" }
                }
                line.startsWith("#EXTGRP:", ignoreCase = true) ->
                    extGroup = line.substringAfter(':').trim()
                line.startsWith("#EXTVLCOPT:", ignoreCase = true) -> {
                    val opt = line.substringAfter(':')
                    when {
                        opt.startsWith("http-user-agent=") -> userAgent = opt.substringAfter('=')
                        opt.startsWith("http-referrer=") -> referrer = opt.substringAfter('=')
                    }
                }
                line.startsWith("#") -> Unit
                else -> {
                    val channelName = name
                    if (channelName != null) {
                        val group = (attrs["group-title"] ?: extGroup)
                            ?.substringBefore(';')?.trim()
                            ?.ifEmpty { null } ?: "Other"
                        channels += Channel(
                            name = channelName,
                            url = line,
                            logo = attrs["tvg-logo"]?.ifBlank { null },
                            group = group,
                            userAgent = userAgent ?: attrs["http-user-agent"] ?: attrs["user-agent"],
                            referrer = referrer ?: attrs["http-referrer"],
                        )
                    }
                    name = null; attrs = emptyMap(); extGroup = null; userAgent = null; referrer = null
                }
            }
        }
        return channels
    }
}
