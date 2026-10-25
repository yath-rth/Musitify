package com.yatharth.musitify.ui.elements

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Desktop replacement for androidx.activity's back dispatcher. The window maps Escape
 * (when no dialog is open) to [dispatch], which runs the most recently registered
 * enabled [BackHandler].
 */
class BackDispatcher {
    internal class Entry(var enabled: Boolean, val onBack: () -> Unit)

    private val entries = mutableListOf<Entry>()

    internal fun add(entry: Entry) {
        entries += entry
    }

    internal fun remove(entry: Entry) {
        entries -= entry
    }

    /** @return true if a handler consumed the back event. */
    fun dispatch(): Boolean {
        val entry = entries.lastOrNull { it.enabled } ?: return false
        entry.onBack()
        return true
    }
}

val LocalBackDispatcher = staticCompositionLocalOf { BackDispatcher() }

/** Same contract as Android's `BackHandler(enabled, onBack)`. */
@Composable
fun BackHandler(enabled: Boolean = true, onBack: () -> Unit) {
    val dispatcher = LocalBackDispatcher.current
    val currentOnBack by rememberUpdatedState(onBack)
    val entry = remember { BackDispatcher.Entry(enabled) { currentOnBack() } }
    SideEffect { entry.enabled = enabled }
    DisposableEffect(dispatcher) {
        dispatcher.add(entry)
        onDispose { dispatcher.remove(entry) }
    }
}
