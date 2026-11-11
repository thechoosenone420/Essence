package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val EssenceDarkColorScheme = darkColorScheme(
    primary = NothingRed,
    onPrimary = NothingWhite,
    primaryContainer = NothingRedDark,
    onPrimaryContainer = NothingWhite,
    secondary = NothingWhite,
    onSecondary = NothingBlack,
    secondaryContainer = NothingSurfaceVariant,
    onSecondaryContainer = NothingWhite,
    tertiary = NothingGray,
    onTertiary = NothingWhite,
    background = NothingBlack,
    onBackground = NothingWhite,
    surface = NothingDarkCard,
    onSurface = NothingWhite,
    surfaceVariant = NothingSurfaceVariant,
    onSurfaceVariant = NothingGrayLight,
    outline = NothingBorder,
    outlineVariant = NothingBorderSubtle
)

val EssenceShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun EssenceTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = EssenceDarkColorScheme,
        typography = Typography,
        shapes = EssenceShapes,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    EssenceTheme(content = content)
}
