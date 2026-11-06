package com.whistl.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFF6D00),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF3E1E00),
    onPrimaryContainer = Color(0xFFFFDBC8),
    secondary = Color(0xFF00B4D8),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF003544),
    onSecondaryContainer = Color(0xFFBCE9FF),
    tertiary = Color(0xFF00E676),
    background = Color(0xFF121316),
    onBackground = Color(0xFFF1F1F4),
    surface = Color(0xFF1B1D22),
    onSurface = Color(0xFFF1F1F4),
    surfaceVariant = Color(0xFF282B33),
    onSurfaceVariant = Color(0xFFB5BAC6)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFE65100),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE0D2),
    onPrimaryContainer = Color(0xFF3E1500),
    secondary = Color(0xFF007799),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFBCE9FF),
    onSecondaryContainer = Color(0xFF001F29),
    tertiary = Color(0xFF00897B),
    background = Color(0xFFF7F8FA),
    onBackground = Color(0xFF191C20),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF191C20),
    surfaceVariant = Color(0xFFECEFF4),
    onSurfaceVariant = Color(0xFF555B66)
)

@Composable
fun WhistlTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(colorScheme = colorScheme, content = content)
}
