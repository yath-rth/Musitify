package com.yatharth.musitify.ui.elements

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.yatharth.musitify.ui.theme.accentColor2
import com.yatharth.musitify.ui.theme.cardBorder
import com.yatharth.musitify.ui.theme.itemsBg
import com.yatharth.musitify.ui.theme.text

/**
 * Hard-border rectangular switch: 48×26 track, 18 dp thumb sliding 20 dp
 * (tween 120), 3/3 shadow. On = green thumb on a 15 % green-tinted track.
 */
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

    val trackColor = if (checked) lerp(itemsBg, accentColor2, 0.15f) else itemsBg
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
                .handCursor()
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
