package com.streamtv.app.ui

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Tab
import androidx.tv.material3.TabRow
import androidx.tv.material3.Text
import com.streamtv.app.MainViewModel
import com.streamtv.app.data.Channel
import com.streamtv.app.data.Movie
import com.streamtv.app.player.PlaybackQueue
import com.streamtv.app.player.PlayerActivity
import kotlinx.coroutines.launch

enum class Section(val label: String) { Home("Home"), Live("Live TV"), Search("Search"), Settings("Settings") }

@Composable
fun App(vm: MainViewModel = viewModel()) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var section by rememberSaveable { mutableStateOf(Section.Home) }
    var detail by remember { mutableStateOf<Movie?>(null) }
    var resolving by remember { mutableStateOf(false) }
    val homeListState = rememberLazyListState()

    fun playMovie(movie: Movie) {
        if (resolving) return
        scope.launch {
            resolving = true
            val item = runCatching { vm.prepareMovie(movie) }.getOrNull()
            resolving = false
            if (item == null) {
                Toast.makeText(context, "No playable video found for “${movie.title}”.", Toast.LENGTH_LONG).show()
            } else {
                vm.markWatched(movie)
                PlaybackQueue.set(listOf(item), 0)
                context.openPlayer()
            }
        }
    }

    fun playChannel(channels: List<Channel>, index: Int) {
        PlaybackQueue.set(channels.map { it.toPlayable() }, index)
        context.openPlayer()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        colors = SurfaceDefaults.colors(containerColor = Palette.Background),
    ) {
        val current = detail
        if (current != null) {
            BackHandler { detail = null }
            DetailScreen(current, loading = resolving, onPlay = { playMovie(current) }, onBack = { detail = null })
        } else {
            Column(Modifier.fillMaxSize()) {
                TopBar(section, onSelect = { section = it })
                when (section) {
                    Section.Home -> HomeScreen(vm, homeListState, onOpen = { detail = it }, onPlayMovie = ::playMovie, onPlayChannel = ::playChannel)
                    Section.Live -> LiveTvScreen(vm, onPlayChannel = ::playChannel)
                    Section.Search -> SearchScreen(vm, onOpen = { detail = it }, onPlayChannel = ::playChannel)
                    Section.Settings -> SettingsScreen(vm)
                }
            }
        }
    }
}

@Composable
private fun TopBar(selected: Section, onSelect: (Section) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = ScreenPadding, end = ScreenPadding, top = 24.dp, bottom = 12.dp),
    ) {
        Text(
            "STREAMTV",
            color = Palette.Red,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
        )
        Spacer(Modifier.width(40.dp))
        TabRow(selectedTabIndex = selected.ordinal) {
            Section.entries.forEach { s ->
                Tab(selected = s == selected, onFocus = { onSelect(s) }, onClick = { onSelect(s) }) {
                    Text(s.label, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
                }
            }
        }
    }
}

private fun Context.openPlayer() = startActivity(Intent(this, PlayerActivity::class.java))
