package com.yatharth.musitify.network

import android.util.Log
import com.yatharth.musitify.data.repository.PlaylistRepository
import com.yatharth.musitify.data.repository.SettingsRepository
import com.yatharth.musitify.data.repository.SongRepository
import com.yatharth.musitify.network.model.MPlaylist_Song
import com.yatharth.musitify.network.model.NPlaylist
import com.yatharth.musitify.network.model.NSong
import com.yatharth.musitify.network.model.SyncLibraryPayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.UUID

@Singleton
class SyncController @Inject constructor(
    val songRepository: SongRepository,
    val playlistRepository: PlaylistRepository,
    val settingsRepository: SettingsRepository
) {
    private val TAG = "SyncController"
    private val client = OkHttpClient()
    private val URL = "http://10.133.82.9:8080/sync"
    val JSON: MediaType? = "application/json; charset=utf-8".toMediaTypeOrNull()

    suspend fun syncData() {
        Log.d(TAG, "Syncing data...")
        val device_id : String = settingsRepository.getSettingByName("device_id")?.value ?: run {
            val id = UUID.randomUUID().toString()
            settingsRepository.insertSetting("device_id", id)
            id
        }

        val songs = songRepository.allSongs
        val songList = songs.first().map {
            NSong(
                id = it.uuid.toString(),
                title = it.name,
                artist = it.artist,
                url = it.url,
                downloaded_at = it.downloadedAt
            )
        }

        val playlists = playlistRepository.allPlaylists.first().map {
            NPlaylist(
                id = it.playlist.uuid.toString(),
                name = it.playlist.name,
                created_on = it.playlist.createdAt
            )
        }

        val user_playlists = playlistRepository.getAllSongRelations().map {
            MPlaylist_Song(
                song_id = it.song_uuid.toString(),
                playlist_id = it.playlist_uuid.toString(),
                added_on = it.addedOn
            )
        }

        val syncPayload = SyncLibraryPayload(
            device_id = device_id,
            songs = songList,
            playlists = playlists,
            playlist_songs = user_playlists,
        )


        val request = Request.Builder()
            .url("$URL/bootstrap")
            .post(Json.encodeToString(syncPayload).toRequestBody(JSON))
            .build()

        try {
            withContext(Dispatchers.IO) {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IOException("Sync failed: ${response.code} ${response.body}")
                    }

                    val responseBody = response.body.string()
                    println("Server response: $responseBody")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, e.message.toString())
        }
    }

}