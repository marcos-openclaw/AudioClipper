package com.audioclipper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.audioclipper.ui.MainScreen
import com.audioclipper.ui.theme.AudioClipperTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AudioClipperTheme {
                MainScreen()
            }
        }
    }
}
