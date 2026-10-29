package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val TripMateColorScheme = lightColorScheme(
    primary = TripMateBlue,
    onPrimary = Color.White,
    secondary = TripMateOrange,
    onSecondary = Color.White,
    background = Color.White,
    onBackground = TripMateTextPrimary,
    surface = Color.White,
    onSurface = TripMateTextPrimary
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = TripMateColorScheme,
        typography = Typography,
        content = content
    )
}
