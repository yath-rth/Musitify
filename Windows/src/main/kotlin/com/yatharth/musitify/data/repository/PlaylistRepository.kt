package com.yatharth.musitify.data.repository

import com.yatharth.musitify.data.dao.PlaylistDao
import com.yatharth.musitify.data.dao.SongDao
import com.yatharth.musitify.data.dao.SongToPlaylistDao
import com.yatharth.musitify.data.entity.Playlist
import com.yatharth.musitify.data.entity.PlaylistInfo
import com.yatharth.musitify.data.entity.Song
import com.yatharth.musitify.data.entity.SongToPlaylist
import com.yatharth.musitify.engine.CoverCollage
import com.yatharth.musitify.engine.DownloadQueue
import com.yatharth.musitify.engine.EnginePaths
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.io.IOException
import java.util.UUID
import kotlin.math.min

/**
 * Port of Android `PlaylistRepository`. Covers are rendered with the engine [CoverCollage] into
 * [EnginePaths.playlistCoversDir] (`AppPaths.coversDir` in the app).
 *
 * Android's `downloadPlaylist` (Python) is replaced by the engine [DownloadQueue], which calls
 * [createPlaylistAndAddSongs] through [LibraryDownloadAdapter].
 *
 * Desktop deviations (forced by the desktop UNIQUE(songId, playListId) index):
 *  - [addSongsToPlaylist] skips songs already in the playlist (no duplicate relation, no action).
 *  - [addSongToPlaylistSync] does not re-record SONG_ADDED_TO_PLAYLIST (Android echoes it).
 */
class PlaylistRepository(
    private val songDao: SongDao,
    private val playlistDao: PlaylistDao,
    private val songToPlaylistDao: SongToPlaylistDao,
    private val songRepository: SongRepository,
    private val actionRepository: ActionRepository,
    private val enginePaths: EnginePaths,
) {
    private val tag = "PlaylistRepository"

    suspend fun createPlaylistFromSync(
        playlistUuid: String?,
        payload: String?
    ) {
        if (playlistUuid == null || payload == null) throw IOException("Song uuid or payload is wrong")
        val jsonObject = Json.parseToJsonElement(payload).jsonObject
        val name = jsonObject["name"]?.jsonPrimitive?.content
            ?: throw IOException("Name not provided for playlist")
        val createdAt = jsonObject["created_at"]?.jsonPrimitive?.content?.toLongOrNull()
            ?: System.currentTimeMillis()
        createPlaylist(
            name = name,
            uuid = UUID.fromString(playlistUuid),
            record = false,
            createdAt = createdAt
        )
    }

    suspend fun createPlaylist(
        name: String,
        uuid: UUID? = null,
        record: Boolean = true,
        createdAt: Long = System.currentTimeMillis()
    ): Playlist = withContext(Dispatchers.IO) {
        val existing = playlistDao.getPlaylistByName(name)
        if (existing != null) {
            existing
        } else {
            val playlist = Playlist(
                name = name,
                timeHeard = 0,
                createdAt = createdAt,
                uuid = uuid ?: UUID.randomUUID()
            )
            val id = playlistDao.addPlaylist(playlist)
            if (record) actionRepository.recordPlaylistCreated(
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

    /**
     * Renders the playlist cover and stores its path. [refresh] re-renders an existing cover
     * (sync changes the songs after creation). Does nothing for an empty playlist.
     */
    suspend fun generatePlaylistArt(playlistId: Int, refresh: Boolean = false) = withContext(Dispatchers.IO) {
        val songRelations = playlistDao.getSongsForPlaylistSuspend(playlistId)
        if (songRelations.isEmpty()) return@withContext
        // Android re-queries by id, so the collage order is downloadedAt, not playlist position.
        val songs = songDao.getSongsByIds(songRelations.map { it.id })
        val art = getOrCreatePlaylistCover(playlistId, songs, refresh)
        playlistDao.updateArt(playlistId, art.path)
    }

    /** Generates covers for playlists that have songs but no cover yet (e.g. synced before this fix). */
    suspend fun generateMissingPlaylistArt() = withContext(Dispatchers.IO) {
        playlistDao.getAllPlaylistsSuspend()
            .filter { it.artPath.isNullOrBlank() || !File(it.artPath).isFile }
            .forEach { generatePlaylistArt(it.id, refresh = true) }
    }

    suspend fun createPlaylistAndAddSongs(name: String, songs: Map<Int, Song>) =
        withContext(Dispatchers.IO) {
            val playlist = createPlaylist(name)
            addSongsToPlaylist(playlist, songs)
            generatePlaylistArt(playlist.id)
        }

    /** Cached: an existing PNG is returned as-is unless [refresh] is set. */
    fun getOrCreatePlaylistCover(playlistId: Int, songs: List<Song>, refresh: Boolean = false): File {
        val outFile = enginePaths.playlistCover(playlistId.toLong())
        if (!refresh && outFile.exists()) return outFile

        val artPaths = songs.take(min(4, songs.size)).map { it.artPath }
        return CoverCollage.write(artPaths, outFile)
    }

    suspend fun addSongPlaylistRelations(relations: List<SongToPlaylist>) {
        songToPlaylistDao.addSongsToPlaylist(relations)
    }

    suspend fun addSongsToPlaylist(playlist: Playlist, songs: Map<Int, Song>, record: Boolean = true) {
        val existingPlaylist = playlistDao.getPlaylistByName(playlist.name)
            ?: throw IllegalStateException("Playlist with name '${playlist.name}' not found in database")

        val validSongs = songs.filter { song ->
            songDao.getSongById(song.key) != null
        }.ifEmpty {
            throw IllegalStateException("No valid songs to add. All referenced songs may have been deleted.")
        }

        // Android always targets the playlist found by name (both branches end up there).
        val target = existingPlaylist
        val alreadyIn = songToPlaylistDao.getSongIdsInPlaylist(target.id).toSet()
        val toAdd = validSongs.keys.filter { it !in alreadyIn }
        if (toAdd.isEmpty()) return

        val position = songToPlaylistDao.getLastPosition(target.id) + 1
        songToPlaylistDao.addSongsToPlaylistIfAbsent(
            toAdd.mapIndexed { i, songId ->
                if (record) actionRepository.recordSongAddedToPlaylist(
                    song = songs[songId],
                    playlistUuid = target.uuid.toString(),
                    position = position + i,
                    added_on = System.currentTimeMillis()
                )

                SongToPlaylist(
                    songId = songId,
                    playListId = target.id,
                    position = position + i,
                    addedOn = System.currentTimeMillis(),
                    song_uuid = songs[songId]?.uuid,
                    playlist_uuid = target.uuid
                )
            }
        )
    }

    suspend fun getAllSongRelations(): List<SongToPlaylist> {
        return songToPlaylistDao.getAll()
    }

    val allPlaylists: Flow<List<PlaylistInfo>> = playlistDao.getAllPlaylists()

    fun songsInPlaylist(playlistId: Int): Flow<List<Song>> {
        return playlistDao.getSongsForPlaylist(playlistId)
    }

    suspend fun updatePlaylist(playlist: Playlist, record: Boolean = true) {
        if (record) {
            val oldPlaylist = playlistDao.getPlaylistById(playlist.id)
            if (oldPlaylist != null && oldPlaylist.name != playlist.name) {
                actionRepository.recordPlaylistRenamed(
                    playlist.uuid?.toString(),
                    oldPlaylist.name,
                    playlist.name
                )
            }
        }
        playlistDao.updatePlaylist(playlist)
    }

    suspend fun deletePlaylistById(
        id: Int,
        record: Boolean = true
    ) {
        val playlist = playlistDao.getPlaylistById(id) ?: throw IOException("Invalid playlist id")
        if (record) actionRepository.recordPlaylistDeleted(playlist.uuid.toString())
        playlistDao.deleteById(id)
    }

    suspend fun deletePlaylistByUuid(
        uuid: String?,
        record: Boolean = true
    ) {
        if (uuid == null) throw IOException("UUID can not be null")
        val playlist =
            playlistDao.getPlaylistByUuid(uuid) ?: throw IOException("Invalid playlist uuid")
        if (record) actionRepository.recordPlaylistDeleted(playlist.uuid.toString())
        playlistDao.deleteByUuid(uuid)
    }

    /** Applies a remote SONG_ADDED_TO_PLAYLIST. Payload `position` is ignored, as on Android. */
    @Suppress("UNUSED_PARAMETER")
    suspend fun addSongToPlaylistSync(
        songUuid: String?, playlistUuid: String?, payload: String?
    ) = withContext(Dispatchers.IO) {
        val song = songDao.getSongByUuid(songUuid ?: throw IOException("Invalid song uuid"))
            ?: throw IOException("Song doesnt exist $songUuid")
        val playlist =
            playlistDao.getPlaylistByUuid(
                playlistUuid ?: throw IOException("Invalid playlist id")
            ) ?: throw IOException("Playlist doesnt exist")

        addSongsToPlaylist(playlist, mapOf(song.id to song), record = false)
    }

    suspend fun removeSong(songId: Int, playlistId: Int) {
        val song = songDao.getSongById(songId) ?: throw IOException("Invalid song id")
        val playlist =
            playlistDao.getPlaylistById(playlistId) ?: throw IOException("Invalid playlist id")

        val songToPlaylist = songToPlaylistDao.findBySongUuidAndPlaylistUuid(
            song.uuid.toString(),
            playlist.uuid.toString()
        ) ?: throw IOException("Song not in playlist")

        actionRepository.recordSongRemovedFromPlaylist(
            song.uuid.toString(),
            playlist.uuid.toString()
        )
        songToPlaylistDao.removeSong(songToPlaylist)
    }

    suspend fun removeSongFromPlaylistSync(songUuid: String?, playlistUuid: String?) {
        val song = songDao.getSongByUuid(songUuid ?: throw IOException("Invalid song uuid"))
            ?: throw IOException("Song doesnt exist $songUuid")
        val playlist =
            playlistDao.getPlaylistByUuid(
                playlistUuid ?: throw IOException("Invalid playlist id")
            ) ?: throw IOException("Playlist doesnt exist")

        val songToPlaylist = songToPlaylistDao.findBySongUuidAndPlaylistUuid(
            song.uuid.toString(),
            playlist.uuid.toString()
        ) ?: throw IOException("Song not in playlist")

        songToPlaylistDao.removeSong(songToPlaylist)
    }

    suspend fun reorderSongsInPlaylist(
        playlistId: Int,
        fromIndex: Int,
        toIndex: Int,
        record: Boolean = true
    ) = withContext(Dispatchers.IO) {
        val songsInPlaylist = songToPlaylistDao.getSongsInPlaylist(playlistId)

        if (fromIndex < 0 || fromIndex >= songsInPlaylist.size ||
            toIndex < 0 || toIndex >= songsInPlaylist.size
        ) {
            return@withContext
        }

        val playlist = playlistDao.getPlaylistById(playlistId)
        val movedSong = songsInPlaylist[fromIndex]
        val song = songDao.getSongById(movedSong.songId)

        val reorderedList = songsInPlaylist.toMutableList()
        val movedItem = reorderedList.removeAt(fromIndex)
        reorderedList.add(toIndex, movedItem)

        reorderedList.forEachIndexed { index, songToPlaylist ->
            songToPlaylistDao.updatePosition(
                songId = songToPlaylist.songId,
                playlistId = playlistId,
                newPosition = index
            )
        }

        if (record && playlist != null && song != null) {
            actionRepository.recordPlaylistSongReordered(
                playlistUuid = playlist.uuid.toString(),
                songUuid = song.uuid.toString(),
                fromPosition = fromIndex,
                toPosition = toIndex
            )
        }
    }

    suspend fun reorderSongInPlaylistSync(
        songUuid: String?,
        playlistUuid: String?,
        payload: String?
    ) = withContext(Dispatchers.IO) {
        if (songUuid == null || playlistUuid == null || payload == null) {
            throw IOException("Invalid parameters for reordering")
        }

        songDao.getSongByUuid(songUuid)
            ?: throw IOException("Song doesn't exist: $songUuid")
        val playlist = playlistDao.getPlaylistByUuid(playlistUuid)
            ?: throw IOException("Playlist doesn't exist: $playlistUuid")

        val jsonObject = Json.parseToJsonElement(payload).jsonObject
        val fromPosition = jsonObject["from_position"]?.jsonPrimitive?.content?.toInt() ?: 0
        val toPosition = jsonObject["to_position"]?.jsonPrimitive?.content?.toInt() ?: 0

        // Apply without recording again (avoids a sync loop).
        reorderSongsInPlaylist(playlist.id, fromPosition, toPosition, record = false)
    }
}
