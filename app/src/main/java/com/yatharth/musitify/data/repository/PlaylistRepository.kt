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
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

//TODO: implement a fetch all playlists songs
//TODO: Populate the queue with only songs from this playlist

@Singleton
class PlaylistRepository @Inject constructor(
    private val songDao: SongDao,
    private val playlistDao: PlaylistDao,
    private val songToPlaylistDao: SongToPlaylistDao,
    private val convertSong: ConvertSong,
    @ApplicationContext private val applicationContext: Context
) {
    private val TAG = "PlaylistRepository"

    suspend fun downloadPlaylist(
        playlistUrl: String,
        outputDir: String? = applicationContext.getExternalFilesDir(null)?.absolutePath,
        maxConcurrent: Int = 5
    ) = withContext(Dispatchers.IO) {
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(applicationContext))
        }
        val py = Python.getInstance()
        val module = py.getModule("downloader")
        Log.d(TAG, module.toString())

        val urlsResult = module.callAttr("get_playlist_urls", playlistUrl)
        val urls: List<String> = urlsResult.asList().map { it.toString() }

        val semaphore = Semaphore(maxConcurrent)

        urls.map { url ->
            async(Dispatchers.IO) {
                semaphore.withPermit {
                    try {
                        val result = module.callAttr(
                            "downloadSong",
                            url,
                            outputDir
                        )
                        val songName = convertSong.invoke(result)
                        songDao.addSong(
                            Song(
                                name = songName,
                                length = 60, //TODO: Implement a way to provide meta-data from yt-dlp to kotlin
                                timeHeard = 0,
                                filePath = applicationContext.getExternalFilesDir(null)?.absolutePath + File.separator + "$songName.mp3",
                                downloadedAt = System.currentTimeMillis(),
                            )
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, e.message ?: "Error somewhere")
                    }
                }
            }
        }.awaitAll()
    }

    suspend fun createPlaylist(name: String, songIds: List<Int>) {
        val id = playlistDao.addPlaylist(
            Playlist(
                name = name,
                timeHeard = 0,
                createdAt = System.currentTimeMillis()
            )
        )

        addSongsToPlaylist(id.toInt(), songIds)
    }

    suspend fun addSongToPlaylist(songId: Int, playlistId: Int) {
        songToPlaylistDao.addSongToPlaylist(
            SongToPlaylist(
                songId = songId,
                playListId = playlistId,
                addedOn = System.currentTimeMillis()
            )
        )
    }

    suspend fun addSongsToPlaylist(playlistId: Int, songIds: List<Int>) {
        songToPlaylistDao.addSongsToPlaylist(
            songIds.map {
                SongToPlaylist(
                    songId = it,
                    playListId = playlistId,
                    addedOn = System.currentTimeMillis()
                )
            }
        )
    }

    val allPlaylists = playlistDao.getAllPlaylists()
}