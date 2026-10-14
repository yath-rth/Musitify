package com.yatharth.musitify.service

import android.util.Log
import com.yatharth.musitify.data.entity.ActionType
import com.yatharth.musitify.data.entity.SyncAction
import com.yatharth.musitify.data.repository.PlaylistRepository
import com.yatharth.musitify.data.repository.SongRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteChangesManager @Inject constructor(
    private val songRepository: SongRepository,
    private val playlistRepository: PlaylistRepository
) {

    private val TAG = "RemoteChanges"

    suspend fun apply(actions: List<SyncAction>) {
        for (action in actions) {
            try {
                when (action.type) {
                    ActionType.SONG_LIKED -> {
                        Log.d(TAG, "Adding song from remote")
                        songRepository.addSongFromSync(
                            songUuid = action.songUuid,
                            payload = action.payload
                        )
                    }

                    ActionType.SONG_UNLIKED -> {
                        Log.d(TAG, "Removed song from remote")
                        songRepository.deleteSongByUUID(
                            id = action.songUuid.toString(),
                            record = false
                        )
                    }

                    ActionType.SONG_RENAMED -> {
                        Log.d(TAG, "Song renamed from remote - not yet implemented")
                        // TODO: Implement song rename from remote sync
                    }

                    ActionType.PLAYLIST_CREATED -> {
                        playlistRepository.createPlaylistFromSync(
                            playlistUuid = action.playlistUuid,
                            payload = action.payload
                        )
                    }

                    ActionType.PLAYLIST_DELETED -> {
                        playlistRepository.deletePlaylistByUuid(
                            uuid = action.playlistUuid,
                            record = false
                        )
                    }

                    ActionType.PLAYLIST_RENAMED -> {
                        Log.d(TAG, "Playlist renamed from remote - not yet implemented")
                        // TODO: Implement playlist rename from remote sync
                    }

                    ActionType.SONG_ADDED_TO_PLAYLIST -> {
                        playlistRepository.addSongToPlaylistSync(
                            songUuid = action.songUuid,
                            playlistUuid = action.playlistUuid,
                            payload = action.payload
                        );
                    }

                    ActionType.SONG_REMOVED_FROM_PLAYLIST -> {
                        playlistRepository.removeSongFromPlaylistSync(
                            songUuid = action.songUuid,
                            playlistUuid = action.playlistUuid
                        )
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, e.message.toString())
            }
        }
    }

}