package com.cipher.studio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import com.cipher.studio.domain.model.Theme
import com.cipher.studio.presentation.main.MainScreen
import com.cipher.studio.presentation.main.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CipherAppTheme {
                // A surface container using the 'background' color from the theme
                Surface(color = MaterialTheme.colorScheme.background) {
                    MainScreen()
                }
            }
        }
    }
}

// Simple Theme Setup
@Composable
fun CipherAppTheme(
    mainViewModel: MainViewModel = hiltViewModel(),
    content: @Composable () -> Unit
) {
    // We observe the theme from the ViewModel to switch dynamically
    val themeState by mainViewModel.theme.collectAsState()
    val darkTheme = themeState == Theme.DARK

    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = Color(0xFF2563EB),
            secondary = Color(0xFF10B981),
            background = Color(0xFF020617),
            surface = Color(0xFF0F172A)
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF3B82F6),
            secondary = Color(0xFF34D399),
            background = Color(0xFFF8FAFC),
            surface = Color.White
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}