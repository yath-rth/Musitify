package com.yatharth.musitify.ui.elements

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yatharth.musitify.ui.theme.accentColor1
import com.yatharth.musitify.ui.theme.cardBorder
import com.yatharth.musitify.ui.theme.secondaryBg
import com.yatharth.musitify.ui.theme.text
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Labelled hard-shadow progress bar (playback progress, stat shares).
 * `max <= 0` (e.g. no song loaded) ⇒ an empty bar, not a full one.
 *
 * Desktop: when [onSeek] is non-null the bar is seekable — click or drag on it;
 * the fill follows the pointer while dragging and [onSeek] is called with the
 * fraction (0..1) on release.
 */
@Composable
fun FillBar(
    modifier: Modifier = Modifier,
    label: String? = null,
    secondaryLabel: String? = "",
    color: Color = accentColor1,
    value: Double,
    max: Double,
    bgColor: Color = secondaryBg,
    onSeek: ((Float) -> Unit)? = null,
) {
    val progress = if (max > 0) (abs(value) / max).coerceIn(0.0, 1.0).toFloat() else 0f

    val animatedProgress = remember { Animatable(progress) }
    var dragging by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val currentOnSeek by rememberUpdatedState(onSeek)

    LaunchedEffect(progress, dragging) {
        if (!dragging) animatedProgress.animateTo(progress, spring(visibilityThreshold = 0.01f))
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (label != null) {
                Text(
                    text = label,
                    color = text,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Normal,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            if (secondaryLabel != null) {
                Text(
                    text = secondaryLabel,
                    color = text.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraLight,
                    letterSpacing = 0.5.sp
                )
            }
        }

        Spacer(modifier = Modifier.padding(1.dp))

        val seekModifier = if (onSeek == null) Modifier else Modifier
            .handCursor()
            .pointerInput(Unit) {
                fun fractionAt(x: Float) =
                    if (size.width > 0) (x / size.width).coerceIn(0f, 1f) else 0f
                awaitEachGesture {
                    val down = awaitFirstDown()
                    down.consume()
                    dragging = true
                    var fraction = fractionAt(down.position.x)
                    scope.launch { animatedProgress.snapTo(fraction) }
                    try {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            change.consume()
                            fraction = fractionAt(change.position.x)
                            scope.launch { animatedProgress.snapTo(fraction) }
                            if (!change.pressed) {
                                currentOnSeek?.invoke(fraction)
                                break
                            }
                        }
                    } finally {
                        dragging = false
                    }
                }
            }

        Box(
            modifier = Modifier
                .height(12.dp)
                .then(seekModifier)
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .offset(x = 2.dp, y = 3.dp)
                    .background(shape = RoundedCornerShape(20), color = cardBorder)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .border(1.5.dp, color = cardBorder, shape = RoundedCornerShape(20))
                        .clip(RoundedCornerShape(20))
                        .background(secondaryBg)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(20))
                            .background(bgColor)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(animatedProgress.value)
                                .background(color = color)
                        )
                    }
                }
            }
        }
    }
}
