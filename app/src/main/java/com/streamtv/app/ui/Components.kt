package com.streamtv.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.streamtv.app.data.Channel
import com.streamtv.app.data.Movie

val ScreenPadding = 48.dp

private val focusBorder
    @Composable get() = CardDefaults.border(
        focusedBorder = Border(BorderStroke(3.dp, Color.White), shape = RoundedCornerShape(8.dp)),
    )

@Composable
fun PosterCard(movie: Movie, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        modifier = modifier.width(150.dp).aspectRatio(2f / 3f),
        shape = CardDefaults.shape(RoundedCornerShape(8.dp)),
        border = focusBorder,
        colors = CardDefaults.colors(containerColor = Palette.Card),
    ) {
        Box(Modifier.fillMaxSize()) {
            AsyncImage(
                model = movie.poster,
                contentDescription = movie.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))))
                    .padding(start = 8.dp, end = 8.dp, top = 24.dp, bottom = 8.dp),
            ) {
                Text(
                    movie.title,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun ChannelCard(
    channel: Channel,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier.width(200.dp).aspectRatio(16f / 10f),
        shape = CardDefaults.shape(RoundedCornerShape(8.dp)),
        border = focusBorder,
        colors = CardDefaults.colors(containerColor = Palette.Card),
    ) {
        Box(Modifier.fillMaxSize()) {
            if (channel.logo != null) {
                AsyncImage(
                    model = channel.logo,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 36.dp),
                )
            } else {
                Text(
                    channel.name.take(2).uppercase(),
                    style = MaterialTheme.typography.headlineMedium,
                    color = Palette.Muted,
                    modifier = Modifier.align(Alignment.Center).padding(bottom = 20.dp),
                )
            }
            Text(
                channel.name,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.BottomStart).padding(horizontal = 10.dp, vertical = 8.dp),
            )
            if (isFavorite) {
                Icon(
                    Icons.Filled.Star,
                    contentDescription = "Favorite",
                    tint = Color(0xFFFFC107),
                    modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(18.dp),
                )
            }
        }
    }
}

@Composable
fun RowHeader(title: String, edgePadding: Dp = ScreenPadding) {
    Text(
        title,
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        color = Color.White,
        modifier = Modifier.padding(start = edgePadding, bottom = 12.dp),
    )
}

@Composable
fun MovieRow(title: String, movies: List<Movie>, onOpen: (Movie) -> Unit) {
    Column {
        RowHeader(title)
        LazyRow(
            contentPadding = PaddingValues(horizontal = ScreenPadding, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(movies, key = { it.id }) { movie -> PosterCard(movie, onClick = { onOpen(movie) }) }
        }
    }
}

@Composable
fun ChannelRow(
    title: String,
    channels: List<Channel>,
    favorites: Set<String>,
    onPlay: (List<Channel>, Int) -> Unit,
    onToggleFavorite: (Channel) -> Unit,
    edgePadding: Dp = ScreenPadding,
) {
    Column {
        RowHeader(title, edgePadding)
        LazyRow(
            contentPadding = PaddingValues(horizontal = edgePadding, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(channels.size) { i ->
                val ch = channels[i]
                ChannelCard(ch, ch.url in favorites, onClick = { onPlay(channels, i) }, onLongClick = { onToggleFavorite(ch) })
            }
        }
    }
}

@Composable
fun TvTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
    imeAction: ImeAction = ImeAction.Search,
) {
    var focused by remember { mutableStateOf(false) }
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = TextStyle(color = Color.White, fontSize = 20.sp),
        cursorBrush = SolidColor(Palette.Red),
        keyboardOptions = KeyboardOptions(imeAction = imeAction),
        keyboardActions = KeyboardActions(onDone = { onSubmit() }, onSearch = { onSubmit() }, onGo = { onSubmit() }),
        modifier = modifier.onFocusChanged { focused = it.isFocused },
        decorationBox = { inner ->
            Box(
                Modifier
                    .background(Palette.Field, RoundedCornerShape(6.dp))
                    .border(2.dp, if (focused) Color.White else Color.Transparent, RoundedCornerShape(6.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                if (value.isEmpty()) Text(placeholder, color = Palette.Muted, fontSize = 20.sp)
                inner()
            }
        },
    )
}

@Composable
fun StatusMessage(text: String, onRetry: (() -> Unit)? = null) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = ScreenPadding, vertical = 16.dp),
    ) {
        Text(text, color = Palette.Muted, style = MaterialTheme.typography.bodyLarge)
        if (onRetry != null) {
            Spacer(Modifier.width(16.dp))
            Button(onClick = onRetry) { Text("Retry") }
        }
    }
}

@Composable
fun VerticalGap(height: Int) = Spacer(Modifier.height(height.dp))
