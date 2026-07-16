package com.yatharth.musitify.data.repository

import android.content.Context
import android.util.Log
import com.chaquo.python.Python
import com.yatharth.musitify.data.dao.SongDao
import com.yatharth.musitify.data.entity.Song
import com.yatharth.musitify.util.ConvertSong
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaylistRepository @Inject constructor(
    private val songDao: SongDao,
    private val convertSong: ConvertSong,
    @ApplicationContext private val applicationContext: Context
) {
    private val TAG = "PlaylistRepository"

    suspend fun downloadPlaylist(
        playlistUrl: String,
        outputDir: String?,
        maxConcurrent: Int = 5
    ) = coroutineScope {
        val python = Python.getInstance()
        val module = python.getModule("downloader")

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

}