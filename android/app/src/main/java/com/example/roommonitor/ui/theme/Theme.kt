package com.example.roommonitor.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val RoomMonitorColors = lightColorScheme(
    primary = Moss500,
    onPrimary = Sage50,
    secondary = Clay100,
    surface = Sage50,
    surfaceContainerHigh = Sage200,
    onSurface = Moss700,
    onSurfaceVariant = Moss500
)

@Composable
fun RoomMonitorTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = RoomMonitorColors,
        typography = Typography,
        content = content
    )
}
