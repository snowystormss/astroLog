package com.example.ui.theme

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

private val DarkColorScheme =
  darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = DarkTertiary,
    onTertiary = DarkOnTertiary,
    tertiaryContainer = DarkTertiaryContainer,
    onTertiaryContainer = DarkOnTertiaryContainer,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerHigh = DarkSurfaceContainerHigh,
    surfaceContainerHighest = DarkSurfaceContainerHighest,
    surfaceContainerLow = DarkSurfaceContainerLow,
    surfaceContainerLowest = DarkSurfaceContainerLowest,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
  )

private val LightColorScheme =
  lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    tertiary = LightTertiary,
    onTertiary = LightOnTertiary,
    tertiaryContainer = LightTertiaryContainer,
    onTertiaryContainer = LightOnTertiaryContainer,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    surfaceContainer = LightSurfaceContainer,
    surfaceContainerHigh = LightSurfaceContainerHigh,
    surfaceContainerHighest = LightSurfaceContainerHighest,
    surfaceContainerLow = LightSurfaceContainerLow,
    surfaceContainerLowest = LightSurfaceContainerLowest,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
  )

val LocalIsDarkTheme = staticCompositionLocalOf { false }

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val targetColorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
  
  val primary by animateColorAsState(targetValue = targetColorScheme.primary, animationSpec = tween(500), label = "primary")
  val onPrimary by animateColorAsState(targetValue = targetColorScheme.onPrimary, animationSpec = tween(500), label = "onPrimary")
  val primaryContainer by animateColorAsState(targetValue = targetColorScheme.primaryContainer, animationSpec = tween(500), label = "primaryContainer")
  val onPrimaryContainer by animateColorAsState(targetValue = targetColorScheme.onPrimaryContainer, animationSpec = tween(500), label = "onPrimaryContainer")
  val secondary by animateColorAsState(targetValue = targetColorScheme.secondary, animationSpec = tween(500), label = "secondary")
  val onSecondary by animateColorAsState(targetValue = targetColorScheme.onSecondary, animationSpec = tween(500), label = "onSecondary")
  val secondaryContainer by animateColorAsState(targetValue = targetColorScheme.secondaryContainer, animationSpec = tween(500), label = "secondaryContainer")
  val onSecondaryContainer by animateColorAsState(targetValue = targetColorScheme.onSecondaryContainer, animationSpec = tween(500), label = "onSecondaryContainer")
  val tertiary by animateColorAsState(targetValue = targetColorScheme.tertiary, animationSpec = tween(500), label = "tertiary")
  val onTertiary by animateColorAsState(targetValue = targetColorScheme.onTertiary, animationSpec = tween(500), label = "onTertiary")
  val tertiaryContainer by animateColorAsState(targetValue = targetColorScheme.tertiaryContainer, animationSpec = tween(500), label = "tertiaryContainer")
  val onTertiaryContainer by animateColorAsState(targetValue = targetColorScheme.onTertiaryContainer, animationSpec = tween(500), label = "onTertiaryContainer")
  val error by animateColorAsState(targetValue = targetColorScheme.error, animationSpec = tween(500), label = "error")
  val onError by animateColorAsState(targetValue = targetColorScheme.onError, animationSpec = tween(500), label = "onError")
  val errorContainer by animateColorAsState(targetValue = targetColorScheme.errorContainer, animationSpec = tween(500), label = "errorContainer")
  val onErrorContainer by animateColorAsState(targetValue = targetColorScheme.onErrorContainer, animationSpec = tween(500), label = "onErrorContainer")
  val background by animateColorAsState(targetValue = targetColorScheme.background, animationSpec = tween(500), label = "background")
  val onBackground by animateColorAsState(targetValue = targetColorScheme.onBackground, animationSpec = tween(500), label = "onBackground")
  val surface by animateColorAsState(targetValue = targetColorScheme.surface, animationSpec = tween(500), label = "surface")
  val onSurface by animateColorAsState(targetValue = targetColorScheme.onSurface, animationSpec = tween(500), label = "onSurface")
  val surfaceVariant by animateColorAsState(targetValue = targetColorScheme.surfaceVariant, animationSpec = tween(500), label = "surfaceVariant")
  val onSurfaceVariant by animateColorAsState(targetValue = targetColorScheme.onSurfaceVariant, animationSpec = tween(500), label = "onSurfaceVariant")
  val outline by animateColorAsState(targetValue = targetColorScheme.outline, animationSpec = tween(500), label = "outline")
  val outlineVariant by animateColorAsState(targetValue = targetColorScheme.outlineVariant, animationSpec = tween(500), label = "outlineVariant")
  val scrim by animateColorAsState(targetValue = targetColorScheme.scrim, animationSpec = tween(500), label = "scrim")
  val inverseSurface by animateColorAsState(targetValue = targetColorScheme.inverseSurface, animationSpec = tween(500), label = "inverseSurface")
  val inverseOnSurface by animateColorAsState(targetValue = targetColorScheme.inverseOnSurface, animationSpec = tween(500), label = "inverseOnSurface")
  val inversePrimary by animateColorAsState(targetValue = targetColorScheme.inversePrimary, animationSpec = tween(500), label = "inversePrimary")
  
  val animatedColorScheme = ColorScheme(
      primary = primary,
      onPrimary = onPrimary,
      primaryContainer = primaryContainer,
      onPrimaryContainer = onPrimaryContainer,
      secondary = secondary,
      onSecondary = onSecondary,
      secondaryContainer = secondaryContainer,
      onSecondaryContainer = onSecondaryContainer,
      tertiary = tertiary,
      onTertiary = onTertiary,
      tertiaryContainer = tertiaryContainer,
      onTertiaryContainer = onTertiaryContainer,
      error = error,
      onError = onError,
      errorContainer = errorContainer,
      onErrorContainer = onErrorContainer,
      background = background,
      onBackground = onBackground,
      surface = surface,
      onSurface = onSurface,
      surfaceVariant = surfaceVariant,
      onSurfaceVariant = onSurfaceVariant,
      outline = outline,
      outlineVariant = outlineVariant,
      scrim = scrim,
      surfaceTint = primary,
      inverseSurface = inverseSurface,
      inverseOnSurface = inverseOnSurface,
      inversePrimary = inversePrimary,
      surfaceBright = targetColorScheme.surfaceBright,
      surfaceDim = targetColorScheme.surfaceDim,
      surfaceContainer = targetColorScheme.surfaceContainer,
      surfaceContainerHigh = targetColorScheme.surfaceContainerHigh,
      surfaceContainerHighest = targetColorScheme.surfaceContainerHighest,
      surfaceContainerLow = targetColorScheme.surfaceContainerLow,
      surfaceContainerLowest = targetColorScheme.surfaceContainerLowest
  )

  CompositionLocalProvider(LocalIsDarkTheme provides darkTheme) {
    MaterialTheme(colorScheme = animatedColorScheme, typography = Typography, content = content)
  }
}
