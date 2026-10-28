package com.yatharth.musitify.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Tracknix page background: [bg] with 1.5dp dots of [text] at 10% alpha on a
 * 20dp grid. Only for page-level surfaces (scaffold, bars, full-screen
 * sheets); cards and controls stay solid itemsBg so they sit on top of it.
 *
 * Dots are placed on a grid anchored to the window, not to this surface, so
 * the header, the page and the bottom bar form one continuous grid and the
 * grid stays put while content scrolls underneath.
 */
fun Modifier.dotGridBackground(
    bg: Color = background,
    dot: Color = text.copy(alpha = 0.10f),
    spacing: Dp = 20.dp,
    radius: Dp = 1.5.dp,
): Modifier = composed {
    var origin by remember { mutableStateOf(Offset.Zero) }
    this
        .onGloballyPositioned { origin = it.positionInWindow() }
        .drawBehind {
            drawRect(bg)
            val step = spacing.toPx()
            val r = radius.toPx()
            val startX = (step / 2 - origin.x).mod(step)
            var y = (step / 2 - origin.y).mod(step)
            while (y < size.height) {
                var x = startX
                while (x < size.width) {
                    drawCircle(dot, r, Offset(x, y))
                    x += step
                }
                y += step
            }
        }
}
