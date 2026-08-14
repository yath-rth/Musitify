package com.yatharth.musitify.data.migrations

import com.yatharth.musitify.data.dao.PlaylistDao
import com.yatharth.musitify.data.dao.SongDao
import com.yatharth.musitify.data.dao.SongToPlaylistDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.forEach
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IdGenerator @Inject constructor(
    private val songDao: SongDao,
    private val playlistDao: PlaylistDao,
    private val songToPlaylistDao: SongToPlaylistDao,
) {

    suspend fun invoke() = withContext(Dispatchers.IO){
        val songs = songDao.getAllSongsSuspend()
        val playlists = playlistDao.getAllPlaylistsSuspend()
        val relations = songToPlaylistDao.getAll()

        songs.forEach { song ->
            song.uuid = UUID.randomUUID()
        }

        playlists.forEach { playlist ->
            playlist.uuid = UUID.randomUUID()
        }

        val songMap = songs.associateBy { it.id }
        val playlistMap = playlists.associateBy { it.id }

        relations.forEach {
            val playlist = playlistMap[it.playListId]
            val song = songMap[it.songId]

            it.song_uuid = song?.uuid
            it.playlist_uuid = playlist?.uuid
        }

        songDao.addSongs(songs)
        playlistDao.addPlaylists(playlists)
        songToPlaylistDao.addSongsToPlaylist(relations)
    }

}