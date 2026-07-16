package com.yatharth.musitify.ui.elements

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yatharth.musitify.ui.theme.adaptiveTypography
import com.yatharth.musitify.ui.theme.accentColor1
import com.yatharth.musitify.ui.theme.accentColor2
import com.yatharth.musitify.ui.theme.cardBorder
import com.yatharth.musitify.ui.theme.itemsBg
import com.yatharth.musitify.ui.theme.text

// ── CustomSwitch ─────────────────────────────────────────────────────────────
//
// Follows the app's hard-border / shadow-offset aesthetic:
// - Rectangular track with 2dp border (cardBorder)
// - Offset shadow box behind the thumb (like ShadowBox / CustomButton)
// - Thumb slides with the same tween(120) timing as before
// - On = accentColor2 (green) fill, Off = itemsBg

@Composable
fun CustomSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 20.dp else 0.dp,
        animationSpec = tween(120),
        label = "switchThumb"
    )

    val trackColor = if (checked) lerp(itemsBg, accentColor2, 0.15f)
    else itemsBg

    val thumbColor = if (checked) accentColor2 else text.copy(alpha = 0.2f)

    Box(modifier = Modifier.padding(end = 3.dp, bottom = 3.dp)) {
        // Container shadow
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 3.dp, y = 3.dp)
                .background(cardBorder, RoundedCornerShape(4.dp))
        )
        // Track
        Box(
            modifier = Modifier
                .width(48.dp)
                .height(26.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(trackColor)
                .border(2.dp, cardBorder, RoundedCornerShape(4.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { onCheckedChange(!checked) }
                )
                .padding(4.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            // Thumb
            Box(
                modifier = Modifier
                    .offset(x = thumbOffset)
                    .size(18.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(thumbColor)
                    .border(1.5.dp, cardBorder, RoundedCornerShape(2.dp))
            )
        }
    }
}

// ── BudgetModeSelector ───────────────────────────────────────────────────────
//
// Replaces the pill-shaped segment control with a flat rectangular row
// that matches ShadowBox card aesthetics:
// - Hard 2dp border around the whole container
// - Selected segment gets accentColor1 tinted fill + hard border divider
// - Unselected segment is transparent
// - Shadow offset on the whole container (like other cards)

enum class BudgetMode { Monthly, Daily }

@Composable
fun BudgetModeSelector(
    selected: BudgetMode,
    onSelect: (BudgetMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val typography = adaptiveTypography()

    Box(modifier = modifier.padding(end = 4.dp, bottom = 4.dp)) {

        // Shadow offset
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 4.dp, y = 4.dp)
                .background(cardBorder, RoundedCornerShape(4.dp))
        )

        // Segmented container
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .background(itemsBg)
                .border(2.dp, cardBorder, RoundedCornerShape(4.dp)),
            horizontalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            BudgetMode.entries.forEachIndexed { index, mode ->
                val isSelected = selected == mode

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            if (isSelected) accentColor1.copy(alpha = 0.12f)
                            else Color.Transparent
                        )
                        // Divider between segments via start border on second item
                        .then(
                            if (index > 0) Modifier.border(
                                width = 2.dp,
                                color = cardBorder,
                                shape = RoundedCornerShape(0.dp)  // flat divider
                            ) else Modifier
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onSelect(mode) }
                        )
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = mode.name.uppercase(),
                        style = typography.labelMedium,
                        color = if (isSelected) accentColor1 else text.copy(alpha = 0.4f),
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.ExtraLight,
                        letterSpacing = typography.labelMedium.letterSpacing
                    )
                }
            }
        }
    }
}