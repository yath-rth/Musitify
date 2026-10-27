package com.yatharth.musitify.engine

import javafx.scene.media.Media
import javafx.scene.media.MediaPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Plays a YouTube stream for a quick listen before downloading (Android `PreviewPlayer`).
 * Separate from the library player; [onStart] runs when a preview starts so the library
 * pauses (Android gets this from audio focus).
 */
class PreviewPlayer(
    private val onStart: () -> Unit = {},
) {
    // FX-thread confined.
    private var player: MediaPlayer? = null

    private val _playingId = MutableStateFlow<String?>(null)

    /** Id of the YouTube result currently loading or playing, if any. */
    val playingId: StateFlow<String?> = _playingId.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun play(id: String, streamUrl: String) {
        _error.value = null
        _playingId.value = id
        onStart()
        FxToolkit.runOnFx {
            releasePlayer()
            // A newer preview (or stop) replaced this one while the URL was resolving.
            if (_playingId.value != id) return@runOnFx
            val mp = try {
                MediaPlayer(Media(streamUrl))
            } catch (e: Exception) {
                fail()
                return@runOnFx
            }
            player = mp
            mp.onEndOfMedia = Runnable { if (player === mp) stop() }
            mp.onError = Runnable { if (player === mp) fail() }
            mp.play()
        }
    }

    /** Marks [id] as loading before its stream URL is known. */
    fun markLoading(id: String) {
        FxToolkit.runOnFx { releasePlayer() }
        _playingId.value = id
    }

    fun stop() {
        _playingId.value = null
        FxToolkit.runOnFx { releasePlayer() }
    }

    fun setError(message: String) {
        _error.value = message
    }

    fun clearError() {
        _error.value = null
    }

    private fun fail() {
        _error.value = "Couldn't play this preview"
        stop()
    }

    private fun releasePlayer() {
        player?.let {
            it.onEndOfMedia = null
            it.onError = null
            runCatching { it.stop() }
            runCatching { it.dispose() }
        }
        player = null
    }
}
