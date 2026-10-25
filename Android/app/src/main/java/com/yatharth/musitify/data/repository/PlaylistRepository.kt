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
import com.yatharth.musitify.util.SongMedia.cachePlaylistCover
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.io.IOException
import java.util.UUID
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

        val playlistName = result?.callAttr("get", "name")?.toString()?.trim() ?: "Unknown Playlist"
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

        val playlistId: Int
        if (existing != null) {
            Log.d(TAG, "Playlist '$name' already exists")
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
        val songs = songDao.getSongsByIds(songRelations.map { it.id })
        val art = getOrCreatePlaylistCover(playlistId, songs, refresh)
        playlistDao.updateArt(playlistId, art.path)
    }

    /** Re-renders the cover of the playlist with [playlistUuid], if it exists. */
    suspend fun refreshPlaylistArtByUuid(playlistUuid: String) = withContext(Dispatchers.IO) {
        val playlist = playlistDao.getPlaylistByUuid(playlistUuid) ?: return@withContext
        generatePlaylistArt(playlist.id, refresh = true)
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

    fun getOrCreatePlaylistCover(playlistId: Int, songs: List<Song>, refresh: Boolean = false): File {
        val coversDir = File(context.filesDir, "playlist_covers").apply { mkdirs() }
        val outFile = File(coversDir, "playlist_$playlistId.png")
        if (!refresh && outFile.exists()) return outFile

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
                        song = songs[songId],
                        playlistUuid = existingPlaylist.uuid.toString(),
                        position = position + i,
                        added_on = System.currentTimeMillis()
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
                        song = songs[songId],
                        playlistUuid = playlist.uuid.toString(),
                        position = position + i,
                        added_on = System.currentTimeMillis(),
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

    suspend fun updatePlaylist(playlist: Playlist, record: Boolean = true) {
        if (record) {
            // Get the old playlist to get the old name
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


    suspend fun addSongToPlaylistSync(
        songUuid: String?, playlistUuid: String?, payload: String?
    ) =
        withContext(Dispatchers.IO) {
            val song = songDao.getSongByUuid(songUuid ?: throw IOException("Invalid song uuid"))
                ?: throw IOException("Song doesnt exist $songUuid")
            val playlist =
                playlistDao.getPlaylistByUuid(
                    playlistUuid ?: throw IOException("Invalid playlist id")
                ) ?: throw IOException("Playlist doesnt exist")

            val position = if (payload != null) {
                val jsonObject = Json.parseToJsonElement(payload).jsonObject
                jsonObject["position"]?.jsonPrimitive?.content?.toInt()
                    ?: 0
            } else null

            addSongsToPlaylist(playlist, mapOf(song.id to song))
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
        Log.d("Temp", "Removed song $songId from $playlistId")
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
            toIndex < 0 || toIndex >= songsInPlaylist.size) {
            return@withContext
        }

        // Get the playlist for UUID
        val playlist = playlistDao.getPlaylistById(playlistId)
        val movedSong = songsInPlaylist[fromIndex]
        val song = songDao.getSongById(movedSong.songId)

        // Create a mutable list and reorder
        val reorderedList = songsInPlaylist.toMutableList()
        val movedItem = reorderedList.removeAt(fromIndex)
        reorderedList.add(toIndex, movedItem)

        // Update positions in database
        reorderedList.forEachIndexed { index, songToPlaylist ->
            songToPlaylistDao.updatePosition(
                songId = songToPlaylist.songId,
                playlistId = playlistId,
                newPosition = index
            )
        }

        // Record the action for sync
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

        val song = songDao.getSongByUuid(songUuid)
            ?: throw IOException("Song doesn't exist: $songUuid")
        val playlist = playlistDao.getPlaylistByUuid(playlistUuid)
            ?: throw IOException("Playlist doesn't exist: $playlistUuid")

        val jsonObject = Json.parseToJsonElement(payload).jsonObject
        val fromPosition = jsonObject["from_position"]?.jsonPrimitive?.content?.toInt() ?: 0
        val toPosition = jsonObject["to_position"]?.jsonPrimitive?.content?.toInt() ?: 0

        // Apply the reorder without recording it again (to avoid sync loop)
        reorderSongsInPlaylist(playlist.id, fromPosition, toPosition, record = false)
    }
}