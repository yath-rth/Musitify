package com.yatharth.musitify.data.repository

import android.util.Log
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import com.yatharth.musitify.data.dao.SyncActionDao
import com.yatharth.musitify.data.entity.ActionType
import com.yatharth.musitify.data.entity.Song
import com.yatharth.musitify.data.entity.SongToPlaylist
import com.yatharth.musitify.data.entity.SyncAction
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActionRepository @Inject constructor(
    private val syncActionDao: SyncActionDao,
    private val settingsRepository: SettingsRepository
) {
    private val TAG = "ActionRepository"
    private val json = Json { ignoreUnknownKeys = true }

    // ---- Cursor (server sequence number) ----

    suspend fun getLastCursor(): Long =
        settingsRepository.getSettingByName("sync_cursor")?.value?.toLongOrNull() ?: 0L

    suspend fun setCursor(cursor: Long) {
        settingsRepository.insertSetting("sync_cursor", cursor.toString())
    }

    // ---- Recording local actions (outbox) ----

    private suspend fun record(
        type: ActionType,
        songUuid: String? = null,
        playlistUuid: String? = null,
        payloadJson: String? = null
    ) {
        val action = SyncAction(
            type = type,
            songUuid = songUuid,
            playlistUuid = playlistUuid,
            payload = payloadJson
        )
        val id = syncActionDao.insert(action)
        if (id == -1L) {
            Log.w(TAG, "Duplicate action ignored: $action")
        }
    }

    suspend fun likedSong(song: Song) {
        record(
            type = ActionType.SONG_LIKED,
            songUuid = song.uuid?.toString(),
            payloadJson = json.encodeToString(
                buildJsonObject {
                    put("url", song.url)
                    put("title", song.name)
                    put("artist", song.artist)
                    put("downloaded_at", song.downloadedAt)
                }.toString()
            )
        )
    }

    suspend fun likedSongs(songs: List<Song>) {
        val actions = songs.map { song ->
            SyncAction(
                type = ActionType.SONG_LIKED,
                songUuid = song.uuid?.toString(),
                payload = json.encodeToString(
                    buildJsonObject {
                        put("url", song.url)
                        put("title", song.name)
                        put("artist", song.artist)
                        put("downloaded_at", song.downloadedAt)
                    }.toString()
                )
            )
        }
        syncActionDao.insertAll(actions)
    }

    suspend fun unlikedSong(songUuid: String?) {
        if (songUuid == null) return
        record(type = ActionType.SONG_UNLIKED, songUuid = songUuid)
    }

    suspend fun recordPlaylistCreated(playlistUuid: String, name: String, createdAt: Long) {
        record(
            type = ActionType.PLAYLIST_CREATED,
            playlistUuid = playlistUuid,
            payloadJson = json.encodeToString(
                buildJsonObject {
                    put("name", name)
                    put("created_at", createdAt)
                }.toString()
            )
        )
    }

    suspend fun recordPlaylistDeleted(playlistUuid: String) {
        record(type = ActionType.PLAYLIST_DELETED, playlistUuid = playlistUuid)
    }

    suspend fun recordSongAddedToPlaylist(songUuid: String, playlistUuid: String, position: Int) {
        record(
            type = ActionType.SONG_ADDED_TO_PLAYLIST,
            songUuid = songUuid,
            playlistUuid = playlistUuid,
            payloadJson = json.encodeToString(
                buildJsonObject { put("position", position) }
            )
        )
    }

    suspend fun recordSongRemovedFromPlaylist(songUuid: String, playlistUuid: String) {
        record(
            type = ActionType.SONG_REMOVED_FROM_PLAYLIST,
            songUuid = songUuid,
            playlistUuid = playlistUuid
        )
    }

    // ---- Outbox management (used by the sync loop) ----

    suspend fun getUnsyncedActions(): List<SyncAction> = syncActionDao.getUnsynced()

    suspend fun markSynced(actionUuids: List<String>) {
        if (actionUuids.isNotEmpty()) syncActionDao.markSynced(actionUuids)
    }

    suspend fun pruneSynced() = syncActionDao.pruneSynced()

    suspend fun hasAction(actionUuid: String): Boolean =
        syncActionDao.getByActionUuid(actionUuid) != null

    // ---- Applying remote actions (pull side) ----

    /**
     * Inserts actions pulled from the server. Duplicates (same actionUuid)
     * are silently skipped thanks to the unique index — makes pulling idempotent.
     * Returns only the actions that were actually new.
     */
    suspend fun insertRemoteActions(actions: List<SyncAction>): List<SyncAction> {
        return actions.filter { !hasAction(it.actionUuid) }
    }
}
