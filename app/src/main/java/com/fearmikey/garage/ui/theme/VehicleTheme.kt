package com.fearmikey.garage.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver

/**
 * Builds a harmonious Material 3 [ColorScheme] seeded by a vehicle's primary signature color.
 */
fun buildVehicleColorScheme(
    seedColor: Color,
    darkTheme: Boolean,
    amoled: Boolean = false,
): ColorScheme {
    val luminance = 0.299f * seedColor.red + 0.587f * seedColor.green + 0.114f * seedColor.blue

    return if (!darkTheme) {
        // In light theme, ensure primary is dark enough to be legible against light background
        val primary = if (luminance > 0.65f) {
            Color(
                red = seedColor.red * 0.7f,
                green = seedColor.green * 0.7f,
                blue = seedColor.blue * 0.7f,
                alpha = 1f,
            )
        } else {
            seedColor
        }
        val onPrimary = if ((0.299f * primary.red + 0.587f * primary.green + 0.114f * primary.blue) > 0.55f) {
            Color(0xFF1A1C1E)
        } else {
            Color.White
        }

        val primaryContainer = seedColor.copy(alpha = 0.16f).compositeOver(Color.White)
        val onPrimaryContainer = primary

        lightColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            secondary = seedColor.copy(alpha = 0.7f).compositeOver(Color(0xFF5C5F72)),
            onSecondary = Color.White,
            secondaryContainer = seedColor.copy(alpha = 0.10f).compositeOver(Color(0xFFE8E8EE)),
            onSecondaryContainer = Color(0xFF191C2B),
            tertiary = GarageTertiary,
            onTertiary = GarageOnTertiary,
            background = Color(0xFFFAF9FD),
            onBackground = Color(0xFF1B1B1F),
            surface = Color(0xFFFAF9FD),
            onSurface = Color(0xFF1B1B1F),
            surfaceContainer = seedColor.copy(alpha = 0.04f).compositeOver(Color(0xFFF2F1F6)),
            surfaceContainerLow = seedColor.copy(alpha = 0.02f).compositeOver(Color(0xFFF7F6FB)),
            surfaceContainerHigh = seedColor.copy(alpha = 0.08f).compositeOver(Color(0xFFECEBF0)),
        )
    } else {
        // In dark theme, ensure primary is bright enough to pop on dark surfaces
        val primary = if (luminance < 0.45f) {
            Color(
                red = minOf(1f, seedColor.red * 1.35f + 0.2f),
                green = minOf(1f, seedColor.green * 1.35f + 0.2f),
                blue = minOf(1f, seedColor.blue * 1.35f + 0.2f),
                alpha = 1f,
            )
        } else {
            seedColor
        }
        val onPrimary = if ((0.299f * primary.red + 0.587f * primary.green + 0.114f * primary.blue) > 0.6f) {
            Color(0xFF121212)
        } else {
            Color.White
        }

        val bg = if (amoled) Color.Black else Color(0xFF121316)
        val primaryContainer = seedColor.copy(alpha = 0.28f).compositeOver(bg)
        val onPrimaryContainer = primary

        darkColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            secondary = seedColor.copy(alpha = 0.6f).compositeOver(Color(0xFFC5C6DE)),
            onSecondary = Color(0xFF2E3042),
            secondaryContainer = seedColor.copy(alpha = 0.20f).compositeOver(Color(0xFF282A36)),
            onSecondaryContainer = Color(0xFFE1E1F9),
            background = bg,
            onBackground = Color(0xFFE2E2E6),
            surface = bg,
            onSurface = Color(0xFFE2E2E6),
            surfaceContainerLowest = if (amoled) Color.Black else Color(0xFF0D0E11),
            surfaceContainerLow = if (amoled) Color(0xFF0D0D0D) else Color(0xFF17181C),
            surfaceContainer = if (amoled) Color(0xFF141414) else Color(0xFF1D1E22),
            surfaceContainerHigh = if (amoled) Color(0xFF1E1E1E) else Color(0xFF27282D),
            surfaceContainerHighest = if (amoled) Color(0xFF282828) else Color(0xFF323338),
            surfaceDim = if (amoled) Color.Black else Color(0xFF101114),
            surfaceBright = if (amoled) Color(0xFF181818) else Color(0xFF38393F),
        )
    }
}
