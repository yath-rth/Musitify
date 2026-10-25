package com.yatharth.musitify.data

import com.yatharth.musitify.data.entity.ActionType
import com.yatharth.musitify.data.entity.Playlist
import com.yatharth.musitify.data.entity.Song
import com.yatharth.musitify.data.entity.SongToPlaylist
import com.yatharth.musitify.data.entity.SyncAction
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DatabaseSmokeTest {

    @Test
    fun insertAndQuerySongPlaylistRelation() = runBlocking {
        val dir = Files.createTempDirectory("musitify-test").toFile()
        val db = MusitifyDatabase.build(dir.resolve("test.db"))
        try {
            val songUuid = UUID.randomUUID()
            val songId = db.songDao().addSong(
                Song(
                    name = "Song", artist = "Artist", length = 1000, filePath = "a.mp3",
                    artPath = "a.webp", timeHeard = 0, downloadedAt = 1, url = "https://x/1",
                    uuid = songUuid
                )
            ).toInt()
            val playlistId = db.playlistDao().addPlaylist(Playlist(name = "Mix", createdAt = 1)).toInt()
            val rel = SongToPlaylist(songId = songId, playListId = playlistId, position = 0, addedOn = 1)
            db.songToPlaylistDao().addSongToPlaylist(rel)

            assertEquals(songUuid, db.songDao().getSongByUuid(songUuid.toString())?.uuid)
            assertEquals(listOf(songId), db.playlistDao().getSongsForPlaylist(playlistId).first().map { it.id })
            val info = db.playlistDao().getAllPlaylists().first().single()
            assertEquals(1, info.numOfSongs)
            assertEquals(1000L, info.playlistLength)

            // Desktop unique (songId, playListId): duplicates are rejected / replaced, never doubled.
            assertEquals(-1L, db.songToPlaylistDao().addSongToPlaylistIfAbsent(rel))
            db.songToPlaylistDao().addSongsToPlaylist(listOf(rel.copy(position = 5)))
            assertEquals(1, db.songToPlaylistDao().getSongsInPlaylist(playlistId).size)

            // markSynced works.
            val action = SyncAction(type = ActionType.SONG_LIKED, songUuid = songUuid.toString())
            db.syncActionDao().insert(action)
            assertEquals(1, db.syncActionDao().getUnsyncedCount().first())
            db.syncActionDao().markSynced(listOf(action.actionUuid))
            assertTrue(db.syncActionDao().getUnsynced().isEmpty())

            // FK cascade.
            db.songDao().deleteSongById(songId)
            assertTrue(db.songToPlaylistDao().getAll().isEmpty())
        } finally {
            db.close()
            dir.deleteRecursively()
        }
    }
}
