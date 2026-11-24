package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val KombaDarkColorScheme = darkColorScheme(
    primary = GoldPrimary,
    onPrimary = OnGold,
    primaryContainer = GoldContainer,
    onPrimaryContainer = GoldBright,
    secondary = EmeraldSecondary,
    onSecondary = OnEmerald,
    secondaryContainer = EmeraldContainer,
    onSecondaryContainer = EmeraldBright,
    tertiary = SapphireTertiary,
    tertiaryContainer = SapphireContainer,
    onTertiaryContainer = TextPrimary,
    background = ObsidianBg,
    onBackground = TextPrimary,
    surface = ObsidianSurface,
    onSurface = TextPrimary,
    surfaceVariant = ObsidianSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    surfaceContainer = ObsidianCard,
    surfaceContainerLow = ObsidianSurface,
    surfaceContainerHigh = ObsidianSurfaceVariant,
    outline = ObsidianOutline,
    outlineVariant = ColorTokens.SubtleBorder,
    error = CoralError,
    onError = TextPrimary,
    errorContainer = CoralContainer,
    onErrorContainer = ColorTokens.CoralText
)

private object ColorTokens {
    val SubtleBorder = androidx.compose.ui.graphics.Color(0xFF1E293B)
    val CoralText = androidx.compose.ui.graphics.Color(0xFFFDA4AF)
}

val KombaShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun KombaBoostTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = KombaDarkColorScheme,
        typography = KombaTypography,
        shapes = KombaShapes,
        content = content
    )
}
