package com.yourorg.systemcore

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.yourorg.systemcore.core.ui.theme.SystemCoreTheme
import com.yourorg.systemcore.presentation.systemwindow.SystemWindowScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SystemCoreTheme {
                SystemWindowScreen()
            }
        }
    }
}
