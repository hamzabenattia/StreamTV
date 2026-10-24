package com.streamtv.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ListItem
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.streamtv.app.MainViewModel
import com.streamtv.app.data.Channel

@Composable
fun LiveTvScreen(vm: MainViewModel, onPlayChannel: (List<Channel>, Int) -> Unit) {
    when {
        vm.channelsLoading -> { StatusMessage("Loading channels…"); return }
        vm.channels.isEmpty() -> {
            StatusMessage(vm.channelsError ?: "No playlist loaded. Add one in Settings.", onRetry = { vm.loadPlaylist() })
            return
        }
    }

    val groups = vm.groups
    var selected by rememberSaveable { mutableStateOf(MainViewModel.ALL) }
    if (selected !in groups) selected = MainViewModel.ALL
    val channels = vm.channelsIn(selected)

    Row(Modifier.fillMaxSize().padding(start = ScreenPadding, top = 8.dp)) {
        LazyColumn(
            modifier = Modifier.width(260.dp).fillMaxHeight(),
            contentPadding = PaddingValues(bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(groups, key = { it }) { group ->
                ListItem(
                    selected = group == selected,
                    onClick = { selected = group },
                    headlineContent = { Text(group, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    // Moving through categories updates the channel grid, like a TV guide.
                    modifier = Modifier.onFocusChanged { if (it.isFocused) selected = group },
                )
            }
        }
        Column(Modifier.weight(1f).padding(start = 24.dp)) {
            Text(
                "$selected · ${channels.size} channels   (hold OK to add/remove a favorite)",
                style = MaterialTheme.typography.titleMedium,
                color = Palette.Muted,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            LazyVerticalGrid(
                columns = GridCells.Adaptive(200.dp),
                contentPadding = PaddingValues(end = ScreenPadding, bottom = 48.dp, top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(channels.size) { i ->
                    val ch = channels[i]
                    ChannelCard(
                        ch,
                        isFavorite = ch.url in vm.favorites,
                        onClick = { onPlayChannel(channels, i) },
                        onLongClick = { vm.toggleFavorite(ch) },
                    )
                }
            }
        }
    }
}
