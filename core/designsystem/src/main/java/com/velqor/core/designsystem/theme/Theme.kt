package com.velqor.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class VelqorColors(
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val surfaceSecondary: Color,
    val separator: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val accent: Color
)

val LocalVelqorColors = staticCompositionLocalOf {
    VelqorColors(
        background = DarkBackground,
        surface = DarkSurface,
        surfaceElevated = DarkSurfaceElevated,
        surfaceSecondary = DarkSurfaceSecondary,
        separator = DarkSeparator,
        textPrimary = DarkTextPrimary,
        textSecondary = DarkTextSecondary,
        textTertiary = DarkTextTertiary,
        accent = AppleBlue
    )
}

private val DarkColorScheme = darkColorScheme(
    primary = AppleBlue,
    onPrimary = Color.White,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkSeparator
)

private val LightColorScheme = lightColorScheme(
    primary = AppleBlue,
    onPrimary = Color.White,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceElevated,
    onSurfaceVariant = LightTextSecondary,
    outline = LightSeparator
)

@Composable
fun VelqorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val velqorColors = if (darkTheme) {
        VelqorColors(
            background = DarkBackground,
            surface = DarkSurface,
            surfaceElevated = DarkSurfaceElevated,
            surfaceSecondary = DarkSurfaceSecondary,
            separator = DarkSeparator,
            textPrimary = DarkTextPrimary,
            textSecondary = DarkTextSecondary,
            textTertiary = DarkTextTertiary,
            accent = AppleBlue
        )
    } else {
        VelqorColors(
            background = LightBackground,
            surface = LightSurface,
            surfaceElevated = LightSurfaceElevated,
            surfaceSecondary = LightSurfaceSecondary,
            separator = LightSeparator,
            textPrimary = LightTextPrimary,
            textSecondary = LightTextSecondary,
            textTertiary = LightTextTertiary,
            accent = AppleBlue
        )
    }

    CompositionLocalProvider(LocalVelqorColors provides velqorColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = VelqorTypography,
            shapes = VelqorShapes,
            content = content
        )
    }
}
