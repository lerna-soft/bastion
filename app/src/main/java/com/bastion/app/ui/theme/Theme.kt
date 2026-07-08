package com.bastion.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val StitchDarkColorScheme = darkColorScheme(
    primary = StitchPrimary,
    onPrimary = StitchOnPrimary,
    primaryContainer = StitchPrimaryContainer,
    onPrimaryContainer = StitchOnPrimaryContainer,
    secondary = StitchSecondary,
    onSecondary = StitchOnSecondary,
    secondaryContainer = StitchSecondaryContainer,
    onSecondaryContainer = StitchOnSecondaryContainer,
    tertiary = StitchTertiary,
    tertiaryContainer = StitchTertiaryContainer,
    background = StitchBackground,
    onBackground = StitchOnSurface,
    surface = StitchSurface,
    onSurface = StitchOnSurface,
    surfaceVariant = StitchSurfaceVariant,
    onSurfaceVariant = StitchOnSurfaceVariant,
    outline = StitchOutline,
    outlineVariant = StitchOutlineVariant,
    error = StitchError,
    onError = StitchOnError,
    errorContainer = StitchErrorContainer,
    onErrorContainer = StitchOnErrorContainer,
)

private val StitchLightColorScheme = lightColorScheme(
    primary = StitchLightPrimary,
    onPrimary = StitchLightOnPrimary,
    primaryContainer = StitchLightPrimaryContainer,
    onPrimaryContainer = StitchLightOnPrimaryContainer,
    secondary = StitchLightSecondary,
    onSecondary = StitchLightOnSecondary,
    secondaryContainer = StitchLightSecondaryContainer,
    onSecondaryContainer = StitchLightOnSecondaryContainer,
    tertiary = StitchLightTertiary,
    tertiaryContainer = StitchLightTertiaryContainer,
    background = StitchLightBackground,
    onBackground = StitchLightOnSurface,
    surface = StitchLightSurface,
    onSurface = StitchLightOnSurface,
    surfaceVariant = StitchLightSurfaceVariant,
    onSurfaceVariant = StitchLightOnSurfaceVariant,
    outline = StitchLightOutline,
    outlineVariant = StitchLightOutlineVariant,
    error = StitchLightError,
    onError = StitchLightOnError,
    errorContainer = StitchLightErrorContainer,
    onErrorContainer = StitchLightOnErrorContainer,
)

@Composable
fun BastionTheme(
    isDarkMode: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (isDarkMode) StitchDarkColorScheme else StitchLightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDarkMode
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
