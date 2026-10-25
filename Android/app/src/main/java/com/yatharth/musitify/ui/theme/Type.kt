package com.yatharth.musitify.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.yatharth.musitify.R

// ── Font families ────────────────────────────────────────────────────────────

val JetBrainsMonoNL = FontFamily(
    Font(R.font.jetbrains_mono_nl_regular,    FontWeight.Normal),
    Font(R.font.jetbrains_mono_nl_extrabold,  FontWeight.ExtraBold),
    Font(R.font.jetbrains_mono_nl_extralight, FontWeight.ExtraLight),
)

// ── Adaptive scaling ─────────────────────────────────────────────────────────

@Composable
fun adaptiveSp(baseSp: Float): TextUnit {
    val screenWidth = LocalConfiguration.current.screenWidthDp
    return when {
        screenWidth < 360 -> (baseSp * 0.85f).sp
        screenWidth < 400 -> (baseSp * 0.9f).sp
        screenWidth < 500 -> (baseSp * 1.0f).sp
        screenWidth < 600 -> (baseSp * 1.05f).sp
        else              -> (baseSp * 1.1f).sp
    }
}

// ── Typography ───────────────────────────────────────────────────────────────
//
// Weight system:
//   ExtraLight  → muted labels, secondary text, hints
//   Regular     → body copy, descriptions
//   ExtraBold   → values, headings, anything that needs to punch
//
// Scale rationale:
//   Monospace fonts read slightly smaller than proportional at the same sp,
//   so sizes are nudged up ~1-2sp versus a typical sans-serif scale.

@Composable
fun adaptiveTypography(): Typography {
    return Typography(

        // ── Body ─────────────────────────────────────────────────────────────

        // Small descriptive text, subtitles, secondary info
        bodySmall = TextStyle(
            fontFamily = JetBrainsMonoNL,
            fontWeight = FontWeight.Normal,
            fontSize = adaptiveSp(12f),
            lineHeight = adaptiveSp(18f),
            letterSpacing = 0.3.sp
        ),

        // Default body — transactions, card content
        bodyMedium = TextStyle(
            fontFamily = JetBrainsMonoNL,
            fontWeight = FontWeight.Normal,
            fontSize = adaptiveSp(14f),
            lineHeight = adaptiveSp(22f),
            letterSpacing = 0.2.sp
        ),

        // Prominent body — insight primary values, amounts
        bodyLarge = TextStyle(
            fontFamily = JetBrainsMonoNL,
            fontWeight = FontWeight.Normal,
            fontSize = adaptiveSp(17f),
            lineHeight = adaptiveSp(24f),
            letterSpacing = 0.sp
        ),

        // ── Title / Hero numbers ──────────────────────────────────────────────

        // Large hero number — total spend, big stat
        titleLarge = TextStyle(
            fontFamily = JetBrainsMonoNL,
            fontWeight = FontWeight.ExtraBold,
            fontSize = adaptiveSp(38f),
            lineHeight = adaptiveSp(44f),
            letterSpacing = (-0.5).sp   // tight tracking for large display text
        ),

        // Medium hero — section totals
        titleMedium = TextStyle(
            fontFamily = JetBrainsMonoNL,
            fontWeight = FontWeight.ExtraBold,
            fontSize = adaptiveSp(30f),
            lineHeight = adaptiveSp(36f),
            letterSpacing = (-0.3).sp
        ),

        // Small title — card headers, screen titles
        titleSmall = TextStyle(
            fontFamily = JetBrainsMonoNL,
            fontWeight = FontWeight.ExtraBold,
            fontSize = adaptiveSp(22f),
            lineHeight = adaptiveSp(28f),
            letterSpacing = 0.sp
        ),

        // ── Labels ────────────────────────────────────────────────────────────
        // Use ExtraLight for a refined, receded label feel —
        // pairs beautifully against ExtraBold values in the same card.

        // Tiny caps label — "HIGHEST SPEND", "SPENT", category tags
        labelSmall = TextStyle(
            fontFamily = JetBrainsMonoNL,
            fontWeight = FontWeight.ExtraLight,
            fontSize = adaptiveSp(10f),
            lineHeight = adaptiveSp(16f),
            letterSpacing = 1.5.sp      // wide tracking for all-caps labels
        ),

        // Medium label — section headers, tab labels
        labelMedium = TextStyle(
            fontFamily = JetBrainsMonoNL,
            fontWeight = FontWeight.ExtraLight,
            fontSize = adaptiveSp(12f),
            lineHeight = adaptiveSp(18f),
            letterSpacing = 1.2.sp
        ),

        // Large label — screen-level headings like "INSIGHTS", "TRANSACTIONS"
        labelLarge = TextStyle(
            fontFamily = JetBrainsMonoNL,
            fontWeight = FontWeight.ExtraBold,
            fontSize = adaptiveSp(20f),
            lineHeight = adaptiveSp(26f),
            letterSpacing = 2.sp        // dramatic wide tracking for page titles
        ),

        // ── Headline (used by isHero streak card) ─────────────────────────────
        headlineMedium = TextStyle(
            fontFamily = JetBrainsMonoNL,
            fontWeight = FontWeight.ExtraBold,
            fontSize = adaptiveSp(28f),
            lineHeight = adaptiveSp(34f),
            letterSpacing = (-0.3).sp
        ),
    )
}