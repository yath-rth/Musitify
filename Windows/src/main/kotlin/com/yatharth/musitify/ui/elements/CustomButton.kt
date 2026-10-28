package com.yatharth.musitify.ui.elements

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.yatharth.musitify.ui.theme.BUTTON_ANIMATION_MS
import com.yatharth.musitify.ui.theme.cardBorder
import com.yatharth.musitify.ui.theme.itemsBg

/** Desktop-only: subtle darkening of the button face while hovered. */
private val HoverTint = cardBorder.copy(alpha = 0.06f)

private fun Modifier.hoverTint(hovered: Boolean): Modifier = drawWithContent {
    drawContent()
    if (hovered) drawRect(HoverTint)
}

/**
 * Rectangular press-offset button: card at (0,0), hard shadow at (+3,+3);
 * pressing slides the card +2/+2 into the shadow (20 ms). Long press — or right
 * click on desktop — calls [onLongClick].
 */
@Composable
fun CustomButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    bgColor: Color = itemsBg,
    content: @Composable () -> Unit = {},
) {
    var pressed by remember { mutableStateOf(false) }
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val currentOnClick by rememberUpdatedState(onClick)
    val currentOnLongClick by rememberUpdatedState(onLongClick)

    val offset by animateDpAsState(
        targetValue = if (pressed) 2.dp else 0.dp,
        animationSpec = tween(BUTTON_ANIMATION_MS),
        label = ""
    )

    Box(
        modifier = modifier
            .wrapContentSize()
            .animateContentSize()
            .padding(4.dp)
            .hoverable(interaction)
            .handCursor()
            .pointerInput(Unit) {
                detectButtonGestures(
                    onPressedChange = { pressed = it },
                    onTap = { currentOnClick() },
                    onLongPress = { currentOnLongClick() }
                )
            },
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 3.dp, y = 3.dp)
                .background(shape = RoundedCornerShape(4.dp), color = cardBorder)
        )

        Card(
            Modifier
                .wrapContentWidth()
                .wrapContentHeight()
                .offset(x = offset, y = offset),
            colors = CardDefaults.cardColors(containerColor = itemsBg),
            shape = RoundedCornerShape(4.dp),
            border = BorderStroke(1.5.dp, color = cardBorder),
        ) {
            Box(
                modifier = Modifier
                    .wrapContentSize()
                    .background(color = bgColor)
                    .hoverTint(hovered)
            ) { content() }
        }
    }
}

/**
 * Icon button (name kept from when it was round): 8 dp corners, 1.5 dp border, 3/3 hard
 * shadow, pressed offset 2 dp. [selected] fills it with ink for toggles that are on.
 * Hit area includes the 4 dp outer padding (as on Android).
 */
@Composable
fun CustomCircularButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    bgColor: Color = itemsBg,
    // Filled ink background for toggles that are on (repeat, shuffle, sleep).
    // Callers should tint their icon itemsBg when this is true.
    selected: Boolean = false,
    content: @Composable () -> Unit = {},
) {
    var pressed by remember { mutableStateOf(false) }
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val currentOnClick by rememberUpdatedState(onClick)
    val currentOnLongClick by rememberUpdatedState(onLongClick)

    val offset by animateDpAsState(
        targetValue = if (pressed) 2.dp else 0.dp,
        animationSpec = tween(BUTTON_ANIMATION_MS),
        label = ""
    )

    Box(
        modifier = modifier
            .wrapContentSize()
            .animateContentSize()
            .hoverable(interaction)
            .handCursor()
            .pointerInput(Unit) {
                detectButtonGestures(
                    onPressedChange = { pressed = it },
                    onTap = { currentOnClick() },
                    onLongPress = { currentOnLongClick() }
                )
            }
            .padding(4.dp),
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 3.dp, y = 3.dp)
                .background(shape = RoundedCornerShape(8.dp), color = cardBorder)
        )

        Card(
            Modifier
                .wrapContentWidth()
                .wrapContentHeight()
                .offset(x = offset, y = offset),
            colors = CardDefaults.cardColors(containerColor = itemsBg),
            // Tracknix buttons use minimal rounding (4-10dp), not circles.
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.5.dp, color = cardBorder),
        ) {
            Box(
                modifier = Modifier
                    .wrapContentSize()
                    .background(color = if (selected) cardBorder else bgColor)
                    .hoverTint(hovered),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier.padding(4.dp),
                    contentAlignment = Alignment.Center
                ) { content() }
            }
        }
    }
}
