package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DevCraftDarkColorScheme =
  darkColorScheme(
    primary = CyanAccent,
    onPrimary = ObsidianBg,
    primaryContainer = CyanContainerDark,
    onPrimaryContainer = CyanContainerLight,
    secondary = AmberAccent,
    onSecondary = ObsidianBg,
    secondaryContainer = AmberContainerDark,
    onSecondaryContainer = AmberContainerLight,
    tertiary = EmeraldAccent,
    onTertiary = ObsidianBg,
    tertiaryContainer = EmeraldContainerDark,
    onTertiaryContainer = EmeraldContainerLight,
    background = ObsidianBg,
    onBackground = SlateTextLight,
    surface = ObsidianSurface,
    onSurface = SlateTextLight,
    surfaceVariant = ObsidianSurfaceHigh,
    onSurfaceVariant = SlateTextMuted,
    outline = ObsidianOutline,
    error = ErrorRed,
    onError = Color.White,
    errorContainer = ErrorContainerDark,
    onErrorContainer = ErrorContainerLight,
  )

private val DevCraftLightColorScheme =
  lightColorScheme(
    primary = CyanDeep,
    onPrimary = Color.White,
    primaryContainer = CyanContainerLight,
    onPrimaryContainer = CyanContainerDark,
    secondary = AmberDark,
    onSecondary = Color.White,
    secondaryContainer = AmberContainerLight,
    onSecondaryContainer = AmberContainerDark,
    tertiary = EmeraldDark,
    onTertiary = Color.White,
    tertiaryContainer = EmeraldContainerLight,
    onTertiaryContainer = EmeraldContainerDark,
    background = AlabasterBg,
    onBackground = InkTextDark,
    surface = AlabasterSurface,
    onSurface = InkTextDark,
    surfaceVariant = AlabasterSurfaceHigh,
    onSurfaceVariant = InkTextMuted,
    outline = AlabasterOutline,
    error = ErrorRed,
    onError = Color.White,
    errorContainer = ErrorContainerLight,
    onErrorContainer = ErrorContainerDark,
  )

@Composable
fun DevCraftTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }
      darkTheme -> DevCraftDarkColorScheme
      else -> DevCraftLightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = DevCraftTypography, content = content)
}
