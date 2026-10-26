package com.yatharth.musitify.music

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Plays a YouTube stream for a quick listen before downloading. Separate from the
 * library player; it takes audio focus, so library playback pauses while previewing.
 */
@Singleton
class PreviewPlayer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var player: ExoPlayer? = null

    private val _playingId = MutableStateFlow<String?>(null)
    /** Id of the YouTube result currently loading or playing, if any. */
    val playingId: StateFlow<String?> = _playingId.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun play(id: String, streamUrl: String) {
        val p = player ?: ExoPlayer.Builder(context).build().also {
            it.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true
            )
            it.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_ENDED) stop()
                }

                override fun onPlayerError(e: androidx.media3.common.PlaybackException) {
                    _error.value = "Couldn't play this preview"
                    stop()
                }
            })
            player = it
        }
        _error.value = null
        p.setMediaItem(MediaItem.fromUri(streamUrl))
        p.prepare()
        p.play()
        _playingId.value = id
    }

    /** Marks [id] as loading before its stream URL is known. */
    fun markLoading(id: String) {
        player?.stop()
        _playingId.value = id
    }

    fun stop() {
        player?.stop()
        _playingId.value = null
    }

    fun setError(message: String) {
        _error.value = message
    }

    fun clearError() {
        _error.value = null
    }

    fun release() {
        player?.release()
        player = null
        _playingId.value = null
    }
}
