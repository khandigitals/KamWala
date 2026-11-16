package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Immutable
data class KaamWalaExtras(
    val heroStart: Color = EmeraldDark,
    val heroEnd: Color = SlateDark,
    val cardBorder: Color = SlateBorder,
    val mutedText: Color = SlateMuted,
    val secondaryText: Color = Slate600,
    val emeraldTint: Color = EmeraldLight,
    val emeraldBorder: Color = EmeraldBorder,
    val emeraldMint: Color = EmeraldMint,
    val amberBg: Color = AmberBg,
    val amberBorder: Color = AmberBorder,
    val amberText: Color = AmberText,
    val amberBadgeBg: Color = AmberBadgeBg,
    val amberAccent: Color = AmberAccent,
    val roseAccent: Color = RoseAccent,
    val roseBg: Color = RoseBg,
    val roseBorder: Color = RoseBorder,
    val blueBg: Color = BlueBg,
    val blueText: Color = BlueText,
    val blueTileStart: Color = BlueTileStart,
    val blueTileEnd: Color = BlueTileEnd,
    val purpleBg: Color = PurpleBg,
    val purpleText: Color = PurpleText,
    val dangerBg: Color = DangerBg,
    val dangerText: Color = DangerText,
    val darkSurface: Color = SlateDark,
    val darkSurfaceAlt: Color = Slate800
)

val LocalKaamWalaExtras = staticCompositionLocalOf { KaamWalaExtras() }

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color.White,
    primaryContainer = EmeraldLight,
    onPrimaryContainer = EmeraldDark,
    secondary = SlateDark,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE2E8F0),
    onSecondaryContainer = SlateDark,
    tertiary = RoseAccent,
    onTertiary = Color.White,
    tertiaryContainer = RoseBg,
    onTertiaryContainer = RoseAccent,
    background = SlateBg,
    onBackground = SlateDark,
    surface = SurfaceWhite,
    onSurface = SlateDark,
    surfaceVariant = Color(0xFFF8FAFC),
    onSurfaceVariant = Slate600,
    outline = SlateBorder,
    error = DangerText,
    onError = Color.White,
    errorContainer = DangerBg,
    onErrorContainer = DangerText
)

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldMint,
    onPrimary = EmeraldDark,
    primaryContainer = EmeraldDeep,
    onPrimaryContainer = EmeraldLight,
    secondary = Color(0xFFCBD5E1),
    onSecondary = SlateDark,
    secondaryContainer = Slate800,
    onSecondaryContainer = Color(0xFFF1F5F9),
    tertiary = Color(0xFFF472B6),
    onTertiary = Color(0xFF500724),
    background = Color(0xFF0B1120),
    onBackground = Color(0xFFF8FAFC),
    surface = Color(0xFF111827),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Slate800,
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = Color(0xFF334155),
    error = Color(0xFFFCA5A5),
    onError = Color(0xFF7F1D1D),
    errorContainer = Color(0xFF450A0A),
    onErrorContainer = Color(0xFFFEE2E2)
)

private val KaamWalaShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(26.dp)
)

@Composable
fun KaamWalaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extras = if (darkTheme) {
        KaamWalaExtras(
            cardBorder = Color(0xFF334155),
            mutedText = Color(0xFF94A3B8),
            secondaryText = Color(0xFFCBD5E1),
            emeraldTint = Color(0xFF064E3B),
            emeraldBorder = Color(0xFF10B981),
            amberBg = Color(0xFF422006),
            amberBorder = Color(0xFFD97706),
            amberText = Color(0xFFFDE68A),
            amberBadgeBg = Color(0xFF78350F),
            roseBg = Color(0xFF500724),
            roseBorder = Color(0xFFBE185D),
            blueBg = Color(0xFF1E3A8A),
            blueText = Color(0xFFBFDBFE),
            purpleBg = Color(0xFF4C1D95),
            purpleText = Color(0xFFDDD6FE),
            dangerBg = Color(0xFF7F1D1D),
            dangerText = Color(0xFFFECACA)
        )
    } else {
        KaamWalaExtras()
    }

    CompositionLocalProvider(LocalKaamWalaExtras provides extras) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = KaamWalaTypography,
            shapes = KaamWalaShapes,
            content = content
        )
    }
}
