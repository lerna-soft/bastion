package com.bastion.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

enum class ColorMode { DARK, STITCH_GREEN, LIGHT, MONOKAI, SYSTEM, OLED_DARK }

private val NeutralDarkColorScheme = darkColorScheme(
    primary = NeutralDarkPrimary,
    onPrimary = NeutralDarkOnPrimary,
    primaryContainer = NeutralDarkPrimaryContainer,
    onPrimaryContainer = NeutralDarkOnPrimaryContainer,
    secondary = NeutralDarkSecondary,
    onSecondary = NeutralDarkOnSecondary,
    secondaryContainer = NeutralDarkSecondaryContainer,
    onSecondaryContainer = NeutralDarkOnSecondaryContainer,
    background = NeutralDarkBackground,
    onBackground = NeutralDarkOnSurface,
    surface = NeutralDarkSurface,
    onSurface = NeutralDarkOnSurface,
    surfaceVariant = NeutralDarkSurfaceVariant,
    onSurfaceVariant = NeutralDarkOnSurfaceVariant,
    outline = NeutralDarkOutline,
    outlineVariant = NeutralDarkOutlineVariant,
    error = NeutralDarkError,
    onError = NeutralDarkOnError,
    errorContainer = NeutralDarkErrorContainer,
    onErrorContainer = NeutralDarkOnErrorContainer,
)

private val StitchGreenColorScheme = darkColorScheme(
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

private val MonokaiColorScheme = darkColorScheme(
    primary = MonokaiPrimary,
    onPrimary = MonokaiOnPrimary,
    primaryContainer = MonokaiPrimaryContainer,
    onPrimaryContainer = MonokaiOnPrimaryContainer,
    secondary = MonokaiSecondary,
    onSecondary = MonokaiOnSecondary,
    secondaryContainer = MonokaiSecondaryContainer,
    onSecondaryContainer = MonokaiOnSecondaryContainer,
    tertiary = MonokaiTertiary,
    tertiaryContainer = MonokaiTertiaryContainer,
    background = MonokaiBackground,
    onBackground = MonokaiOnSurface,
    surface = MonokaiSurface,
    onSurface = MonokaiOnSurface,
    surfaceVariant = MonokaiSurfaceVariant,
    onSurfaceVariant = MonokaiOnSurfaceVariant,
    outline = MonokaiOutline,
    outlineVariant = MonokaiOutlineVariant,
    error = MonokaiError,
    onError = MonokaiOnError,
    errorContainer = MonokaiErrorContainer,
    onErrorContainer = MonokaiOnErrorContainer,
)

private val OledDarkColorScheme = darkColorScheme(
    primary = OledDarkPrimary,
    onPrimary = OledDarkOnPrimary,
    primaryContainer = OledDarkPrimaryContainer,
    onPrimaryContainer = OledDarkOnPrimaryContainer,
    background = OledDarkBackground,
    onBackground = OledDarkOnSurface,
    surface = OledDarkSurface,
    onSurface = OledDarkOnSurface,
    surfaceVariant = OledDarkSurfaceVariant,
    onSurfaceVariant = OledDarkOnSurfaceVariant,
    outline = OledDarkOutline,
    outlineVariant = OledDarkOutlineVariant,
    error = OledDarkError,
    onError = OledDarkOnError,
    errorContainer = OledDarkErrorContainer,
    onErrorContainer = OledDarkOnErrorContainer,
)

@Composable
fun BastionTheme(
    colorMode: ColorMode = ColorMode.DARK,
    applyStatusBar: Boolean = true,
    content: @Composable () -> Unit
) {
    val resolvedDark = when (colorMode) {
        ColorMode.SYSTEM -> isSystemInDarkTheme()
        ColorMode.DARK, ColorMode.STITCH_GREEN, ColorMode.MONOKAI, ColorMode.OLED_DARK -> true
        ColorMode.LIGHT -> false
    }
    val colorScheme = when (colorMode) {
        ColorMode.DARK -> NeutralDarkColorScheme
        ColorMode.STITCH_GREEN -> StitchGreenColorScheme
        ColorMode.LIGHT -> StitchLightColorScheme
        ColorMode.MONOKAI -> MonokaiColorScheme
        ColorMode.OLED_DARK -> OledDarkColorScheme
        ColorMode.SYSTEM -> if (isSystemInDarkTheme()) NeutralDarkColorScheme else StitchLightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode && applyStatusBar) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !resolvedDark
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
