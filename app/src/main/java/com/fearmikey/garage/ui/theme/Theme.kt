package com.fearmikey.garage.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = GaragePrimary,
    onPrimary = GarageOnPrimary,
    primaryContainer = GaragePrimaryContainer,
    onPrimaryContainer = GarageOnPrimaryContainer,
    secondary = GarageSecondary,
    onSecondary = GarageOnSecondary,
    secondaryContainer = GarageSecondaryContainer,
    onSecondaryContainer = GarageOnSecondaryContainer,
    tertiary = GarageTertiary,
    onTertiary = GarageOnTertiary,
    tertiaryContainer = GarageTertiaryContainer,
    onTertiaryContainer = GarageOnTertiaryContainer,
    error = GarageError,
    onError = GarageOnError,
    errorContainer = GarageErrorContainer,
    onErrorContainer = GarageOnErrorContainer,
    background = GarageBackground,
    onBackground = GarageOnBackground,
    surface = GarageSurface,
    onSurface = GarageOnSurface,
)

private val DarkColors = darkColorScheme(
    primary = GaragePrimaryContainer,
    onPrimary = GarageOnPrimaryContainer,
    secondary = GarageSecondaryContainer,
    onSecondary = GarageOnSecondaryContainer,
    tertiary = GarageTertiaryContainer,
    onTertiary = GarageOnTertiaryContainer,
)

@Composable
fun GarageTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    // minSdk 34 means dynamic color (Android 12+) is always available.
    val colorScheme = when {
        dynamicColor -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = GarageTypography,
        content = content,
    )
}
