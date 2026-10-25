package com.yatharth.musitify.util

import android.content.Context
import android.util.Log
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.yatharth.musitify.data.dao.PlaylistDao
import com.yatharth.musitify.data.dao.SettingsDao
import com.yatharth.musitify.data.dao.SongDao
import com.yatharth.musitify.data.dao.SongToPlaylistDao
import com.yatharth.musitify.data.entity.Settings
import com.yatharth.musitify.data.entity.Song
import dagger.hilt.android.qualifiers.ApplicationContext
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
    private val settingsDao: SettingsDao,
    @ApplicationContext private val context: Context
) {

    /**
     * One-time repair of artists stored before the downloader fix: "None" and other
     * missing values are re-read from YouTube, "['A', 'B']" becomes "A, B", and channel
     * noise (" - Topic", VEVO, Official) is stripped. Logic lives in downloader.py's
     * fix_artist.
     *
     * Marked done (setting [ARTIST_FIX_KEY]) only when every song was handled, so songs
     * that failed (e.g. offline) are retried on the next launch. Fixed songs no longer
     * look broken, so a rerun only touches what's left.
     */
    suspend fun fixArtists() = withContext(Dispatchers.IO) {
        if (settingsDao.getSettingByName(ARTIST_FIX_KEY)?.value == "true") return@withContext

        if (!Python.isStarted()) Python.start(AndroidPlatform(context))
        val module = Python.getInstance().getModule("downloader")

        val songs = songDao.getAllSongsSuspend()
        var fixed = 0
        var failed = 0
        songs.forEach { song ->
            try {
                val newArtist = module.callAttr("fix_artist", song.artist, song.url)?.toString()
                if (!newArtist.isNullOrBlank() && newArtist != song.artist) {
                    songDao.updateSong(song.copy(artist = newArtist))
                    Log.d("Legacy", "Artist: ${song.name}: ${song.artist} -> $newArtist")
                    fixed++
                }
            } catch (e: Exception) {
                failed++
                Log.w("Legacy", "Artist fix failed for ${song.name} (${song.url}): ${e.message}")
            }
        }

        Log.d("Legacy", "Artist fix: ${songs.size} songs, $fixed fixed, $failed failed")
        if (failed == 0) {
            settingsDao.updateSetting(
                Settings(name = ARTIST_FIX_KEY, value = "true", updated_on = System.currentTimeMillis())
            )
        }
    }

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

    companion object {
        const val ARTIST_FIX_KEY = "artist_fix_v1"
    }
}