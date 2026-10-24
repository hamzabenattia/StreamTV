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
import androidx.tv.material3.FilterChip
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Text
import com.streamtv.app.MainViewModel

/** Free, publicly available channels collected by the iptv-org project (github.com/iptv-org/iptv). */
private val SUBTITLE_LANGUAGES = listOf("ar" to "العربية Arabic", "fr" to "Français", "en" to "English", "" to "Off")

private val PRESETS = listOf(
    "All channels (best for filters)" to "https://iptv-org.github.io/iptv/index.m3u",
    "🇹🇳 Tunisia" to "https://iptv-org.github.io/iptv/countries/tn.m3u",
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
)

@Composable
fun SettingsScreen(vm: MainViewModel) {
    var url by rememberSaveable { mutableStateOf(vm.playlistUrl) }
    var subsKey by rememberSaveable { mutableStateOf(vm.openSubtitlesKey) }

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
            Column {
                Text("Subtitles", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
                Text(
                    "Preferred language — picked automatically when a channel or movie has it. Change it anytime with the CC button in the player.",
                    color = Palette.Muted,
                )
                LazyRow(
                    contentPadding = PaddingValues(vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(SUBTITLE_LANGUAGES) { (code, label) ->
                        FilterChip(selected = vm.subtitleLanguage == code, onClick = { vm.updateSubtitleLanguage(code) }) {
                            Text(label)
                        }
                    }
                }
                Text(
                    "Movie subtitles from OpenSubtitles: create a free account at opensubtitles.com → Consumers → New consumer, then paste the API key here.",
                    color = Palette.Muted,
                )
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 12.dp)) {
                    TvTextField(
                        value = subsKey,
                        onValueChange = { subsKey = it },
                        placeholder = "OpenSubtitles API key (optional)",
                        onSubmit = { vm.updateOpenSubtitlesKey(subsKey) },
                        imeAction = ImeAction.Done,
                        modifier = Modifier.weight(1f),
                    )
                    Button(onClick = { vm.updateOpenSubtitlesKey(subsKey) }, modifier = Modifier.padding(start = 16.dp)) {
                        Text(if (vm.openSubtitlesKey.isNotEmpty() && vm.openSubtitlesKey == subsKey.trim()) "Saved ✓" else "Save")
                    }
                }
            }
        }
        item {
            Column(Modifier.fillMaxWidth().padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Tips", style = MaterialTheme.typography.titleMedium)
                Text("• Paste any M3U / M3U8 playlist URL you're allowed to use, e.g. from your TV provider.", color = Palette.Muted)
                Text("• Live TV: use the Country and Category chips to filter; the highlighted channel plays in the preview.", color = Palette.Muted)
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
