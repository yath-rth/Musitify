package com.yatharth.musitify.ui.theme

import androidx.compose.ui.graphics.Color

val accentColor1 = Color(0xFFCB3D38)  // soft coral instead of harsh red
val accentColor2 = Color(0xFF52AB2E)  // green
val accentColor3 = Color(0xFFE7C277)  // golden amber
val background = Color(0xFFDBE0D8)    // soft sage green
val secondaryBg = Color(0xFFDDE3DB)   // slightly darker sage
val itemsBg = Color(0xFFF8FFFF)       // light sage for cards
val text = Color(0xFF1A1A2E)          // near black
val cardBorder = Color(0xFF1A1A2E)    // card outlines / hard shadows
val dividerColor = Color(0xFFCCC7BE)  // outlineVariant

// Semantic roles. Use these instead of the raw accents so colour keeps one meaning:
// green = the main action, red = destructive or error only.
val primaryAction = accentColor2
val danger = accentColor1
// Secondary text (metadata, captions, hints): Tracknix uses text at 70%.
val textSecondary = text.copy(alpha = 0.7f)

/** Press animation length for the hard-shadow buttons (Android DesignSystem BUTTON_ANIMATION_MS). */
const val BUTTON_ANIMATION_MS = 50
