package com.yatharth.musitify.data.repository

import com.yatharth.musitify.data.MusitifyDatabase
import com.yatharth.musitify.data.entity.ActionType
import com.yatharth.musitify.data.entity.Song
import com.yatharth.musitify.engine.DownloadedSong
import com.yatharth.musitify.engine.EnginePaths
import com.yatharth.musitify.engine.PlaylistInfo
import com.yatharth.musitify.engine.SongDownloader
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LibraryDownloadAdapterTest {
    private lateinit var dir: File
    private lateinit var db: MusitifyDatabase
    private lateinit var paths: EnginePaths
    private lateinit var songs: SongRepository
    private lateinit var playlists: PlaylistRepository
    private lateinit var adapter: LibraryDownloadAdapter

    private val noDownloader = object : SongDownloader {
        override suspend fun fetchPlaylist(playlistUrl: String): PlaylistInfo = error("unused")
        override suspend fun downloadSong(url: String, outputDir: File, onProgress: ((Float) -> Unit)?) = error("unused")
    }

    @BeforeTest
    fun setUp() {
        dir = Files.createTempDirectory("musitify-lib-test").toFile()
        db = MusitifyDatabase.build(dir.resolve("test.db"))
        paths = EnginePaths(dir.resolve("songs"), dir.resolve("engine"), dir.resolve("covers"))
        val actions = ActionRepository(db.syncActionDao(), SettingsRepository(db.settingsDao()))
        songs = SongRepository(db.songDao(), actions, noDownloader, paths)
        playlists = PlaylistRepository(db.songDao(), db.playlistDao(), db.songToPlaylistDao(), songs, actions, paths)
        adapter = LibraryDownloadAdapter(songs, playlists)
    }

    @AfterTest
    fun tearDown() {
        db.close()
        dir.deleteRecursively()
    }

    private fun downloaded(n: Int, name: String = "song $n"): DownloadedSong {
        paths.songsDir.mkdirs()
        val audio = File(paths.songsDir, "s$n.mp3").apply { writeText("x") }
        return DownloadedSong(
            url = "https://y/$n", title = name, normalizedName = name, artist = "a$n",
            durationMs = 1000L * n, audioPath = audio.absolutePath, artPath = null,
        )
    }

    @Test
    fun persistsSongsAndPlaylistWithActionsAndCover() = runBlocking {
        adapter.persistSong(downloaded(1))
        adapter.persistSong(downloaded(2))
        assertTrue(adapter.isInLibrary("https://y/1"))
        assertFalse(adapter.isInLibrary("https://y/9"))

        val s1 = db.songDao().getSongByUrl("https://y/1")!!
        assertNotNull(s1.uuid)
        assertTrue(s1.artPath.endsWith("s1.webp"))
        assertEquals(1000L, s1.length)

        adapter.persistPlaylist("  Mix ", listOf("https://y/2", "https://y/1"))
        val playlist = db.playlistDao().getPlaylistByName("Mix")!!
        assertEquals(
            listOf("https://y/2", "https://y/1"),
            db.playlistDao().getSongsForPlaylist(playlist.id).first().map { it.url }
        )
        assertTrue(File(playlist.artPath!!).isFile)
        assertEquals(File(paths.playlistCoversDir, "playlist_${playlist.id}.png").path, playlist.artPath)

        // Re-running the playlist download does not duplicate relations or actions.
        adapter.persistPlaylist("Mix", listOf("https://y/2", "https://y/1"))
        assertEquals(2, db.songToPlaylistDao().getSongsInPlaylist(playlist.id).size)

        val types = db.syncActionDao().getAll().map { it.type }
        assertEquals(
            listOf(
                ActionType.SONG_LIKED, ActionType.SONG_LIKED, ActionType.PLAYLIST_CREATED,
                ActionType.SONG_ADDED_TO_PLAYLIST, ActionType.SONG_ADDED_TO_PLAYLIST
            ),
            types
        )
    }

    @Test
    fun legacySongIsMergedByName() = runBlocking {
        db.songDao().addSong(
            Song(
                name = "old", artist = "x", length = 5, filePath = "keep.mp3", artPath = "keep.webp",
                timeHeard = 0, downloadedAt = 1, url = "legacy-123"
            )
        )
        val d = downloaded(3, name = "old")
        adapter.persistSong(d)
        val merged = db.songDao().getSongByName("old")!!
        assertEquals("https://y/3", merged.url)
        assertEquals("keep.mp3", merged.filePath)
        assertFalse(File(d.audioPath).exists())
        assertTrue(db.syncActionDao().getAll().isEmpty())
    }

    @Test
    fun reorderAndRemoveRecordActions() = runBlocking {
        (1..3).forEach { adapter.persistSong(downloaded(it)) }
        val all = db.songDao().getAllSongsSuspend()
        playlists.createPlaylistAndAddSongs("P", all.associateBy { it.id })
        val p = db.playlistDao().getPlaylistByName("P")!!
        playlists.reorderSongsInPlaylist(p.id, 0, 2)
        assertEquals(listOf("https://y/2", "https://y/3", "https://y/1"),
            db.playlistDao().getSongsForPlaylistSuspend(p.id).map { it.url })
        playlists.removeSong(all[0].id, p.id)
        val last = db.syncActionDao().getAll().takeLast(2).map { it.type }
        assertEquals(listOf(ActionType.PLAYLIST_SONG_REORDERED, ActionType.SONG_REMOVED_FROM_PLAYLIST), last)
    }
}
