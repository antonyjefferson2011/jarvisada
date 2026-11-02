package com.jarvis.mobile.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val JarvisColors = darkColorScheme(
    primary = Color(0xFF42E8F5),
    onPrimary = Color(0xFF002126),
    secondary = Color(0xFF8CECF2),
    background = Color(0xFF050B12),
    surface = Color(0xFF09131E),
    surfaceVariant = Color(0xFF122231),
    onBackground = Color(0xFFE8F6FA),
    onSurface = Color(0xFFE8F6FA),
    onSurfaceVariant = Color(0xFF9DB6C1),
    error = Color(0xFFFF7585),
)

@Composable
fun JarvisTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = JarvisColors,
        content = content,
    )
}
