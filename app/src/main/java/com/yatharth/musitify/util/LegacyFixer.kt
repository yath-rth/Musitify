package com.yatharth.musitify.util

import android.util.Log
import com.yatharth.musitify.data.dao.PlaylistDao
import com.yatharth.musitify.data.dao.SettingsDao
import com.yatharth.musitify.data.dao.SongDao
import com.yatharth.musitify.data.dao.SongToPlaylistDao
import com.yatharth.musitify.data.entity.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LegacyFixer @Inject constructor(
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

    suspend fun updateAllMusicNames() = withContext(Dispatchers.IO) {

        val songs = songDao.getAllSongsSuspend()

        Log.d("Legacy", "Updating ${songs.size} songs")

        val newSongs = songs.mapIndexed { index, song ->

            val normalized = SongNormalizer.normalize(song.name)

            Log.d(
                "Legacy",
                "${index + 1}. Updating ${song.name} → ${normalized.normalizedTitle}"
            )

            song.copy(
                name = normalized.normalizedTitle
            )
        }

        val groupedSongs = newSongs.groupBy { it.name }

        val duplicateSongs = groupedSongs.filterValues { it.size > 1 }

        val uniqueSongs = groupedSongs
            .filterValues { it.size == 1 }
            .values
            .flatten()

        Log.d("Legacy", "Duplicates: $duplicateSongs")

        val additionalSongs = duplicateSongs.mapNotNull { (_, songs) ->

            val minId = songs.minOf { it.id }

            val filtered = songs
                .filter { it.id != minId }
                .filter { !it.url.contains("legacy") }

            val url = filtered.firstOrNull()?.url

            if (url != null) {
                songs
                    .find { it.id == minId }!!
                    .copy(url = url)
            } else {
                null
            }
        }

        Log.d("Legacy", "Additional songs: $additionalSongs")

        // IMPORTANT: UPDATE, don't REPLACE
        (uniqueSongs + additionalSongs).forEach { song ->
            songDao.updateSong(song)
        }
    }
}