package com.streamtv.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.streamtv.app.MainViewModel
import com.streamtv.app.data.Channel
import com.streamtv.app.data.Movie

@Composable
fun SearchScreen(vm: MainViewModel, onOpen: (Movie) -> Unit, onPlayChannel: (List<Channel>, Int) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var submitted by rememberSaveable { mutableStateOf("") }
    val channelHits = vm.searchChannels(submitted)

    LazyVerticalGrid(
        columns = GridCells.Adaptive(150.dp),
        contentPadding = PaddingValues(start = ScreenPadding, end = ScreenPadding, top = 8.dp, bottom = 64.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            TvTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = "Search movies, shows and channels…",
                onSubmit = { submitted = query; vm.search(query) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (channelHits.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                // The grid already pads the screen edges.
                ChannelRow("Channels", channelHits, vm.favorites, onPlayChannel, vm::toggleFavorite, edgePadding = 0.dp)
            }
        }
        when {
            vm.searching -> item(span = { GridItemSpan(maxLineSpan) }) { StatusMessage("Searching…") }
            vm.searchError != null -> item(span = { GridItemSpan(maxLineSpan) }) { StatusMessage(vm.searchError!!) }
            submitted.isNotBlank() && vm.searchResults.isEmpty() && channelHits.isEmpty() ->
                item(span = { GridItemSpan(maxLineSpan) }) { StatusMessage("No results for \"$submitted\".") }
        }
        items(vm.searchResults, key = { it.id }) { movie -> PosterCard(movie, onClick = { onOpen(movie) }) }
    }
}
