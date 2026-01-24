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

// Define the Dark Theme Scheme
private val DarkColorScheme = darkColorScheme(
    primary = CipherBlue,
    secondary = CipherGreen,
    tertiary = CipherPurple,
    background = DarkBackground,
    surface = DarkSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = DarkTextPrimary,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceHighlight,
    outline = BorderDark
)

// Define the Light Theme Scheme
private val LightColorScheme = lightColorScheme(
    primary = CipherBlue,
    secondary = CipherGreen,
    tertiary = CipherPurple,
    background = LightBackground,
    surface = LightSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = LightTextPrimary,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceHighlight,
    outline = BorderLight
)

@Composable
fun CipherAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(), // Auto-detect system preference
    // Dynamic color is available on Android 12+, but we turn it OFF by default 
    // to maintain our "Cipher Elite" brand identity.
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

    // --- IMMERSIVE SYSTEM BARS (The "Expensive App" Look) ---
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            
            // Make Status Bar & Nav Bar transparent to let content flow behind them
            // or match the background color for a seamless look.
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()

            // Control the icons (Light icons for dark bg, Dark icons for light bg)
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        // typography = Typography, // We can add custom font later if needed
        content = content
    )
}