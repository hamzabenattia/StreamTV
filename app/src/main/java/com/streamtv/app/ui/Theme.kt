package com.streamtv.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

object Palette {
    val Red = Color(0xFFE50914)
    val Background = Color(0xFF141414)
    val Card = Color(0xFF232323)
    val Field = Color(0xFF2A2A2A)
    val Muted = Color(0xFFB3B3B3)
}

@Composable
fun StreamTvTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Palette.Red,
            onPrimary = Color.White,
            background = Palette.Background,
            surface = Palette.Background,
            onSurface = Color.White,
            onBackground = Color.White,
            surfaceVariant = Palette.Card,
            onSurfaceVariant = Palette.Muted,
        ),
        content = content,
    )
}
