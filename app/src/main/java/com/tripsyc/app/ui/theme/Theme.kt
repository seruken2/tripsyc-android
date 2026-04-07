package com.tripsyc.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = Coral,
    onPrimary = CardBackground,
    primaryContainer = Coral.copy(alpha = 0.12f),
    onPrimaryContainer = Chalk900,
    secondary = Dusk,
    onSecondary = CardBackground,
    secondaryContainer = Dusk.copy(alpha = 0.12f),
    background = Chalk50,
    onBackground = Chalk900,
    surface = CardBackground,
    onSurface = Chalk900,
    surfaceVariant = Chalk100,
    onSurfaceVariant = Chalk500,
    outline = Chalk200,
    outlineVariant = Chalk100,
    error = Danger,
    onError = CardBackground,
    tertiary = Sage,
    onTertiary = CardBackground,
)

@Composable
fun TripsycTheme(content: @Composable () -> Unit) {
    val colorScheme = LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Chalk50.toArgb()
            window.navigationBarColor = Chalk50.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = true
        }
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
