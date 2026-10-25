package com.yatharth.musitify.network

import com.yatharth.musitify.data.dao.PlaylistDao
import com.yatharth.musitify.data.entity.SongToPlaylist
import com.yatharth.musitify.data.repository.PlaylistRepository
import com.yatharth.musitify.data.repository.SettingsRepository
import com.yatharth.musitify.data.repository.SongRepository
import com.yatharth.musitify.data.repository.SyncSongRef
import com.yatharth.musitify.network.model.FetchLibraryPayload
import com.yatharth.musitify.network.model.MPlaylist_Song
import com.yatharth.musitify.network.model.NPlaylist
import com.yatharth.musitify.network.model.NSong
import com.yatharth.musitify.network.model.PushLibraryPayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.UUID

/**
 * Port of Android's legacy library-snapshot sync (`POST {base}/sync/bootstrap`, `GET {base}/sync`).
 * **Unused**, as on Android (its calls are commented out there); kept for completeness and not
 * bound in Koin.
 *
 * Desktop fixes: base URL from `server_url`; rows with a null uuid are left out instead of being
 * sent as "null"; lenient decoding; existing playlists are matched by uuid (Android's lookup
 * filtered by name); the pull does not record PLAYLIST_CREATED; failures are thrown.
 */
class SyncController(
    val songRepository: SongRepository,
    val playlistRepository: PlaylistRepository,
    val settingsRepository: SettingsRepository,
    private val playlistDao: PlaylistDao,
    private val client: OkHttpClient = OkHttpClient(),
) {
    private val tag = "SyncController"

    suspend fun sendData() {
        val deviceId = SyncConfig.deviceId(settingsRepository)

        val songList = songRepository.allSongs.first().mapNotNull {
            NSong(
                id = it.uuid?.toString() ?: return@mapNotNull null,
                title = it.name,
                artist = it.artist,
                url = it.url,
                downloaded_at = it.downloadedAt
            )
        }

        val playlists = playlistRepository.allPlaylists.first().mapNotNull {
            NPlaylist(
                id = it.playlist.uuid?.toString() ?: return@mapNotNull null,
                name = it.playlist.name,
                created_on = it.playlist.createdAt
            )
        }

        val userPlaylists = playlistRepository.getAllSongRelations().mapNotNull {
            MPlaylist_Song(
                song_id = it.song_uuid?.toString() ?: return@mapNotNull null,
                playlist_id = it.playlist_uuid?.toString() ?: return@mapNotNull null,
                position = it.position,
                added_on = it.addedOn
            )
        }

        val syncPayload = PushLibraryPayload(
            device_id = deviceId,
            songs = songList,
            playlists = playlists,
            playlist_songs = userPlaylists,
        )

        val request = Request.Builder()
            .url(SyncConfig.endpoint(settingsRepository, "sync/bootstrap"))
            .post(SyncConfig.encodeJson.encodeToString(syncPayload).toRequestBody(SyncConfig.JSON))
            .build()

        withContext(Dispatchers.IO) {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("Sync failed: ${response.code} ${response.body.string()}")
                }
                println("$tag: Server response: ${response.body.string()}")
            }
        }
    }

    suspend fun receiveData() {
        val request = Request.Builder()
            .url(SyncConfig.endpoint(settingsRepository, "sync"))
            .get()
            .build()

        val body = withContext(Dispatchers.IO) {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("Fetch failed: ${response.code} ${response.body.string()}")
                }
                response.body.string()
            }
        }
        val payload = SyncConfig.decodeJson.decodeFromString<FetchLibraryPayload>(body)

        val nSongs = payload.likedSongs
        val songs = songRepository.getSongsByUrl(nSongs.map { it.url })
        val existingSongIds = songs.map { it.uuid.toString() }
        val newSongs = nSongs.filter { it.id !in existingSongIds }

        val newSongsDownloaded = songRepository.downloadSongsFromSync(
            nSongs = newSongs.map { SyncSongRef(id = it.id, url = it.url, downloadedAt = it.downloaded_at) }
        )

        val oldPlaylists = playlistDao.getPlaylistsByUuids(payload.playlists.map { it.id })
            .associateBy { it.uuid.toString() }
        val toCreatePlaylists = payload.playlists.filter { it.id !in oldPlaylists.keys }
        val newPlaylists = toCreatePlaylists.map {
            playlistRepository.createPlaylist(
                name = it.name,
                uuid = runCatching { UUID.fromString(it.id) }.getOrNull(),
                record = false,
                createdAt = it.created_on
            )
        }.associateBy { it.uuid.toString() } + oldPlaylists

        val songMap = songs.associateBy { it.uuid.toString() } + newSongsDownloaded

        val songToPlaylist = payload.playlist_songs.mapNotNull { relation ->
            val song = songMap[relation.song_id] ?: return@mapNotNull null
            val playlist = newPlaylists[relation.playlist_id] ?: return@mapNotNull null
            if (song.id <= 0 || playlist.id <= 0) {
                System.err.println("$tag: Skipping relation with unresolved ids: $relation")
                return@mapNotNull null
            }
            SongToPlaylist(
                songId = song.id,
                playListId = playlist.id,
                position = relation.position,
                addedOn = relation.added_on,
                song_uuid = song.uuid,
                playlist_uuid = playlist.uuid
            )
        }
        playlistRepository.addSongPlaylistRelations(songToPlaylist)
        newPlaylists.values.forEach {
            playlistRepository.generatePlaylistArt(it.id)
        }
    }
}
