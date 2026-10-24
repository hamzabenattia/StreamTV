package com.streamtv.app.player

import com.streamtv.app.data.PlayableItem

/** Hands the player its list without squeezing thousands of channels through an Intent. */
object PlaybackQueue {
    var items: List<PlayableItem> = emptyList()
        private set
    var index: Int = 0

    val current: PlayableItem? get() = items.getOrNull(index)

    fun set(items: List<PlayableItem>, index: Int) {
        this.items = items
        this.index = index.coerceIn(0, (items.size - 1).coerceAtLeast(0))
    }
}
