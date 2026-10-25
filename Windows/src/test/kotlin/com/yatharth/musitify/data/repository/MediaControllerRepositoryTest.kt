package com.yatharth.musitify.data.repository

import com.yatharth.musitify.data.MusitifyDatabase
import com.yatharth.musitify.data.entity.Playlist
import com.yatharth.musitify.data.entity.Song
import com.yatharth.musitify.engine.SleepTimer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MediaControllerRepositoryTest {

    @Test
    fun listenFlushPersistsSongPlaylistAndSession() = runBlocking {
        val dir = Files.createTempDirectory("musitify-mc-test").toFile()
        val db = MusitifyDatabase.build(dir.resolve("t.db"))
        try {
            val songId = db.songDao().addSong(
                Song(name = "n", artist = "a", length = 200_000, filePath = "f.mp3", artPath = "f.webp",
                    timeHeard = 0, downloadedAt = 1, url = "u")
            ).toInt()
            val playlistId = db.playlistDao().addPlaylist(Playlist(name = "Mix", createdAt = 1)).toInt()
            val song = db.songDao().getSongById(songId)!!

            val repo = MediaControllerRepository(
                db.songDao(), db.playlistDao(), db.listeningSessionDao(), SleepTimer(),
                playerProvider = { error("player not needed") },
            )
            assertNull(repo.controller.value)

            repo.prepareSession(listOf(song), playlistId).join()
            repo.recordListen(songId, 7_000).join()
            repo.recordListen(songId, 3_000).join()

            assertEquals(10_000, db.songDao().getSongById(songId)!!.timeHeard)
            assertEquals(10_000, db.playlistDao().getPlaylistById(playlistId)!!.timeHeard)
            val rows = db.listeningSessionDao().getAll().first()
            assertEquals(2, rows.size)
            val r = rows.first { it.durationMs == 7_000L }
            assertEquals(songId, r.songId)
            assertEquals(playlistId, r.playlistId)
            assertEquals("n", r.songName)
            assertEquals("a", r.artistName)
            assertEquals("Mix", r.playlistName)
            assertEquals("f.webp", r.songArtPath)
            assertEquals(200_000, r.songLength)

            // No playlist: only the song is credited.
            repo.prepareSession(listOf(song), null).join()
            repo.recordListen(songId, 1_000).join()
            assertEquals(11_000, db.songDao().getSongById(songId)!!.timeHeard)
            assertEquals(10_000, db.playlistDao().getPlaylistById(playlistId)!!.timeHeard)
            assertNull(db.listeningSessionDao().getAll().first().last().playlistId)
        } finally {
            db.close(); dir.deleteRecursively()
        }
    }
}
