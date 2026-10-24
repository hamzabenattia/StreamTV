package com.streamtv.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Text
import com.streamtv.app.MainViewModel

/** Free, publicly available channels collected by the iptv-org project (github.com/iptv-org/iptv). */
private val PRESETS = listOf(
    "News" to "https://iptv-org.github.io/iptv/categories/news.m3u",
    "Movies" to "https://iptv-org.github.io/iptv/categories/movies.m3u",
    "Sports" to "https://iptv-org.github.io/iptv/categories/sports.m3u",
    "Kids" to "https://iptv-org.github.io/iptv/categories/kids.m3u",
    "Music" to "https://iptv-org.github.io/iptv/categories/music.m3u",
    "English" to "https://iptv-org.github.io/iptv/languages/eng.m3u",
    "Arabic" to "https://iptv-org.github.io/iptv/languages/ara.m3u",
    "French" to "https://iptv-org.github.io/iptv/languages/fra.m3u",
    "Spanish" to "https://iptv-org.github.io/iptv/languages/spa.m3u",
    "USA" to "https://iptv-org.github.io/iptv/countries/us.m3u",
    "UK" to "https://iptv-org.github.io/iptv/countries/uk.m3u",
    "Everything (very large)" to "https://iptv-org.github.io/iptv/index.m3u",
)

@Composable
fun SettingsScreen(vm: MainViewModel) {
    var url by rememberSaveable { mutableStateOf(vm.playlistUrl) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = ScreenPadding, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            Text("Live TV playlist (M3U)", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TvTextField(
                    value = url,
                    onValueChange = { url = it },
                    placeholder = "https://example.com/playlist.m3u",
                    onSubmit = { vm.loadPlaylist(url) },
                    imeAction = ImeAction.Go,
                    modifier = Modifier.weight(1f),
                )
                Button(onClick = { vm.loadPlaylist(url) }, modifier = Modifier.padding(start = 16.dp)) {
                    Text("Load")
                }
            }
        }
        item {
            val status = when {
                vm.channelsLoading -> "Loading…"
                vm.channelsError != null -> vm.channelsError!!
                else -> "${vm.channels.size} channels loaded from ${vm.playlistUrl}"
            }
            Text(status, color = Palette.Muted)
        }
        item {
            Column {
                Text("Quick picks (free public channels from iptv-org)", style = MaterialTheme.typography.titleMedium)
                LazyRow(
                    contentPadding = PaddingValues(vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(PRESETS) { (label, presetUrl) ->
                        OutlinedButton(onClick = { url = presetUrl; vm.loadPlaylist(presetUrl) }) { Text(label) }
                    }
                }
            }
        }
        item {
            Column(Modifier.fillMaxWidth().padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Tips", style = MaterialTheme.typography.titleMedium)
                Text("• Paste any M3U / M3U8 playlist URL you're allowed to use, e.g. from your TV provider.", color = Palette.Muted)
                Text("• While watching live TV: ▲ / ▼ or CH+ / CH− switches channels.", color = Palette.Muted)
                Text("• Hold OK on a channel to add it to Favorites; favorites show first on Home.", color = Palette.Muted)
                Text("• Movies & shows come from the Internet Archive's public-domain collections.", color = Palette.Muted)
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Movie library", style = MaterialTheme.typography.titleMedium, modifier = Modifier.width(200.dp))
                OutlinedButton(onClick = vm::loadMovies) { Text("Refresh") }
            }
        }
    }
}
