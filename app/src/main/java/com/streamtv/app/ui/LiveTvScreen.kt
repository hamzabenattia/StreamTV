package com.streamtv.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.FilterChip
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.streamtv.app.MainViewModel
import com.streamtv.app.data.Channel
import com.streamtv.app.data.Countries

@Composable
fun LiveTvScreen(vm: MainViewModel, onPlayChannel: (List<Channel>, Int) -> Unit) {
    when {
        vm.channelsLoading -> { StatusMessage("Loading channels…"); return }
        vm.channels.isEmpty() -> {
            StatusMessage(vm.channelsError ?: "No playlist loaded. Add one in Settings.", onRetry = { vm.loadPlaylist() })
            return
        }
    }

    val filtered = remember(vm.channels, vm.countryFilter, vm.categoryFilter, vm.favorites) { vm.filteredChannels() }
    var focused by remember { mutableStateOf<Channel?>(null) }
    val previewed = focused?.takeIf { it in filtered } ?: filtered.firstOrNull()

    val countryOptions = remember(vm.countries) {
        listOf<Pair<String?, String>>(null to "🌍 All") + vm.countries.map { it to Countries.label(it) }
    }
    val categoryOptions = remember(vm.categories) {
        listOf<Pair<String?, String>>(null to "All", MainViewModel.FAVORITES to MainViewModel.FAVORITES) +
            vm.categories.map { it to it }
    }

    Column(Modifier.fillMaxSize()) {
        PreviewHeader(previewed, Modifier.fillMaxWidth().height(200.dp).padding(horizontal = ScreenPadding))
        LazyVerticalGrid(
            columns = GridCells.Adaptive(200.dp),
            contentPadding = PaddingValues(start = ScreenPadding, end = ScreenPadding, top = 12.dp, bottom = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                FilterRow("Country", countryOptions, vm.countryFilter) { vm.countryFilter = it }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                FilterRow("Category", categoryOptions, vm.categoryFilter) { vm.categoryFilter = it }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    if (filtered.isEmpty()) "No channels match these filters." else "${filtered.size} channels · hold OK to add/remove a favorite",
                    color = Palette.Muted,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            items(filtered.size) { i ->
                val ch = filtered[i]
                ChannelCard(
                    ch,
                    isFavorite = ch.url in vm.favorites,
                    onClick = { onPlayChannel(filtered, i) },
                    onLongClick = { vm.toggleFavorite(ch) },
                    modifier = Modifier.onFocusChanged { if (it.isFocused) focused = ch },
                )
            }
        }
    }
}

@Composable
private fun PreviewHeader(channel: Channel?, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 24.dp)) {
            if (channel != null) {
                if (channel.logo != null) {
                    AsyncImage(
                        model = channel.logo,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(width = 120.dp, height = 56.dp),
                    )
                    Spacer(Modifier.height(10.dp))
                }
                Text(
                    channel.name,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    listOfNotNull(channel.country?.let(Countries::label), channel.categories.joinToString(" · "))
                        .joinToString("   •   "),
                    color = Palette.Muted,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(10.dp))
                Text("● LIVE   Press OK to watch full screen", color = Palette.Red, style = MaterialTheme.typography.labelLarge)
            }
        }
        ChannelPreview(
            channel,
            Modifier.fillMaxHeight().aspectRatio(16f / 9f).clip(RoundedCornerShape(10.dp)),
        )
    }
}

@Composable
private fun FilterRow(
    label: String,
    options: List<Pair<String?, String>>,
    selected: String?,
    onSelect: (String?) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Palette.Muted, style = MaterialTheme.typography.titleSmall, modifier = Modifier.width(90.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 4.dp, horizontal = 4.dp),
        ) {
            items(options, key = { it.first ?: "__all" }) { (key, text) ->
                FilterChip(selected = key == selected, onClick = { onSelect(key) }) { Text(text) }
            }
        }
    }
}
