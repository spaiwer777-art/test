package com.example.calorietracker.ui.theme

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.calorietracker.data.AccentColor
import com.example.calorietracker.data.ThemeMode

private fun lightScheme(s: AccentSwatch) = lightColorScheme(
    primary = s.lightPrimary,
    onPrimary = Color.White,
    primaryContainer = s.lightContainer,
    onPrimaryContainer = s.lightOnContainer,
    secondary = s.lightPrimary,
    secondaryContainer = s.lightContainer,
    onSecondaryContainer = s.lightOnContainer,
    background = Color(0xFFF6F7F4),
    onBackground = Color(0xFF1A1C19),
    surface = Color(0xFFF6F7F4),
    onSurface = Color(0xFF1A1C19),
    surfaceVariant = Color(0xFFE3E5E0),
    onSurfaceVariant = Color(0xFF52554F),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFBFCF9),
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color(0xFFEEF0EB),
    surfaceContainerHighest = Color(0xFFE6E8E3),
    outlineVariant = Color(0xFFD5D8D1)
)

private fun darkScheme(s: AccentSwatch) = darkColorScheme(
    primary = s.darkPrimary,
    onPrimary = Color(0xFF0E1510),
    primaryContainer = s.darkContainer,
    onPrimaryContainer = s.darkOnContainer,
    secondary = s.darkPrimary,
    secondaryContainer = s.darkContainer,
    onSecondaryContainer = s.darkOnContainer,
    background = Color(0xFF111311),
    onBackground = Color(0xFFE3E4DF),
    surface = Color(0xFF111311),
    onSurface = Color(0xFFE3E4DF),
    surfaceVariant = Color(0xFF2F322E),
    onSurfaceVariant = Color(0xFFC2C5BE),
    surfaceContainerLowest = Color(0xFF0C0E0C),
    surfaceContainerLow = Color(0xFF191B19),
    surfaceContainer = Color(0xFF1C1F1C),
    surfaceContainerHigh = Color(0xFF252825),
    surfaceContainerHighest = Color(0xFF30332F),
    outlineVariant = Color(0xFF3E423D)
)

/** Cross-fades every scheme color so switching theme or accent animates instead of snapping. */
@Composable
private fun ColorScheme.animated(): ColorScheme {
    @Composable
    fun Color.anim(): Color = animateColorAsState(this, tween(450), label = "scheme").value
    return copy(
        primary = primary.anim(),
        onPrimary = onPrimary.anim(),
        primaryContainer = primaryContainer.anim(),
        onPrimaryContainer = onPrimaryContainer.anim(),
        secondary = secondary.anim(),
        secondaryContainer = secondaryContainer.anim(),
        onSecondaryContainer = onSecondaryContainer.anim(),
        background = background.anim(),
        onBackground = onBackground.anim(),
        surface = surface.anim(),
        onSurface = onSurface.anim(),
        surfaceVariant = surfaceVariant.anim(),
        onSurfaceVariant = onSurfaceVariant.anim(),
        surfaceContainerLow = surfaceContainerLow.anim(),
        surfaceContainer = surfaceContainer.anim(),
        surfaceContainerHigh = surfaceContainerHigh.anim(),
        surfaceContainerHighest = surfaceContainerHighest.anim(),
        outlineVariant = outlineVariant.anim()
    )
}

@Composable
fun CalorieTrackerTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    accent: AccentColor = AccentColor.GREEN,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val context = LocalContext.current
    val scheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> darkScheme(accent.swatch)
        else -> lightScheme(accent.swatch)
    }
    MaterialTheme(
        colorScheme = scheme.animated(),
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
