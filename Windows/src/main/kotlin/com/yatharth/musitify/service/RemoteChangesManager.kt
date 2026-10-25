package com.yatharth.musitify.service

import com.yatharth.musitify.data.dao.PlaylistDao
import com.yatharth.musitify.data.dao.SongDao
import com.yatharth.musitify.data.dao.SongToPlaylistDao
import com.yatharth.musitify.data.entity.ActionType
import com.yatharth.musitify.data.entity.Playlist
import com.yatharth.musitify.data.entity.Song
import com.yatharth.musitify.data.entity.SongToPlaylist
import com.yatharth.musitify.data.entity.SyncAction
import com.yatharth.musitify.data.repository.PlaylistRepository
import com.yatharth.musitify.data.repository.SongRepository
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.IOException
import java.util.UUID

/**
 * Port of Android `RemoteChangesManager`: applies pulled actions one by one, in server order;
 * a failing action is logged and skipped (as on Android).
 *
 * Desktop fixes:
 *  - songs are resolved by uuid first, then by payload `url`; playlists by uuid, then by payload
 *    name (`name` / `old_name`), so differing uuids across devices still reconcile;
 *  - SONG_RENAMED and PLAYLIST_RENAMED are applied;
 *  - SONG_ADDED_TO_PLAYLIST records nothing, honours the payload `position` and never inserts a
 *    second relation for the same (song, playlist);
 *  - PLAYLIST_SONG_REORDERED moves the song named by `songUuid` (idempotent) and only falls back
 *    to `from_position` when that song is not found;
 *  - "already applied" situations (song exists, playlist exists, relation gone) are no-ops.
 * Nothing here records a local action.
 */
class RemoteChangesManager(
    private val songRepository: SongRepository,
    private val playlistRepository: PlaylistRepository,
    private val songDao: SongDao,
    private val playlistDao: PlaylistDao,
    private val songToPlaylistDao: SongToPlaylistDao,
) {

    private val tag = "RemoteChanges"

    /** Returns how many actions were applied without error. */
    suspend fun apply(actions: List<SyncAction>): Int {
        var applied = 0
        val touchedPlaylists = mutableSetOf<Int>()
        for (action in actions) {
            try {
                applyOne(action, touchedPlaylists)
                applied++
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                System.err.println("$tag: ${action.type} ${action.actionUuid} skipped: ${e.message}")
            }
        }
        // Covers depend on the playlist's songs, so re-render once per touched playlist.
        for (playlistId in touchedPlaylists) {
            try {
                playlistRepository.generatePlaylistArt(playlistId, refresh = true)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                System.err.println("$tag: cover for playlist $playlistId failed: ${e.message}")
            }
        }
        return applied
    }

    /** Backfills covers for playlists that have songs but no cover file. Never throws. */
    suspend fun generateMissingCovers() {
        try {
            playlistRepository.generateMissingPlaylistArt()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            System.err.println("$tag: cover backfill failed: ${e.message}")
        }
    }

    private suspend fun applyOne(action: SyncAction, touchedPlaylists: MutableSet<Int>) {
        val payload = parsePayload(action.payload)
        val songUuid = action.songUuid.cleanUuid()
        val playlistUuid = action.playlistUuid.cleanUuid()

        when (action.type) {
            ActionType.SONG_LIKED -> {
                val url = payload.str("url") ?: throw IOException("Url not provided for song")
                if (resolveSong(songUuid, url) != null) return
                val downloadedAt = payload.str("downloaded_at")?.toLongOrNull()
                    ?: System.currentTimeMillis()
                songRepository.downloadSong(
                    url = url,
                    downloaded_at = downloadedAt,
                    songUUID = songUuid.toUuidOrNull() ?: UUID.randomUUID(),
                    record = false
                ) ?: throw IOException("Download failed for $url")
            }

            ActionType.SONG_UNLIKED -> {
                val song = resolveSong(songUuid, payload.str("url")) ?: return
                songRepository.deleteSongById(id = song.id, record = false)
            }

            ActionType.SONG_RENAMED -> {
                val newName = payload.str("new_name") ?: throw IOException("new_name missing")
                val song = resolveSong(songUuid, payload.str("url"))
                    ?: throw IOException("Song doesnt exist $songUuid")
                if (song.name != newName) {
                    songRepository.updateSong(song.copy(name = newName), record = false)
                }
            }

            ActionType.PLAYLIST_CREATED -> {
                val name = payload.str("name") ?: throw IOException("Name not provided for playlist")
                if (resolvePlaylist(playlistUuid, name) != null) return
                playlistRepository.createPlaylist(
                    name = name,
                    uuid = playlistUuid.toUuidOrNull(),
                    record = false,
                    createdAt = payload.str("created_at")?.toLongOrNull() ?: System.currentTimeMillis()
                )
            }

            ActionType.PLAYLIST_DELETED -> {
                val playlist = resolvePlaylist(playlistUuid, payload.str("name")) ?: return
                playlistRepository.deletePlaylistById(id = playlist.id, record = false)
            }

            ActionType.PLAYLIST_RENAMED -> {
                val newName = payload.str("new_name") ?: throw IOException("new_name missing")
                val playlist = resolvePlaylist(playlistUuid, payload.str("old_name"))
                    ?: resolvePlaylist(null, newName)
                    ?: throw IOException("Playlist doesnt exist $playlistUuid")
                if (playlist.name == newName) return
                val clash = playlistDao.getPlaylistByName(newName)
                if (clash != null && clash.id != playlist.id) {
                    throw IOException("A playlist named '$newName' already exists")
                }
                playlistRepository.updatePlaylist(playlist.copy(name = newName), record = false)
            }

            ActionType.SONG_ADDED_TO_PLAYLIST -> {
                val song = resolveSong(songUuid, payload.str("url"))
                    ?: throw IOException("Song doesnt exist $songUuid")
                val playlist = resolvePlaylist(playlistUuid, null)
                    ?: throw IOException("Playlist doesnt exist $playlistUuid")
                if (songToPlaylistDao.find(song.id, playlist.id) != null) return
                touchedPlaylists += playlist.id

                val existing = songToPlaylistDao.getSongsInPlaylist(playlist.id)
                val position = payload.str("position")?.toIntOrNull()?.takeIf { it >= 0 }
                    ?: (songToPlaylistDao.getLastPosition(playlist.id) + 1)
                // Make room so the new song lands exactly at `position`.
                existing.filter { it.position >= position }.forEach {
                    songToPlaylistDao.updatePosition(it.songId, playlist.id, it.position + 1)
                }
                songToPlaylistDao.addSongToPlaylistIfAbsent(
                    SongToPlaylist(
                        songId = song.id,
                        playListId = playlist.id,
                        position = position,
                        addedOn = payload.str("added_on")?.toLongOrNull() ?: System.currentTimeMillis(),
                        song_uuid = song.uuid,
                        playlist_uuid = playlist.uuid
                    )
                )
            }

            ActionType.SONG_REMOVED_FROM_PLAYLIST -> {
                val song = resolveSong(songUuid, payload.str("url")) ?: return
                val playlist = resolvePlaylist(playlistUuid, null) ?: return
                songToPlaylistDao.remove(song.id, playlist.id)
                touchedPlaylists += playlist.id
            }

            ActionType.PLAYLIST_SONG_REORDERED -> {
                val from = payload.str("from_position")?.toIntOrNull()
                val to = payload.str("to_position")?.toIntOrNull()
                    ?: throw IOException("to_position missing")
                val playlist = resolvePlaylist(playlistUuid, null)
                    ?: throw IOException("Playlist doesn't exist: $playlistUuid")
                val song = resolveSong(songUuid, null)
                val current = song?.let { s ->
                    songToPlaylistDao.getSongIdsInPlaylist(playlist.id).indexOf(s.id).takeIf { it >= 0 }
                }
                val fromIndex = current ?: from ?: throw IOException("from_position missing")
                if (fromIndex == to) return
                playlistRepository.reorderSongsInPlaylist(playlist.id, fromIndex, to, record = false)
            }
        }
    }

    private suspend fun resolveSong(uuid: String?, url: String?): Song? =
        uuid?.let { songDao.getSongByUuid(it) } ?: url?.let { songDao.getSongByUrl(it) }

    private suspend fun resolvePlaylist(uuid: String?, name: String?): Playlist? =
        uuid?.let { playlistDao.getPlaylistByUuid(it) } ?: name?.let { playlistDao.getPlaylistByName(it) }

    private fun parsePayload(payload: String?): JsonObject? =
        payload?.let { runCatching { Json.parseToJsonElement(it).jsonObject }.getOrNull() }

    private fun JsonObject?.str(key: String): String? =
        this?.get(key)?.let { runCatching { it.jsonPrimitive.contentOrNull }.getOrNull() }

    /** Android sometimes sends the literal "null" for a missing uuid. */
    private fun String?.cleanUuid(): String? =
        this?.trim()?.takeIf { it.isNotEmpty() && it != "null" }

    private fun String?.toUuidOrNull(): UUID? =
        this?.let { runCatching { UUID.fromString(it) }.getOrNull() }
}
