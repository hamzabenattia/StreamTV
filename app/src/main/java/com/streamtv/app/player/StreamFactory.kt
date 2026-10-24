package com.streamtv.app.player

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import com.streamtv.app.data.PlayableItem
import okhttp3.OkHttpClient

/** Builds ExoPlayer sources for streams, shared by the full-screen player and the Live TV preview. */
@OptIn(UnstableApi::class)
object StreamFactory {
    private val http = OkHttpClient.Builder().followRedirects(true).build()

    private const val DEFAULT_USER_AGENT =
        "Mozilla/5.0 (Linux; Android 12) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36"

    /** [asHls] forces HLS for IPTV URLs that don't end in ".m3u8". */
    fun mediaSource(item: PlayableItem, asHls: Boolean): MediaSource {
        val dataSource = OkHttpDataSource.Factory(http)
            .setUserAgent(item.userAgent ?: DEFAULT_USER_AGENT)
            .apply { item.referrer?.let { setDefaultRequestProperties(mapOf("Referer" to it)) } }

        val subtitles = item.subtitles.map { sub ->
            MediaItem.SubtitleConfiguration.Builder(Uri.parse(sub.url))
                .setMimeType(if (sub.url.contains(".vtt", ignoreCase = true)) MimeTypes.TEXT_VTT else MimeTypes.APPLICATION_SUBRIP)
                .setLanguage(sub.language)
                .setLabel(sub.label)
                .build()
        }
        val mediaItem = MediaItem.Builder()
            .setUri(item.url)
            .setMediaMetadata(MediaMetadata.Builder().setTitle(item.title).build())
            .setSubtitleConfigurations(subtitles)
            .apply { if (asHls) setMimeType(MimeTypes.APPLICATION_M3U8) }
            .build()
        return DefaultMediaSourceFactory(dataSource).createMediaSource(mediaItem)
    }

    /** Prefers subtitle and audio tracks in [language] ("ar", "fr", ...); "" leaves subtitles off by default. */
    fun withLanguage(params: TrackSelectionParameters, language: String): TrackSelectionParameters =
        params.buildUpon().apply {
            if (language.isNotEmpty()) {
                setPreferredTextLanguage(language)
                setPreferredAudioLanguages(language)
            } else {
                setPreferredTextLanguage(null)
            }
        }.build()
}
