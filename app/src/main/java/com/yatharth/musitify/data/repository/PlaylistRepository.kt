package com.yatharth.musitify.data.repository

import android.content.Context
import android.util.Log
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.yatharth.musitify.data.dao.PlaylistDao
import com.yatharth.musitify.data.dao.SongDao
import com.yatharth.musitify.data.dao.SongToPlaylistDao
import com.yatharth.musitify.data.entity.Playlist
import com.yatharth.musitify.data.entity.Song
import com.yatharth.musitify.data.entity.SongToPlaylist
import com.yatharth.musitify.network.model.NSong
import com.yatharth.musitify.util.ConvertSong
import com.yatharth.musitify.util.SongMedia.cachePlaylistCover
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

@Singleton
class PlaylistRepository @Inject constructor(
    private val songDao: SongDao,
    private val playlistDao: PlaylistDao,
    private val songToPlaylistDao: SongToPlaylistDao,
    private val songRepository: SongRepository,
    private val actionRepository: ActionRepository,
    @ApplicationContext private val context: Context
) {
    private val TAG = "PlaylistRepository"

    suspend fun downloadPlaylist(
        playlistUrl: String,
        onProgress: (completed: Int, total: Int) -> Unit = { _, _ -> }
    ) = withContext(Dispatchers.IO) {
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(context))
        }
        val py = Python.getInstance()
        val module = py.getModule("downloader")

        val result = module.callAttr("get_playlist_urls", playlistUrl)

        val playlistName = result?.callAttr("get", "name")?.toString() ?: "Unknown Playlist"
        val urlsObj = result?.callAttr("get", "urls")
        val urls: List<String> = urlsObj?.asList()?.map { it.toString() } ?: emptyList()

        Log.d(TAG, "Playlist: $playlistName, ${urls.size} tracks")

        val songs = songRepository.downloadSongsFromSync(
            nSongs = urls.map {
                NSong(
                    id = UUID.randomUUID().toString(),
                    url = it,
                    title = "",
                    artist = "",
                    downloaded_at = System.currentTimeMillis()
                )
            },
            onProgress = onProgress
        ).map { item ->
            Pair(item.value.id, item.value)
        }.toMap()

        if (songs.isNotEmpty()) {
            createPlaylistAndAddSongs(playlistName, songs)
            Log.d(TAG, "Created playlist '$playlistName' with ${songs.size} songs")
        } else {
            Log.e(TAG, "No songs downloaded successfully, skipping playlist creation")
        }
    }

    suspend fun createPlaylist(
        name: String,
        uuid: UUID? = null
    ): Playlist = withContext(Dispatchers.IO) {
        val existing = playlistDao.getPlaylistByName(name)

        val playlistId: Int
        if (existing != null) {
            Log.d(TAG, "Playlist '$name' already exists, adding songs to it")
            existing
        } else {
            val playlist = Playlist(
                name = name,
                timeHeard = 0,
                createdAt = System.currentTimeMillis(),
                uuid = uuid ?: UUID.randomUUID()
            )
            val id = playlistDao.addPlaylist(playlist)
            actionRepository.recordPlaylistCreated(
                playlistUuid = playlist.uuid.toString(),
                name = playlist.name,
                createdAt = playlist.createdAt
            )
            playlist.copy(id = id.toInt())
        }
    }

    suspend fun getAllPlaylistsFrom(names: List<String>): List<Playlist> {
        return playlistDao.getAllPlaylistsFrom(names)
    }

    suspend fun generatePlaylistArt(playlistId: Int) = withContext(Dispatchers.IO) {
        val songRelations = playlistDao.getSongsForPlaylistSuspend(playlistId)
        val songs = songDao.getSongsByIds(songRelations.map { it.id })
        val art = getOrCreatePlaylistCover(playlistId, songs)
        playlistDao.updateArt(playlistId, art.path)
    }

    suspend fun createPlaylistAndAddSongs(name: String, songs: Map<Int, Song>) =
        withContext(Dispatchers.IO) {
            val existing = playlistDao.getPlaylistByName(name)

            val playlist: Playlist
            if (existing != null) {
                Log.d(TAG, "Playlist '$name' already exists, adding songs to it")
                playlist = existing
            } else {
                val _playlist = Playlist(
                    name = name,
                    timeHeard = 0,
                    createdAt = System.currentTimeMillis(),
                    uuid = UUID.randomUUID()
                )
                val id = playlistDao.addPlaylist(_playlist)
                playlist = _playlist.copy(id = id.toInt())
            }

            addSongsToPlaylist(playlist, songs)
            generatePlaylistArt(playlist.id)
        }

    fun getOrCreatePlaylistCover(playlistId: Int, songs: List<Song>): File {
        val coversDir = File(context.filesDir, "playlist_covers").apply { mkdirs() }
        val outFile = File(coversDir, "playlist_$playlistId.png")
        if (outFile.exists()) return outFile

        val artPaths = songs.take(min(4, songs.size)).map { it.artPath }
        return cachePlaylistCover(context, playlistId, artPaths)
    }

    suspend fun addSongPlaylistRelations(relations: List<SongToPlaylist>) {
        songToPlaylistDao.addSongsToPlaylist(relations)
    }

    suspend fun addSongsToPlaylist(playlist: Playlist, songs: Map<Int, Song>) {
        // Validate that playlist exists
        val existingPlaylist = playlistDao.getPlaylistByName(playlist.name) ?: run {
            throw IllegalStateException("Playlist with name '$playlist.name' not found in database")
        }

        val validSongs = songs.filter { song ->
            songDao.getSongById(song.key) != null
        }.ifEmpty {
            throw IllegalStateException("No valid songs to add. All referenced songs may have been deleted.")
        }

        if (existingPlaylist.id != playlist.id) {
            // Use existing playlist ID instead of the one from the parameter
            val position = songToPlaylistDao.getLastPosition(existingPlaylist.id) + 1

            songToPlaylistDao.addSongsToPlaylist(
                validSongs.keys.mapIndexed { i, songId ->
                    actionRepository.recordSongAddedToPlaylist(
                        songUuid = songs[songId]?.uuid.toString(),
                        playlistUuid = existingPlaylist.uuid.toString(),
                        position = position + i
                    )

                    SongToPlaylist(
                        songId = songId,
                        playListId = existingPlaylist.id,
                        position = position + i,
                        addedOn = System.currentTimeMillis(),
                        song_uuid = songs[songId]?.uuid,
                        playlist_uuid = existingPlaylist.uuid
                    )
                }
            )
        } else {
            // Playlist is new or parameter was correct
            val position = songToPlaylistDao.getLastPosition(playlist.id) + 1

            songToPlaylistDao.addSongsToPlaylist(
                validSongs.keys.mapIndexed { i, songId ->
                    actionRepository.recordSongAddedToPlaylist(
                        songUuid = songs[songId]?.uuid.toString(),
                        playlistUuid = playlist.uuid.toString(),
                        position = position + i
                    )

                    SongToPlaylist(
                        songId = songId,
                        playListId = playlist.id,
                        position = position + i,
                        addedOn = System.currentTimeMillis(),
                        song_uuid = songs[songId]?.uuid,
                        playlist_uuid = playlist.uuid
                    )
                }
            )
        }
    }


    suspend fun getAllSongRelations(): List<SongToPlaylist> {
        return songToPlaylistDao.getAll()
    }

    val allPlaylists = playlistDao.getAllPlaylists()

    fun songsInPlaylist(playlistId: Int): Flow<List<Song>> {
        return playlistDao.getSongsForPlaylist(playlistId)
    }

    suspend fun deletePlaylist(id: Int) {
        val playlist = playlistDao.getPlaylistById(id) ?: throw IOException("Invalid playlist id")
        actionRepository.recordPlaylistDeleted(playlist.uuid.toString())
        playlistDao.delete(id)
    }

    suspend fun removeSong(songId: Int, playlistId: Int) {
        val song = songDao.getSongById(songId) ?: throw IOException("Invalid song id")
        val playlist =
            playlistDao.getPlaylistById(playlistId) ?: throw IOException("Invalid playlist id")

        actionRepository.recordSongRemovedFromPlaylist(
            song.uuid.toString(),
            playlist.uuid.toString()
        )
        songToPlaylistDao.remove(songId, playlistId)
        Log.d("Temp", "Removed song $songId from $playlistId")
    }
}