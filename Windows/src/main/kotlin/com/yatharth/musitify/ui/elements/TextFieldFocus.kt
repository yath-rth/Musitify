package com.yatharth.musitify.ui.elements

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.onFocusChanged

/**
 * Desktop-only: knows whether any text field currently has keyboard focus, so the
 * window-level shortcuts (`handleAppShortcut`) leave Space / arrow keys to the field.
 *
 * Created once in `Main.kt`, provided to the UI through [LocalTextFieldFocusTracker];
 * every text field opts in with [Modifier.trackTextFieldFocus].
 */
class TextFieldFocusTracker {
    private var focusedCount by mutableIntStateOf(0)

    /** True while at least one tracked text field is focused. */
    val isTextFieldFocused: Boolean get() = focusedCount > 0

    internal fun onFocusGained() {
        focusedCount++
    }

    internal fun onFocusLost() {
        focusedCount = (focusedCount - 1).coerceAtLeast(0)
    }
}

val LocalTextFieldFocusTracker = staticCompositionLocalOf { TextFieldFocusTracker() }

/** Reports this text field's focus to [LocalTextFieldFocusTracker] (also on disposal while focused). */
fun Modifier.trackTextFieldFocus(): Modifier = composed {
    val tracker = LocalTextFieldFocusTracker.current
    var focused by remember { mutableStateOf(false) }

    DisposableEffect(tracker) {
        onDispose { if (focused) tracker.onFocusLost() }
    }

    onFocusChanged { state ->
        val now = state.isFocused
        if (now != focused) {
            focused = now
            if (now) tracker.onFocusGained() else tracker.onFocusLost()
        }
    }
}
