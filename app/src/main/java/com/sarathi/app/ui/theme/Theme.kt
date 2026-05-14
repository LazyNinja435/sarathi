package com.sarathi.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SarathiDarkColors = darkColorScheme(
    primary = SacredGold,
    onPrimary = Ink,
    primaryContainer = DeepBlue,
    onPrimaryContainer = SoftGold,
    secondary = SoftGold,
    onSecondary = Ink,
    tertiary = PeacockGreen,
    onTertiary = Color.White,
    background = MidnightIndigo,
    onBackground = SoftGold,
    surface = DeepBlue,
    onSurface = SoftGold,
    surfaceVariant = IndigoBubble,
    onSurfaceVariant = SoftGold,
    outline = SacredGold,
)

@Composable
fun SarathiTheme(
    darkTheme: Boolean = isSystemInDarkTheme() || true,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = SarathiDarkColors,
        typography = SarathiTypography,
        content = content,
    )
}
