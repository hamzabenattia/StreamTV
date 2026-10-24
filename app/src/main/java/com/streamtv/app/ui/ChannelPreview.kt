package com.streamtv.app.ui

import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.tv.material3.Text
import com.streamtv.app.data.Channel
import com.streamtv.app.player.StreamFactory
import kotlinx.coroutines.delay

/**
 * Small live window that starts playing whichever channel is highlighted, after a short pause
 * so scrolling through the grid doesn't start dozens of streams.
 */
@OptIn(UnstableApi::class)
@Composable
fun ChannelPreview(channel: Channel?, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val player = remember { ExoPlayer.Builder(context).build().apply { volume = 0.6f } }
    var failed by remember { mutableStateOf(false) }

    DisposableEffect(Unit) { onDispose { player.release() } }

    // Stop while the full-screen player (or another app) is on top; resume when we come back.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> player.stop()
                Lifecycle.Event.ON_START -> if (player.currentMediaItem != null) {
                    player.prepare(); player.play()
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    DisposableEffect(channel?.url) {
        var triedAsHls = false
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                val ch = channel ?: return
                if (!triedAsHls) {
                    triedAsHls = true
                    player.setMediaSource(StreamFactory.mediaSource(ch.toPlayable(), asHls = true))
                    player.prepare()
                } else {
                    failed = true
                }
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    LaunchedEffect(channel?.url) {
        player.stop()
        player.clearMediaItems()
        failed = false
        val ch = channel ?: return@LaunchedEffect
        delay(700)
        player.setMediaSource(StreamFactory.mediaSource(ch.toPlayable(), asHls = false))
        player.prepare()
        player.playWhenReady = true
    }

    Box(modifier.background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    this.player = player
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                    // Keep remote focus on the channel grid.
                    isFocusable = false
                    descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
                }
            },
            modifier = Modifier.fillMaxSize(),
        )
        if (failed) {
            Text("Preview not available", color = Palette.Muted, modifier = Modifier.align(Alignment.Center))
        }
    }
}
