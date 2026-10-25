package com.yatharth.musitify.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ── Font families ────────────────────────────────────────────────────────────
// Only Regular / ExtraBold / ExtraLight files exist (like Android). Medium and
// Bold are mapped explicitly to the nearest file, matching what Android picks.

val JetBrainsMonoNL = FontFamily(
    Font(resource = "fonts/jetbrains_mono_nl_regular.ttf", weight = FontWeight.Normal),
    Font(resource = "fonts/jetbrains_mono_nl_regular.ttf", weight = FontWeight.Medium),
    Font(resource = "fonts/jetbrains_mono_nl_extrabold.ttf", weight = FontWeight.ExtraBold),
    Font(resource = "fonts/jetbrains_mono_nl_extrabold.ttf", weight = FontWeight.Bold),
    Font(resource = "fonts/jetbrains_mono_nl_extralight.ttf", weight = FontWeight.ExtraLight),
)

// ── Adaptive scaling ─────────────────────────────────────────────────────────
//
// Android scaled text by screen width (0.85x … 1.1x, ≥600dp ⇒ 1.1x).
// On desktop the window is always "wide", so the factor is derived from the
// window width instead and clamped: ≤900dp ⇒ 1.0x, linearly up to 1.2x at
// ≥2000dp. Provided by [MusitifyTheme] through [LocalUiScale].

const val MIN_SCALE_WIDTH_DP = 900f
const val MAX_SCALE_WIDTH_DP = 2000f
const val MIN_UI_SCALE = 1.0f
const val MAX_UI_SCALE = 1.2f

fun uiScaleFor(windowWidth: Dp): Float {
    val t = ((windowWidth.value - MIN_SCALE_WIDTH_DP) / (MAX_SCALE_WIDTH_DP - MIN_SCALE_WIDTH_DP))
        .coerceIn(0f, 1f)
    return MIN_UI_SCALE + (MAX_UI_SCALE - MIN_UI_SCALE) * t
}

/** Text scale factor for the current window width. */
val LocalUiScale = staticCompositionLocalOf { uiScaleFor(1100.dp) }

@Composable
@ReadOnlyComposable
fun adaptiveSp(baseSp: Float): TextUnit = (baseSp * LocalUiScale.current).sp

private fun scaledSp(base: Float, scale: Float): TextUnit = (base * scale).sp

/** Letter spacing is NOT scaled; sizes and line heights are (same as Android). */
fun adaptiveTypography(scale: Float): Typography = Typography(
    bodySmall = TextStyle(
        fontFamily = JetBrainsMonoNL,
        fontWeight = FontWeight.Normal,
        fontSize = scaledSp(12f, scale),
        lineHeight = scaledSp(18f, scale),
        letterSpacing = 0.3.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = JetBrainsMonoNL,
        fontWeight = FontWeight.Normal,
        fontSize = scaledSp(14f, scale),
        lineHeight = scaledSp(22f, scale),
        letterSpacing = 0.2.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = JetBrainsMonoNL,
        fontWeight = FontWeight.Normal,
        fontSize = scaledSp(17f, scale),
        lineHeight = scaledSp(24f, scale),
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = JetBrainsMonoNL,
        fontWeight = FontWeight.ExtraBold,
        fontSize = scaledSp(38f, scale),
        lineHeight = scaledSp(44f, scale),
        letterSpacing = (-0.5).sp
    ),
    titleMedium = TextStyle(
        fontFamily = JetBrainsMonoNL,
        fontWeight = FontWeight.ExtraBold,
        fontSize = scaledSp(30f, scale),
        lineHeight = scaledSp(36f, scale),
        letterSpacing = (-0.3).sp
    ),
    titleSmall = TextStyle(
        fontFamily = JetBrainsMonoNL,
        fontWeight = FontWeight.ExtraBold,
        fontSize = scaledSp(22f, scale),
        lineHeight = scaledSp(28f, scale),
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = JetBrainsMonoNL,
        fontWeight = FontWeight.ExtraLight,
        fontSize = scaledSp(10f, scale),
        lineHeight = scaledSp(16f, scale),
        letterSpacing = 1.5.sp
    ),
    labelMedium = TextStyle(
        fontFamily = JetBrainsMonoNL,
        fontWeight = FontWeight.ExtraLight,
        fontSize = scaledSp(12f, scale),
        lineHeight = scaledSp(18f, scale),
        letterSpacing = 1.2.sp
    ),
    labelLarge = TextStyle(
        fontFamily = JetBrainsMonoNL,
        fontWeight = FontWeight.ExtraBold,
        fontSize = scaledSp(20f, scale),
        lineHeight = scaledSp(26f, scale),
        letterSpacing = 2.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = JetBrainsMonoNL,
        fontWeight = FontWeight.ExtraBold,
        fontSize = scaledSp(28f, scale),
        lineHeight = scaledSp(34f, scale),
        letterSpacing = (-0.3).sp
    ),
    // headlineLarge intentionally left at the Material3 default (as on Android).
)
