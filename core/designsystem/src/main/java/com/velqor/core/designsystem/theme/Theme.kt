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
    val surfaceElevated: Color,
    val label: Color,
    val secondaryLabel: Color,
    val separator: Color,
    val accent: Color,
    val danger: Color,
    val success: Color,
    val iconFolder: Color,
    val iconImage: Color,
    val iconVideo: Color,
    val iconAudio: Color,
    val iconDocument: Color,
    val iconArchive: Color,
    val iconApk: Color,
    val iconCode: Color
) {
    val textPrimary: Color get() = label
    val textSecondary: Color get() = secondaryLabel
    val textTertiary: Color get() = secondaryLabel.copy(alpha = 0.5f)
    val surface: Color get() = surfaceElevated
}

val LocalVelqorColors = staticCompositionLocalOf {
    VelqorColors(
        background = DarkBackground,
        surfaceElevated = DarkSurfaceElevated,
        label = DarkLabel,
        secondaryLabel = DarkSecondaryLabel,
        separator = DarkSeparator,
        accent = DarkAccent,
        danger = DarkDanger,
        success = DarkSuccess,
        iconFolder = DarkIconFolder,
        iconImage = DarkIconImage,
        iconVideo = DarkIconVideo,
        iconAudio = DarkIconAudio,
        iconDocument = DarkIconDocument,
        iconArchive = DarkIconArchive,
        iconApk = DarkIconApk,
        iconCode = DarkIconCode
    )
}

private val DarkColorScheme = darkColorScheme(
    primary = DarkAccent,
    onPrimary = Color.White,
    background = DarkBackground,
    onBackground = DarkLabel,
    surface = DarkSurfaceElevated,
    onSurface = DarkLabel,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = DarkSecondaryLabel,
    outline = DarkSeparator,
    error = DarkDanger
)

private val LightColorScheme = lightColorScheme(
    primary = LightAccent,
    onPrimary = Color.White,
    background = LightBackground,
    onBackground = LightLabel,
    surface = LightSurfaceElevated,
    onSurface = LightLabel,
    surfaceVariant = LightSurfaceElevated,
    onSurfaceVariant = LightSecondaryLabel,
    outline = LightSeparator,
    error = LightDanger
)

@Composable
fun VelqorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val velqorColors = if (darkTheme) {
        VelqorColors(
            background = DarkBackground,
            surfaceElevated = DarkSurfaceElevated,
            label = DarkLabel,
            secondaryLabel = DarkSecondaryLabel,
            separator = DarkSeparator,
            accent = DarkAccent,
            danger = DarkDanger,
            success = DarkSuccess,
            iconFolder = DarkIconFolder,
            iconImage = DarkIconImage,
            iconVideo = DarkIconVideo,
            iconAudio = DarkIconAudio,
            iconDocument = DarkIconDocument,
            iconArchive = DarkIconArchive,
            iconApk = DarkIconApk,
            iconCode = DarkIconCode
        )
    } else {
        VelqorColors(
            background = LightBackground,
            surfaceElevated = LightSurfaceElevated,
            label = LightLabel,
            secondaryLabel = LightSecondaryLabel,
            separator = LightSeparator,
            accent = LightAccent,
            danger = LightDanger,
            success = LightSuccess,
            iconFolder = LightIconFolder,
            iconImage = LightIconImage,
            iconVideo = LightIconVideo,
            iconAudio = LightIconAudio,
            iconDocument = LightIconDocument,
            iconArchive = LightIconArchive,
            iconApk = LightIconApk,
            iconCode = LightIconCode
        )
    }

    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(LocalVelqorColors provides velqorColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = VelqorTypography,
            shapes = VelqorShapes,
            content = content
        )
    }
}
