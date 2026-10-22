package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
  primary = ElectricIndigo,
  onPrimary = Color.White,
  primaryContainer = IndigoContainer,
  onPrimaryContainer = ElectricIndigoLight,
  secondary = CyanTelemetry,
  onSecondary = Color.Black,
  secondaryContainer = CyanContainer,
  onSecondaryContainer = CyanTelemetry,
  tertiary = EmeraldPulse,
  onTertiary = Color.Black,
  tertiaryContainer = EmeraldContainer,
  onTertiaryContainer = EmeraldPulse,
  error = CrimsonBlocker,
  onError = Color.White,
  errorContainer = CrimsonContainer,
  onErrorContainer = CrimsonBlocker,
  background = ObsidianBackground,
  onBackground = TextPrimaryDark,
  surface = CarbonSurface,
  onSurface = TextPrimaryDark,
  surfaceVariant = CarbonSurfaceVariant,
  onSurfaceVariant = TextSecondaryDark,
  outline = SlateBorder
)

private val LightColorScheme = lightColorScheme(
  primary = ElectricIndigo,
  onPrimary = Color.White,
  primaryContainer = Color(0xFFE0E7FF),
  onPrimaryContainer = IndigoContainer,
  secondary = Color(0xFF0891B2),
  onSecondary = Color.White,
  secondaryContainer = Color(0xFFCFFAFE),
  onSecondaryContainer = CyanContainer,
  tertiary = Color(0xFF059669),
  onTertiary = Color.White,
  tertiaryContainer = Color(0xFFD1FAE5),
  onTertiaryContainer = EmeraldContainer,
  error = CrimsonBlocker,
  onError = Color.White,
  errorContainer = Color(0xFFFFE4E6),
  onErrorContainer = CrimsonContainer,
  background = AlabasterBackground,
  onBackground = TextPrimaryLight,
  surface = PureWhiteSurface,
  onSurface = TextPrimaryLight,
  surfaceVariant = SoftSlateVariant,
  onSurfaceVariant = TextSecondaryLight,
  outline = Color(0xFFCBD5E1)
)

@Composable
fun ArborFlowTheme(
  darkTheme: Boolean = true,
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  ArborFlowTheme(darkTheme = darkTheme, content = content)
}
