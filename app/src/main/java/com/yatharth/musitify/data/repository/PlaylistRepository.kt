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
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

@Singleton
class PlaylistRepository @Inject constructor(
    private val songDao: SongDao,
    private val playlistDao: PlaylistDao,
    private val songToPlaylistDao: SongToPlaylistDao,
    private val convertSong: ConvertSong,
    @ApplicationContext private val context: Context
) {
    private val TAG = "PlaylistRepository"

    suspend fun downloadPlaylist(
        playlistUrl: String,
        outputDir: String? = context.getExternalFilesDir(null)?.absolutePath,
        maxConcurrent: Int = 5,
        onProgress: (completed: Int, total: Int) -> Unit = { _, _ -> }
    ): Int = withContext(Dispatchers.IO) {
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(context))
        }
        val py = Python.getInstance()
        val module = py.getModule("downloader")

        val result = module.callAttr("get_playlist_urls", playlistUrl)

        val playlistName = result?.callAttr("get", "name")?.toString() ?: "Unknown Playlist"
        val urlsObj = result?.callAttr("get", "urls")
        val urls: List<String> = urlsObj?.asList()?.map { it.toString() } ?: emptyList()
        val total = urls.size

        Log.d(TAG, "Playlist: $playlistName, ${urls.size} tracks")

        val completedCount = java.util.concurrent.atomic.AtomicInteger(0)
        val semaphore = Semaphore(maxConcurrent)

        val songs: Map<Int, Song?> = urls.map { url ->
            async(Dispatchers.IO) {
                semaphore.withPermit {
                    try {
                        val downloadResult = module.callAttr("downloadSong", url, outputDir)
                        val songName = convertSong.invoke(downloadResult)
                        val durationSeconds = downloadResult?.callAttr("get", "duration")?.toLong()
                        val artist =
                            downloadResult?.callAttr("get", "artist")?.toString() ?: "unknown"
                        val durationMs = durationSeconds?.times(1000)

                        val song = Song(
                            name = songName,
                            artist = artist,
                            length = durationMs ?: 0,
                            timeHeard = 0,
                            filePath = context.getExternalFilesDir(null)?.absolutePath + File.separator + "$songName.mp3",
                            artPath = context.getExternalFilesDir(null)?.absolutePath + File.separator + "$songName.webp",
                            downloadedAt = System.currentTimeMillis(),
                            url = url,
                            uuid = UUID.randomUUID()
                        )

                        val insertedId = songDao.addSong(song)
                        Pair(insertedId.toInt(), song)
                    } catch (e: Exception) {
                        Log.e(TAG, e.message ?: "Error somewhere")
                        null
                    } finally {
                        val done = completedCount.incrementAndGet()
                        onProgress(done, total)
                    }
                }
            }
        }.awaitAll()
            .filterNotNull()
            .toMap()

        val validSongs = songs.filter { it.value != null }

        if (validSongs.isNotEmpty()) {
            createPlaylistAndAddSongs(playlistName, validSongs as Map<Int, Song>)
            Log.d(TAG, "Created playlist '$playlistName' with ${validSongs.size} songs")
        } else {
            Log.e(TAG, "No songs downloaded successfully, skipping playlist creation")
        }

        completedCount.get()
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
        playlistDao.delete(id)
    }

    suspend fun removeSong(songId: Int, playlistId: Int) {
        songToPlaylistDao.remove(songId, playlistId)
        Log.d("Temp", "Removed song $songId from $playlistId")
    }
}