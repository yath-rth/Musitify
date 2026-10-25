package com.yatharth.musitify.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.Tray
import com.yatharth.musitify.engine.AudioPlayer
import javax.imageio.ImageIO

private const val ICON_RESOURCE = "icons/musitify.png"

/** Windows truncates tray tooltips at 127 chars. */
private const val MAX_TOOLTIP = 120

/** Loads the 256px app icon from resources (`icons/musitify.png`). Also usable as the Window icon. */
fun loadAppIconBitmap(): ImageBitmap {
    val stream = Thread.currentThread().contextClassLoader?.getResourceAsStream(ICON_RESOURCE)
        ?: AudioPlayer::class.java.classLoader.getResourceAsStream(ICON_RESOURCE)
        ?: error("Missing resource $ICON_RESOURCE")
    return stream.use { ImageIO.read(it) }.toComposeImageBitmap()
}

/** Remembered [Painter] of the app icon, for `Window(icon = ...)` and the tray. */
@Composable
fun rememberAppIcon(): Painter = remember { BitmapPainter(loadAppIconBitmap()) }

/**
 * System-tray icon with playback controls.
 *
 * Tooltip shows "Title — Artist" of the current track (or "Musitify"). Left/double click
 * ([onAction]) and the "Open" item call [onOpen]; "Quit" calls [onQuit].
 *
 * Intended `Main.kt` wiring — closing the window only hides it, the tray brings it back,
 * and Quit is the real exit:
 * ```
 * fun main() = application {
 *     val player: AudioPlayer = koinInject() // or however the player is obtained
 *     var visible by remember { mutableStateOf(true) }
 *     val windowState = rememberWindowState()
 *
 *     MusitifyTray(
 *         player = player,
 *         onOpen = {
 *             visible = true
 *             windowState.isMinimized = false
 *         },
 *         onQuit = {
 *             player.shutdown()
 *             exitApplication()
 *         },
 *     )
 *
 *     Window(
 *         onCloseRequest = { visible = false }, // hide to tray instead of exiting
 *         visible = visible,
 *         state = windowState,
 *         title = "Musitify",
 *         icon = rememberAppIcon(),
 *     ) {
 *         // Bring to front whenever the tray re-opens us
 *         LaunchedEffect(visible) { if (visible) window.toFront() }
 *         App()
 *     }
 * }
 * ```
 */
@Composable
fun ApplicationScope.MusitifyTray(
    player: AudioPlayer,
    onOpen: () -> Unit,
    onQuit: () -> Unit,
) {
    val state by player.state.collectAsState()
    val icon = rememberAppIcon()
    val track = state.currentTrack
    val tooltip = if (track == null) "Musitify" else {
        val full = if (track.artist.isBlank()) track.title else "${track.title} — ${track.artist}"
        if (full.length > MAX_TOOLTIP) full.take(MAX_TOOLTIP - 1) + "…" else full
    }
    val hasTrack = track != null

    Tray(
        icon = icon,
        tooltip = tooltip,
        onAction = onOpen,
        menu = {
            Item("Open Musitify", onClick = onOpen)
            Separator()
            Item(if (state.isPlaying) "Pause" else "Play", enabled = hasTrack, onClick = { player.toggle() })
            Item("Next", enabled = hasTrack, onClick = { player.next() })
            Item("Previous", enabled = hasTrack, onClick = { player.previous() })
            Separator()
            Item("Quit", onClick = onQuit)
        },
    )
}
