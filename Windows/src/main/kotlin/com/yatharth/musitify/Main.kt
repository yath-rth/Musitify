package com.yatharth.musitify

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.yatharth.musitify.data.appModules
import com.yatharth.musitify.data.repository.MediaControllerRepository
import com.yatharth.musitify.engine.AudioPlayer
import com.yatharth.musitify.engine.DownloadQueue
import com.yatharth.musitify.platform.MediaKeys
import com.yatharth.musitify.platform.MusitifyTray
import com.yatharth.musitify.platform.SingleInstance
import com.yatharth.musitify.platform.handleAppShortcut
import com.yatharth.musitify.platform.rememberAppIcon
import com.yatharth.musitify.service.bootstrapApp
import com.yatharth.musitify.ui.App
import com.yatharth.musitify.ui.elements.BackDispatcher
import com.yatharth.musitify.ui.elements.OpenDialogs
import com.yatharth.musitify.ui.elements.TextFieldFocusTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.koin.core.Koin
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import java.awt.Dimension
import kotlin.system.exitProcess

/**
 * Desktop entry point (Android `MusitifyApp` + `MainActivity`):
 * single instance → Koin → bootstrap (first-run id, player, yt-dlp update) → global media
 * keys → tray + window. Closing the window hides it to the tray; tray "Quit" exits.
 */
fun main() {
    if (!SingleInstance.acquire()) exitProcess(0) // the running instance was asked to show itself

    val koin = startKoin { modules(appModules()) }.koin
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    runBlocking {
        try {
            bootstrapApp(koin, appScope)
        } catch (t: Throwable) {
            System.err.println("Musitify: startup bootstrap failed: $t")
        }
    }

    val player = koin.get<AudioPlayer>()
    val mediaKeys = MediaKeys(player).also { it.start() }
    val backDispatcher = BackDispatcher()
    val focusTracker = TextFieldFocusTracker()

    application {
        var visible by remember { mutableStateOf(true) }
        var activations by remember { mutableIntStateOf(0) }
        val windowState = rememberWindowState(width = 1280.dp, height = 860.dp)

        fun show() {
            visible = true
            windowState.isMinimized = false
            activations++
        }

        fun quit() {
            shutdown(koin, mediaKeys, appScope)
            exitApplication()
        }

        LaunchedEffect(Unit) {
            SingleInstance.activations.collect { show() }
        }

        MusitifyTray(
            player = player,
            onOpen = ::show,
            onQuit = ::quit,
        )

        Window(
            onCloseRequest = { visible = false }, // hide to tray instead of exiting
            visible = visible,
            state = windowState,
            title = "Musitify",
            icon = rememberAppIcon(),
            onPreviewKeyEvent = { event ->
                if (event.key == Key.Escape) {
                    // An open dialog owns Escape (dismiss); otherwise Escape = Android back.
                    !OpenDialogs.isAnyOpen && event.type == KeyEventType.KeyDown && backDispatcher.dispatch()
                } else {
                    handleAppShortcut(event, player, focusTracker.isTextFieldFocused)
                }
            },
        ) {
            LaunchedEffect(Unit) { window.minimumSize = Dimension(900, 650) }
            LaunchedEffect(visible, activations) {
                if (visible) window.toFront()
            }
            App(backDispatcher = backDispatcher, focusTracker = focusTracker)
        }
    }

    // application {} returns after exitApplication(); JavaFX / hook threads must not keep us alive.
    exitProcess(0)
}

/** Stops media keys, flushes listen time and releases the player, cancels downloads, closes Koin. */
private fun shutdown(koin: Koin, mediaKeys: MediaKeys, appScope: CoroutineScope) {
    runCatching { mediaKeys.stop() }
    runCatching { koin.get<MediaControllerRepository>().release() }
    runCatching { koin.get<DownloadQueue>().shutdown() }
    // Give the last listen-time write a moment to land before the process goes away.
    runCatching { Thread.sleep(300) }
    appScope.cancel()
    runCatching { SingleInstance.release() }
    runCatching { stopKoin() }
    runCatching { javafx.application.Platform.exit() }
}
