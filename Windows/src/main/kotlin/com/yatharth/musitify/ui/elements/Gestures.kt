package com.yatharth.musitify.ui.elements

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput

/** Hand cursor for anything clickable. */
fun Modifier.handCursor(): Modifier = pointerHoverIcon(PointerIcon.Hand)

/**
 * Fires [action] on a secondary (right) mouse click or on a primary long press
 * (held for the platform long-press timeout). Primary taps are NOT consumed, so
 * this can be stacked with `clickable`/`combinedClickable`; the release after a
 * long press IS consumed so the tap handler does not also fire.
 */
fun Modifier.onRightClickOrLongPress(
    enabled: Boolean = true,
    action: () -> Unit,
): Modifier = if (!enabled) this else composed {
    val currentAction by rememberUpdatedState(action)
    pointerInput(Unit) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            if (currentEvent.buttons.isSecondaryPressed) {
                down.consume()
                currentAction()
                consumeUntilUp()
                return@awaitEachGesture
            }
            val released = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                waitForUpOrCancellation()
                true
            }
            if (released == null) {
                currentAction()
                consumeUntilUp()
            }
        }
    }
}

/**
 * Press/tap/long-press detector used by the press-offset buttons. Mirrors
 * Android's `detectTapGestures(onPress, onTap, onLongPress)` and additionally
 * maps a right click to [onLongPress].
 */
internal suspend fun PointerInputScope.detectButtonGestures(
    onPressedChange: (Boolean) -> Unit,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
) {
    awaitEachGesture {
        val down = awaitFirstDown()
        down.consume()
        if (currentEvent.buttons.isSecondaryPressed) {
            onLongPress()
            consumeUntilUp()
            return@awaitEachGesture
        }
        onPressedChange(true)
        try {
            var finished = false
            var up: PointerInputChange? = null
            withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                up = waitForUpOrCancellation()
                finished = true
            }
            if (finished) {
                // Released inside bounds ⇒ tap; cancelled (dragged out) ⇒ nothing.
                up?.let {
                    it.consume()
                    onTap()
                }
            } else {
                onLongPress()
                consumeUntilUp()
            }
        } finally {
            onPressedChange(false)
        }
    }
}

private suspend fun AwaitPointerEventScope.consumeUntilUp() {
    while (true) {
        val event = awaitPointerEvent()
        event.changes.forEach { it.consume() }
        if (event.changes.none { it.pressed }) break
    }
}
