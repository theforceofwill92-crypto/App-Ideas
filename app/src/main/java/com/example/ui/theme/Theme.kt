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

private val DarkColorScheme =
    darkColorScheme(
        primary = GoldPrimary,
        onPrimary = Color(0xFF1E1300),
        primaryContainer = GoldDark,
        onPrimaryContainer = GoldLight,
        secondary = EmeraldAccent,
        onSecondary = Color(0xFF003822),
        secondaryContainer = EmeraldDark,
        onSecondaryContainer = EmeraldLight,
        tertiary = CyanAccent,
        background = NavyDarkBackground,
        onBackground = TextPrimary,
        surface = NavyDarkSurface,
        onSurface = TextPrimary,
        surfaceVariant = NavyDarkSurfaceVariant,
        onSurfaceVariant = TextSecondary,
        outline = NavyDarkCardBorder,
        error = CrimsonAlert,
        onError = Color.White
    )

private val LightColorScheme =
    lightColorScheme(
        primary = GoldDark,
        onPrimary = Color.White,
        primaryContainer = GoldLight,
        onPrimaryContainer = Color(0xFF451A03),
        secondary = EmeraldAccent,
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFD1FAE5),
        onSecondaryContainer = EmeraldDark,
        tertiary = RoyalBlueAccent,
        background = CreamLightBackground,
        onBackground = TextPrimaryLight,
        surface = SlateLightSurface,
        onSurface = TextPrimaryLight,
        surfaceVariant = SlateLightSurfaceVariant,
        onSurfaceVariant = TextSecondaryLight,
        outline = SlateLightBorder,
        error = CrimsonAlert,
        onError = Color.White
    )

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to sleek executive dark theme for sports tycoon feel
    dynamicColor: Boolean = false, // Preserve bespoke commissioner styling
    content: @Composable () -> Unit,
) {
    val colorScheme =
        when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }
            darkTheme -> DarkColorScheme
            else -> LightColorScheme
        }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
