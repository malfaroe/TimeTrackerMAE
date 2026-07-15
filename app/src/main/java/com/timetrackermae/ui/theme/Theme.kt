package com.timetrackermae.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val TimeTrackerTeal = Color(0xFF00897B)
private val TimeTrackerTealDark = Color(0xFF004D40)

private val LightColors = lightColorScheme(
    primary = TimeTrackerTeal,
    secondary = TimeTrackerTealDark
)

private val DarkColors = darkColorScheme(
    primary = TimeTrackerTeal,
    secondary = TimeTrackerTealDark
)

@Composable
fun TimeTrackerMAETheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, content = content)
}
