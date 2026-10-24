package com.streamtv.app.player

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.streamtv.app.data.Prefs

@OptIn(UnstableApi::class)
class PlayerActivity : ComponentActivity() {
    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView
    private lateinit var infoText: TextView
    private val handler = Handler(Looper.getMainLooper())
    private val hideInfo = Runnable { infoText.visibility = View.GONE }

    /** Many IPTV URLs have no ".m3u8" extension; on failure we retry once telling ExoPlayer it's HLS. */
    private var triedAsHls = false
    private var resumePositionMs = C.TIME_UNSET

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (PlaybackQueue.current == null) {
            finish(); return
        }

        playerView = PlayerView(this).apply {
            setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
            setBackgroundColor(Color.BLACK)
            controllerShowTimeoutMs = 4000
            setShowSubtitleButton(true)
        }
        infoText = TextView(this).apply {
            textSize = 26f
            setTextColor(Color.WHITE)
            setBackgroundColor(0xCC000000.toInt())
            setPadding(40, 20, 40, 20)
            visibility = View.GONE
        }
        val root = FrameLayout(this)
        root.addView(playerView, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        root.addView(
            infoText,
            FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                gravity = Gravity.TOP or Gravity.START
                setMargins(64, 48, 64, 0)
            },
        )
        setContentView(root)
    }

    override fun onStart() {
        super.onStart()
        if (PlaybackQueue.current == null) return
        player = ExoPlayer.Builder(this).build().also {
            it.trackSelectionParameters = StreamFactory.withLanguage(it.trackSelectionParameters, Prefs(this).subtitleLanguage)
            it.addListener(listener)
            playerView.player = it
        }
        play(asHls = false)
    }

    override fun onStop() {
        super.onStop()
        player?.let {
            resumePositionMs = if (PlaybackQueue.current?.isLive == true) C.TIME_UNSET else it.currentPosition
            it.release()
        }
        player = null
    }

    private fun play(asHls: Boolean) {
        val item = PlaybackQueue.current ?: return
        val player = player ?: return
        triedAsHls = asHls

        player.setMediaSource(StreamFactory.mediaSource(item, asHls), resumePositionMs)
        player.prepare()
        player.playWhenReady = true

        val prefix = if (item.isLive && PlaybackQueue.items.size > 1) "${PlaybackQueue.index + 1}  " else ""
        showInfo(prefix + item.title)
    }

    private fun zap(delta: Int) {
        val size = PlaybackQueue.items.size
        if (size < 2) return
        PlaybackQueue.index = (PlaybackQueue.index + delta).mod(size)
        resumePositionMs = C.TIME_UNSET
        play(asHls = false)
    }

    private fun showInfo(text: String, durationMs: Long = 4000) {
        infoText.text = text
        infoText.visibility = View.VISIBLE
        handler.removeCallbacks(hideInfo)
        handler.postDelayed(hideInfo, durationMs)
    }

    private val listener = object : Player.Listener {
        override fun onPlayerError(error: PlaybackException) {
            val p = player ?: return
            when {
                error.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW -> {
                    p.seekToDefaultPosition(); p.prepare()
                }
                !triedAsHls -> play(asHls = true)
                else -> {
                    val hint = if (PlaybackQueue.current?.isLive == true) "  ·  ▲▼ to change channel" else ""
                    showInfo("Can't play this stream (${error.errorCodeName})$hint", durationMs = 8000)
                }
            }
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val live = PlaybackQueue.current?.isLive == true
        if (live && event.action == KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_CHANNEL_UP -> { zap(+1); return true }
                KeyEvent.KEYCODE_CHANNEL_DOWN -> { zap(-1); return true }
                KeyEvent.KEYCODE_DPAD_UP -> if (!playerView.isControllerFullyVisible) { zap(-1); return true }
                KeyEvent.KEYCODE_DPAD_DOWN -> if (!playerView.isControllerFullyVisible) { zap(+1); return true }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onDestroy() {
        handler.removeCallbacks(hideInfo)
        super.onDestroy()
    }
}
