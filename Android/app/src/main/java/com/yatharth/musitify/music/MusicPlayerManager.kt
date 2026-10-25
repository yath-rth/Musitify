package com.yatharth.musitify.music

import android.app.PendingIntent
import androidx.media3.session.MediaSession
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSessionService
import dagger.hilt.android.AndroidEntryPoint

//TODO: Modify position and songs in the queue
//TODO: Count the time for each song and playlist as they are played ( an approx number works here)
@AndroidEntryPoint
class MusicPlayerManager : MediaSessionService() {
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this).build()

        val sessionActivityIntent = packageManager
            .getLaunchIntentForPackage(packageName)
            ?.let { intent ->
                PendingIntent.getActivity(
                    this, 0, intent,
                    PendingIntent.FLAG_IMMUTABLE
                )
            }

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivityIntent!!)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        mediaSession?.player?.release()
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()
    }
}