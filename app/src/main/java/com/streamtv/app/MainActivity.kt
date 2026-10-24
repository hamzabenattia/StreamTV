package com.streamtv.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.streamtv.app.ui.App
import com.streamtv.app.ui.StreamTvTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            StreamTvTheme { App() }
        }
    }
}
