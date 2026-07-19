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
import com.yatharth.musitify.util.SongMedia.getAudioDurationMs
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File
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

        val urlsResult = module.callAttr("get_playlist_urls", playlistUrl)
        val urls: List<String> = urlsResult.asList().map { it.toString() }
        val total = urls.size

        val completedCount = java.util.concurrent.atomic.AtomicInteger(0)
        val semaphore = Semaphore(maxConcurrent)

        urls.map { url ->
            async(Dispatchers.IO) {
                semaphore.withPermit {
                    try {
                        val result = module.callAttr("downloadSong", url, outputDir)
                        val songName = convertSong.invoke(result)
                        val durationSeconds = result["duration"]?.toLong()
                        val durationMs = durationSeconds?.times(1000)

                        songDao.addSong(
                            Song(
                                name = songName,
                                length = durationMs ?: 0,
                                timeHeard = 0,
                                filePath = context.getExternalFilesDir(null)?.absolutePath + File.separator + "$songName.mp3",
                                artPath = context.getExternalFilesDir(null)?.absolutePath + File.separator + "$songName.webp",
                                downloadedAt = System.currentTimeMillis(),
                            )
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, e.message ?: "Error somewhere")
                    } finally {
                        val done = completedCount.incrementAndGet()
                        onProgress(done, total)
                    }
                }
            }
        }.awaitAll()

        completedCount.get()
    }

    suspend fun createPlaylist(name: String, songIds: List<Int>) = withContext(Dispatchers.IO) {
        val playlist = Playlist(
            name = name,
            timeHeard = 0,
            createdAt = System.currentTimeMillis()
        )
        val id = playlistDao.addPlaylist(playlist)

        addSongsToPlaylist(id.toInt(), songIds)

        val songs = songDao.getSongsByIds(songIds) // however you fetch songs by id list
        val art = getOrCreatePlaylistCover(id.toInt(), songs)
        playlistDao.updatePlaylist(playlist.copy(id = id.toInt(), artPath = art.path))
    }

    fun getOrCreatePlaylistCover(playlistId: Int, songs: List<Song>): File {
        val coversDir = File(context.filesDir, "playlist_covers").apply { mkdirs() }
        val outFile = File(coversDir, "playlist_$playlistId.png")
        if (outFile.exists()) return outFile

        val artPaths = songs.take(min(4, songs.size)).map { it.artPath }
        return cachePlaylistCover(context, playlistId, artPaths)
    }

    suspend fun addSongToPlaylist(songId: Int, playlistId: Int) {
        val startPosition = songToPlaylistDao.getLastPosition(playlistId) + 1

        songToPlaylistDao.addSongToPlaylist(
            SongToPlaylist(
                songId = songId,
                playListId = playlistId,
                position = startPosition,
                addedOn = System.currentTimeMillis()
            )
        )
    }

    suspend fun addSongsToPlaylist(playlistId: Int, songIds: List<Int>) {
        val position = songToPlaylistDao.getLastPosition(playlistId) + 1

        songToPlaylistDao.addSongsToPlaylist(
            songIds.mapIndexed { index, i ->
                SongToPlaylist(
                    songId = i,
                    playListId = playlistId,
                    position = position + index,
                    addedOn = System.currentTimeMillis()
                )
            }
        )
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
    }
}