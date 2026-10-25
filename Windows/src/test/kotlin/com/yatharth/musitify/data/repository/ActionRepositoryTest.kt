package com.yatharth.musitify.data.repository

import com.yatharth.musitify.data.MusitifyDatabase
import com.yatharth.musitify.data.entity.ActionType
import com.yatharth.musitify.data.entity.Song
import com.yatharth.musitify.data.entity.SyncAction
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Files
import java.util.UUID
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ActionRepositoryTest {
    private lateinit var dir: File
    private lateinit var db: MusitifyDatabase
    private lateinit var settings: SettingsRepository
    private lateinit var actions: ActionRepository

    @BeforeTest
    fun setUp() {
        dir = Files.createTempDirectory("musitify-action-test").toFile()
        db = MusitifyDatabase.build(dir.resolve("test.db"))
        settings = SettingsRepository(db.settingsDao())
        actions = ActionRepository(db.syncActionDao(), settings)
    }

    @AfterTest
    fun tearDown() {
        db.close()
        dir.deleteRecursively()
    }

    private val song = Song(
        name = "song", artist = "artist", length = 1000, filePath = "a.mp3", artPath = "a.webp",
        timeHeard = 0, downloadedAt = 42, url = "https://y/1", uuid = UUID.fromString("00000000-0000-0000-0000-000000000001")
    )

    @Test
    fun recordsPayloadsLikeAndroid() = runBlocking {
        actions.likedSong(song)
        actions.recordSongAddedToPlaylist(song, "p-uuid", position = 3, added_on = 7)
        actions.recordSongAddedToPlaylist(null, "p-uuid", position = 3, added_on = 7) // ignored
        actions.unlikedSong(null) // ignored
        actions.recordPlaylistCreated("p-uuid", "Mix", 5)
        actions.recordSongRenamed(song.uuid.toString(), "a", "b")
        actions.recordPlaylistSongReordered("p-uuid", song.uuid.toString(), 0, 2)
        actions.recordPlaylistDeleted("p-uuid")

        val all = actions.getUnsyncedActions()
        assertEquals(
            listOf(
                ActionType.SONG_LIKED, ActionType.SONG_ADDED_TO_PLAYLIST, ActionType.PLAYLIST_CREATED,
                ActionType.SONG_RENAMED, ActionType.PLAYLIST_SONG_REORDERED, ActionType.PLAYLIST_DELETED
            ),
            all.map { it.type }
        )
        assertEquals("""{"url":"https://y/1","title":"song","artist":"artist","downloaded_at":42}""", all[0].payload)
        assertEquals(song.uuid.toString(), all[0].songUuid)
        assertEquals(
            """{"url":"https://y/1","title":"song","artist":"artist","downloaded_at":42,"position":3,"added_on":7}""",
            all[1].payload
        )
        assertEquals("""{"name":"Mix","created_at":5}""", all[2].payload)
        assertEquals("""{"old_name":"a","new_name":"b"}""", all[3].payload)
        assertEquals("""{"from_position":0,"to_position":2}""", all[4].payload)
        assertNull(all[5].payload)
        assertEquals("p-uuid", all[5].playlistUuid)
    }

    @Test
    fun markSyncedAndPrune() = runBlocking {
        actions.likedSongs(listOf(song, song.copy(url = "https://y/2", uuid = UUID.randomUUID())))
        val unsynced = actions.getUnsyncedActions()
        assertEquals(2, unsynced.size)
        actions.markSynced(listOf(unsynced[0].actionUuid))
        assertEquals(listOf(unsynced[1].actionUuid), actions.getUnsyncedActions().map { it.actionUuid })
        actions.pruneSynced()
        assertEquals(1, db.syncActionDao().getAll().size)
        assertTrue(actions.hasAction(unsynced[1].actionUuid))
    }

    @Test
    fun cursorPersists() = runBlocking {
        assertEquals(0L, actions.getLastCursor())
        actions.setCursor(123L)
        assertEquals(123L, actions.getLastCursor())
        assertEquals("123", settings.getSettingByName("sync_cursor")?.value)
        actions.setCursor(456L)
        // A fresh repository over the same DB sees the stored cursor.
        assertEquals(456L, ActionRepository(db.syncActionDao(), SettingsRepository(db.settingsDao())).getLastCursor())
    }

    @Test
    fun insertRemoteActionsDedupes() = runBlocking {
        val a = SyncAction(type = ActionType.SONG_UNLIKED, songUuid = "x")
        val b = SyncAction(type = ActionType.SONG_UNLIKED, songUuid = "y")
        assertEquals(listOf(a), actions.insertRemoteActions(listOf(a)))
        assertEquals(listOf(b), actions.insertRemoteActions(listOf(a, b)))
    }
}
