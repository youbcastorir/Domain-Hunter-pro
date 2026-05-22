package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = ElectricBlue,
    secondary = AccentCyan,
    tertiary = PositiveGreen,
    background = BackgroundNavy,
    surface = CardNavy,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    primaryContainer = ElectricBlue.copy(alpha = 0.2f),
    onPrimaryContainer = TextPrimary,
    surfaceVariant = CardNavy,
    onSurfaceVariant = TextSecondary,
    error = CriticalRed,
    onError = TextPrimary
  )

private val LightColorScheme = DarkColorScheme // Enforce dark scheme for design consistency

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force dark mode as default/standard theme
  dynamicColor: Boolean = false, // Disable dynamic colors to keep brand colors intact
  content: @Composable () -> Unit,
) {
  val colorScheme = DarkColorScheme
  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
