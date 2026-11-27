package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight

private val LightColorScheme = lightColorScheme(
    primary = LeafGreenPrimaryLight,
    onPrimary = OnLeafGreenPrimaryLight,
    primaryContainer = LeafGreenContainerLight,
    onPrimaryContainer = OnLeafGreenContainerLight,
    secondary = MangoAmberSecondaryLight,
    onSecondary = OnMangoAmberSecondaryLight,
    secondaryContainer = MangoAmberContainerLight,
    onSecondaryContainer = OnMangoAmberContainerLight,
    tertiary = PomegranateTertiaryLight,
    onTertiary = OnPomegranateTertiaryLight,
    tertiaryContainer = PomegranateContainerLight,
    onTertiaryContainer = OnPomegranateContainerLight,
    background = CreamBackgroundLight,
    onBackground = OnCreamBackgroundLight,
    surface = WarmSurfaceLight,
    onSurface = OnWarmSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    surfaceContainerLowest = SurfaceContainerLowestLight,
    surfaceContainerLow = SurfaceContainerLowLight,
    surfaceContainer = SurfaceContainerLight,
    surfaceContainerHigh = SurfaceContainerHighLight,
    surfaceContainerHighest = SurfaceContainerHighestLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight
)

private val DarkColorScheme = darkColorScheme(
    primary = LeafGreenPrimaryDark,
    onPrimary = OnLeafGreenPrimaryDark,
    primaryContainer = LeafGreenContainerDark,
    onPrimaryContainer = OnLeafGreenContainerDark,
    secondary = MangoAmberSecondaryDark,
    onSecondary = OnMangoAmberSecondaryDark,
    secondaryContainer = MangoAmberContainerDark,
    onSecondaryContainer = OnMangoAmberContainerDark,
    tertiary = PomegranateTertiaryDark,
    onTertiary = OnPomegranateTertiaryDark,
    tertiaryContainer = PomegranateContainerDark,
    onTertiaryContainer = OnPomegranateContainerDark,
    background = DeepEarthBackgroundDark,
    onBackground = OnDeepEarthBackgroundDark,
    surface = DeepEarthSurfaceDark,
    onSurface = OnDeepEarthSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    surfaceContainerLow = SurfaceContainerLowDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    surfaceContainerHighest = SurfaceContainerHighestDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark
)

val AnnapurnaShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun AnnapurnaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val language = LocalContext.current.resources.configuration.locales[0].language
    val appTypography = if (language == "en") Typography.copy(
        displayLarge = Typography.displayLarge.copy(fontWeight = FontWeight.Bold),
        displayMedium = Typography.displayMedium.copy(fontWeight = FontWeight.Bold),
        displaySmall = Typography.displaySmall.copy(fontWeight = FontWeight.Bold),
        headlineLarge = Typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
        headlineMedium = Typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
        headlineSmall = Typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
        titleLarge = Typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        titleMedium = Typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        titleSmall = Typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        bodyLarge = Typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
        bodyMedium = Typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
        bodySmall = Typography.bodySmall.copy(fontWeight = FontWeight.Bold),
        labelLarge = Typography.labelLarge.copy(fontWeight = FontWeight.Bold),
        labelMedium = Typography.labelMedium.copy(fontWeight = FontWeight.Bold),
        labelSmall = Typography.labelSmall.copy(fontWeight = FontWeight.Bold)
    ) else Typography

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = appTypography,
        shapes = AnnapurnaShapes,
        content = content
    )
}
