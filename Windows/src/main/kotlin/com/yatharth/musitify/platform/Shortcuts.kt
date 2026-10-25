package com.yatharth.musitify.platform

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import com.yatharth.musitify.engine.AudioPlayer

/** Seek step for the plain arrow keys. */
const val SHORTCUT_SEEK_STEP_MS = 5_000L

/**
 * In-window keyboard shortcuts. Wire it as the Window's `onPreviewKeyEvent`:
 * ```
 * Window(onPreviewKeyEvent = { handleAppShortcut(it, player, textFieldFocused) }, ...)
 * ```
 * where `textFieldFocused` is tracked by the UI (e.g. `Modifier.onFocusChanged` on search fields).
 *
 * | Key            | Action                          |
 * |----------------|---------------------------------|
 * | Space          | Play / pause                    |
 * | Ctrl + Right   | Next track                      |
 * | Ctrl + Left    | Previous track                  |
 * | Right          | Seek forward 5 s                |
 * | Left           | Seek back 5 s                   |
 * | Ctrl + R       | Cycle repeat (OFF → ALL → ONE)  |
 *
 * While a text field is focused, Space and the arrow keys are left alone so typing and
 * cursor movement work; Ctrl+R still cycles repeat. Combinations involving Alt, Shift or
 * Meta are ignored. The action fires on key-down; the matching key-up is
 * also consumed so a focused button doesn't additionally react to Space.
 *
 * @return true if the event was consumed.
 */
fun handleAppShortcut(event: KeyEvent, player: AudioPlayer, textFieldFocused: Boolean = false): Boolean {
    if (event.isAltPressed || event.isMetaPressed || event.isShiftPressed) return false
    val down = event.type == KeyEventType.KeyDown
    if (!down && event.type != KeyEventType.KeyUp) return false
    val ctrl = event.isCtrlPressed

    val action: (() -> Unit)? = when (event.key) {
        Key.Spacebar -> if (!ctrl && !textFieldFocused) ({ player.toggle() }) else null
        Key.DirectionRight -> when {
            textFieldFocused -> null
            ctrl -> ({ player.next() })
            else -> ({ seekBy(player, SHORTCUT_SEEK_STEP_MS) })
        }
        Key.DirectionLeft -> when {
            textFieldFocused -> null
            ctrl -> ({ player.previous() })
            else -> ({ seekBy(player, -SHORTCUT_SEEK_STEP_MS) })
        }
        Key.R -> if (ctrl) ({ player.cycleRepeatMode() }) else null
        else -> null
    }
    if (action == null) return false
    if (down) action()
    return true
}

private fun seekBy(player: AudioPlayer, deltaMs: Long) {
    val s = player.state.value
    if (s.currentTrack == null) return
    val max = if (s.durationMs > 0) s.durationMs else Long.MAX_VALUE
    player.seekTo((s.positionMs + deltaMs).coerceIn(0, max))
}
