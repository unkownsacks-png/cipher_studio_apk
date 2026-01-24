package com.cipher.studio

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.cipher.studio.domain.model.Theme
import com.cipher.studio.presentation.main.MainScreen
import com.cipher.studio.presentation.main.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // --- REAL FIX 1: EDGE-TO-EDGE DISPLAY ---
        // This command tells the system: "Don't draw the white bars. Let the app content flow behind the battery/time."
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            // We pass the view model to the theme to observe changes instantly
            CipherAppTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    color = MaterialTheme.colorScheme.background,
                    // Ensures the surface takes up the entire window size
                ) {
                    MainScreen()
                }
            }
        }
    }
}

// --- PROFESSIONAL THEME SETUP ---
@Composable
fun CipherAppTheme(
    mainViewModel: MainViewModel = hiltViewModel(),
    content: @Composable () -> Unit
) {
    // Observer: Watches for theme changes from Settings
    val themeState by mainViewModel.theme.collectAsState()
    
    // Auto-detect system setting if user hasn't forced a preference, 
    // otherwise use the user's choice.
    val darkTheme = when(themeState) {
        Theme.DARK -> true
        Theme.LIGHT -> false
        else -> isSystemInDarkTheme() // Default to system
    }

    // --- REAL FIX 2: THE "GEMINI" COLOR PALETTE ---
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            // Primary: The Emerald Green from your Logo (#10B981)
            primary = Color(0xFF10B981), 
            onPrimary = Color.Black, // Text on green button should be black for contrast
            
            // Secondary: A softer version of the green
            secondary = Color(0xFF34D399),
            
            // Background: THE MATTE GREY (Not Pure Black)
            // #131314 is the exact hex code used by modern AI assistants for less eye strain
            background = Color(0xFF131314), 
            
            // Surface: Slightly lighter grey for Cards/Sidebars
            surface = Color(0xFF1E1F20), 
            surfaceVariant = Color(0xFF2D2E35), // For Input bars
            
            onBackground = Color(0xFFE3E3E3), // Off-white text (softer on eyes)
            onSurface = Color(0xFFE3E3E3)
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF10B981),
            onPrimary = Color.White,
            secondary = Color(0xFF059669),
            background = Color(0xFFFFFFFF), // Pure white for clean light mode
            surface = Color(0xFFF3F4F6),    // Very light grey for contrast
            onBackground = Color(0xFF111827),
            onSurface = Color(0xFF111827)
        )
    }

    // --- REAL FIX 3: SYSTEM BAR ICON CONTROLLER ---
    // This logic ensures that if the background is Dark, the battery/clock icons turn White.
    // If background is Light, icons turn Black.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            
            // Make the bars completely transparent
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()

            // Toggle icon colors based on theme
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}