package com.streamtv.app.ui

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.streamtv.app.MainViewModel
import com.streamtv.app.data.Channel
import com.streamtv.app.data.Movie

@Composable
fun HomeScreen(
    vm: MainViewModel,
    listState: LazyListState,
    onOpen: (Movie) -> Unit,
    onPlayMovie: (Movie) -> Unit,
    onPlayChannel: (List<Channel>, Int) -> Unit,
) {
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(bottom = 64.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        vm.featured?.let { movie ->
            item(key = "hero") { Hero(movie, onPlay = { onPlayMovie(movie) }, onInfo = { onOpen(movie) }) }
        }
        if (vm.recent.isNotEmpty()) {
            item(key = "recent") { MovieRow("Continue Watching", vm.recent, onOpen) }
        }
        if (vm.channels.isNotEmpty()) {
            item(key = "live") {
                val favs = vm.channelsIn(MainViewModel.FAVORITES)
                ChannelRow(
                    title = if (favs.isNotEmpty()) "Your Favorite Channels" else "Live TV",
                    channels = favs.ifEmpty { vm.channels.take(30) },
                    favorites = vm.favorites,
                    onPlay = onPlayChannel,
                    onToggleFavorite = vm::toggleFavorite,
                )
            }
        }
        items(vm.shelves, key = { it.title }) { shelf -> MovieRow(shelf.title, shelf.movies, onOpen) }

        when {
            vm.moviesLoading && vm.shelves.isEmpty() -> item { StatusMessage("Loading movies & shows…") }
            vm.moviesError != null -> item { StatusMessage(vm.moviesError!!, onRetry = vm::loadMovies) }
        }
    }
}

@Composable
private fun Hero(movie: Movie, onPlay: () -> Unit, onInfo: () -> Unit) {
    Box(Modifier.fillMaxWidth().height(380.dp)) {
        // Soft, blurred backdrop (blur needs Android 12+; older TVs just get a dimmer image).
        AsyncImage(
            model = movie.poster,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alpha = if (Build.VERSION.SDK_INT >= 31) 0.45f else 0.2f,
            modifier = Modifier.fillMaxSize().blur(40.dp),
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.horizontalGradient(0f to Palette.Background, 0.55f to Palette.Background.copy(alpha = 0.6f), 1f to Color.Transparent),
            ),
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(0.6f to Color.Transparent, 1f to Palette.Background),
            ),
        )
        Row(
            Modifier.fillMaxSize().padding(horizontal = ScreenPadding, vertical = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("FEATURED", color = Palette.Red, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                Spacer(Modifier.height(8.dp))
                Text(
                    movie.title,
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                movie.year?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(it, color = Palette.Muted, style = MaterialTheme.typography.titleMedium)
                }
                movie.description?.let {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        it,
                        color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth(0.85f),
                    )
                }
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(
                        onClick = onPlay,
                        colors = ButtonDefaults.colors(
                            containerColor = Color.White,
                            contentColor = Color.Black,
                            focusedContainerColor = Palette.Red,
                            focusedContentColor = Color.White,
                        ),
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Play")
                    }
                    OutlinedButton(onClick = onInfo) {
                        Icon(Icons.Filled.Info, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("More Info")
                    }
                }
            }
            AsyncImage(
                model = movie.poster,
                contentDescription = movie.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(10.dp)),
            )
        }
    }
}
