package com.yatharth.musitify.service

import com.yatharth.musitify.data.dao.PlaylistDao
import com.yatharth.musitify.data.dao.SettingsDao
import com.yatharth.musitify.data.dao.SongDao
import com.yatharth.musitify.data.dao.SongToPlaylistDao
import com.yatharth.musitify.data.entity.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IdManager @Inject constructor(
    private val songDao: SongDao,
    private val playlistDao: PlaylistDao,
    private val songToPlaylistDao: SongToPlaylistDao,
    private val settingsDao: SettingsDao
) {

    suspend fun generateUUIDForLibrary() = withContext(Dispatchers.IO) {
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

        relations.forEach { relation ->
            val playlist = playlistMap[relation.playListId] ?: run {
                // Remove orphaned relations where playlist was deleted
                songToPlaylistDao.remove(relation.songId, relation.playListId)
                return@forEach
            }

            val song = songMap[relation.songId] ?: run {
                // Remove orphaned relations where song was deleted
                songToPlaylistDao.remove(relation.songId, relation.playListId)
                return@forEach
            }

            relation.song_uuid = song.uuid
            relation.playlist_uuid = playlist.uuid
        }

        songDao.addSongs(songs)
        playlistDao.addPlaylists(playlists)
        // Only add relations that passed validation above
        val validRelations = relations.filter { relation ->
            playlistMap[relation.playListId] != null && songMap[relation.songId] != null
        }
        if (validRelations.isNotEmpty()) {
            songToPlaylistDao.addSongsToPlaylist(validRelations)
        }
    }

    suspend fun generateDeviceId() {
        val id = UUID.randomUUID()
        settingsDao.updateSetting(
            Settings(
                name = "device_id",
                value = id.toString(),
                updated_on = System.currentTimeMillis()
            )
        )
    }
}