package com.cipher.studio.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Mapping our Elite Colors to Material Slots
private val DarkColorScheme = darkColorScheme(
    primary = EliteDarkPrimary,
    onPrimary = Color.Black, // Text on primary button should be dark
    secondary = EliteDarkPrimary,
    background = EliteDarkBackground,
    surface = EliteDarkBackground, // Main screen background
    surfaceContainer = EliteDarkSurface, // For Cards/Modals
    onBackground = EliteDarkOnSurface,
    onSurface = EliteDarkOnSurface,
    surfaceVariant = EliteDarkSurfaceVariant, // For Input Bar & Bubbles
    onSurfaceVariant = EliteDarkOnSurfaceVariant,
    outline = EliteDarkSurfaceVariant
)

private val LightColorScheme = lightColorScheme(
    primary = EliteLightPrimary,
    onPrimary = Color.White,
    secondary = EliteLightPrimary,
    background = EliteLightBackground,
    surface = EliteLightBackground,
    surfaceContainer = EliteLightSurface,
    onBackground = EliteLightOnSurface,
    onSurface = EliteLightOnSurface,
    surfaceVariant = EliteLightSurfaceVariant,
    onSurfaceVariant = Color.DarkGray,
    outline = EliteLightSurfaceVariant
)

@Composable
fun CipherAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // We force Dynamic Color OFF to maintain our specific "Cipher" branding
    dynamicColor: Boolean = false, 
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // --- IMMERSIVE SYSTEM BARS ---
    // This is what makes the app look "Expensive". 
    // The status bar blends perfectly with the background.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            
            // EDGE-TO-EDGE ACTIVATION
            WindowCompat.setDecorFitsSystemWindows(window, false)

            // Set both Status Bar and Nav Bar to the Background Color
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()

            WindowCompat.getInsetsController(window, view).apply {
                // If Dark Mode -> Icons should be Light (true logic inverted usually)
                // correct logic: appearLightStatusBars = !darkTheme (if dark theme, we want light icons -> false)
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        // typography = Typography, // Add Typography.kt later for custom fonts like Google Sans
        content = content
    )
}