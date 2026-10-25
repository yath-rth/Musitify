package com.yatharth.musitify.platform

import com.github.kwhat.jnativehook.GlobalScreen
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener
import com.yatharth.musitify.engine.AudioPlayer
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Global (system-wide) media keys via jnativehook:
 * Play/Pause → [AudioPlayer.toggle], Next → [AudioPlayer.next],
 * Previous → [AudioPlayer.previous], Stop → [AudioPlayer.stop].
 *
 * Failures (native library can't load, hook can't register) are logged and swallowed —
 * the app keeps running without global media keys. Call [stop] on exit so the native
 * hook thread doesn't keep the JVM alive.
 */
class MediaKeys(private val player: AudioPlayer) {

    @Volatile
    private var running = false

    private val listener = object : NativeKeyListener {
        override fun nativeKeyPressed(e: NativeKeyEvent) {
            try {
                when (e.keyCode) {
                    NativeKeyEvent.VC_MEDIA_PLAY -> player.toggle()
                    NativeKeyEvent.VC_MEDIA_NEXT -> player.next()
                    NativeKeyEvent.VC_MEDIA_PREVIOUS -> player.previous()
                    NativeKeyEvent.VC_MEDIA_STOP -> player.stop()
                }
            } catch (t: Throwable) {
                System.err.println("Musitify: media key handling failed: $t")
            }
        }
    }

    /** Registers the native hook. Safe to call more than once; never throws. */
    @Synchronized
    fun start() {
        if (running) return
        silenceLogging()
        try {
            if (!GlobalScreen.isNativeHookRegistered()) GlobalScreen.registerNativeHook()
            GlobalScreen.addNativeKeyListener(listener)
            running = true
        } catch (t: Throwable) { // NativeHookException, UnsatisfiedLinkError, ...
            System.err.println("Musitify: global media keys unavailable: $t")
        }
    }

    /** Removes the listener and unregisters the native hook. Never throws. */
    @Synchronized
    fun stop() {
        if (!running) return
        running = false
        runCatching { GlobalScreen.removeNativeKeyListener(listener) }
        try {
            if (GlobalScreen.isNativeHookRegistered()) GlobalScreen.unregisterNativeHook()
        } catch (t: Throwable) {
            System.err.println("Musitify: failed to unregister media key hook: $t")
        }
    }

    private companion object {
        /** Strong ref so the configured logger isn't GC'd and reset. */
        private val hookLogger: Logger = Logger.getLogger(GlobalScreen::class.java.packageName)

        fun silenceLogging() {
            hookLogger.level = Level.OFF
            hookLogger.useParentHandlers = false
        }
    }
}
