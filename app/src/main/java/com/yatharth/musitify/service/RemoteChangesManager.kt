package com.yatharth.musitify.service

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

    suspend fun apply(actions: List<SyncAction>) {
        for(action in actions){
            when(action.type){
                ActionType.SONG_LIKED -> {
                    songRepository.addSongFromSync(
                        songUuid = action.songUuid,
                        payload = action.payload
                    )
                }

                ActionType.SONG_UNLIKED -> {

                }

                else -> {

                }
            }
        }
    }

}