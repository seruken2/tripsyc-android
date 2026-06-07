package com.tripwave.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
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

/// Dark scheme keeps the brand identity — Coral / Dusk / Sage stay vivid on
/// the warm chalk-scale background. Material You dynamic colors are
/// intentionally NOT used so the trip app reads the same regardless of
/// wallpaper; a heavy brand identity like this app's loses too much when
/// the primary swaps to the system palette.
///
/// Wired but NOT yet selected by `TripwaveTheme` — screens currently hardcode
/// raw `Chalk*` / `Coral` references rather than reading from
/// `MaterialTheme.colorScheme.*`, so flipping to this scheme today would only
/// swap the status/nav bar colors while every Composable stays light-themed.
/// Activate after a sweep migrates screens to colorScheme tokens.
@Suppress("unused")
private val DarkColorScheme = darkColorScheme(
    primary = Coral,
    onPrimary = Chalk900,
    primaryContainer = Coral.copy(alpha = 0.20f),
    onPrimaryContainer = CoralLight,
    secondary = Dusk,
    onSecondary = Chalk900,
    secondaryContainer = Dusk.copy(alpha = 0.22f),
    background = Chalk900,
    onBackground = Chalk100,
    surface = CardBackgroundDark,
    onSurface = Chalk100,
    surfaceVariant = Chalk800,
    onSurfaceVariant = Chalk300,
    outline = Chalk700,
    outlineVariant = Chalk800,
    error = Danger,
    onError = Chalk900,
    tertiary = Sage,
    onTertiary = Chalk900,
)

@Composable
fun TripwaveTheme(content: @Composable () -> Unit) {
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
