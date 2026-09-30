package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val TripMateColorScheme = lightColorScheme(
    primary = TravelBluePrimary,
    onPrimary = Color.White,
    primaryContainer = TravelBlueLight,
    onPrimaryContainer = TravelBlueDark,
    secondary = TravelTeal,
    onSecondary = Color.White,
    tertiary = TravelOrange,
    background = BackgroundLight,
    onBackground = TextPrimary,
    surface = SurfaceLight,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = TextSecondary,
    outline = CardBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = TripMateColorScheme,
        typography = Typography,
        content = content
    )
}
