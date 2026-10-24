package com.streamtv.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
import com.streamtv.app.data.Movie

@Composable
fun DetailScreen(movie: Movie, loading: Boolean, onPlay: () -> Unit, onBack: () -> Unit) {
    val playFocus = remember { FocusRequester() }
    LaunchedEffect(movie.id) { playFocus.requestFocus() }

    Box(Modifier.fillMaxSize()) {
        AsyncImage(
            model = movie.poster,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alpha = 0.3f,
            modifier = Modifier.fillMaxSize().blur(60.dp),
        )
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Palette.Background, Color.Transparent))))

        Row(
            Modifier.fillMaxSize().padding(horizontal = 72.dp, vertical = 56.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = movie.poster,
                contentDescription = movie.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxHeight(0.9f).aspectRatio(2f / 3f).clip(RoundedCornerShape(12.dp)),
            )
            Spacer(Modifier.width(48.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    movie.title,
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    listOfNotNull(movie.year, "Public domain", "HD where available").joinToString("  •  "),
                    color = Palette.Muted,
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(20.dp))
                Text(
                    movie.description ?: "No description available.",
                    color = Color.White.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 8,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(28.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(
                        onClick = onPlay,
                        enabled = !loading,
                        modifier = Modifier.focusRequester(playFocus),
                        colors = ButtonDefaults.colors(
                            containerColor = Color.White,
                            contentColor = Color.Black,
                            focusedContainerColor = Palette.Red,
                            focusedContentColor = Color.White,
                        ),
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (loading) "Loading…" else "Play")
                    }
                    OutlinedButton(onClick = onBack) { Text("Back") }
                }
            }
        }
    }
}
