package com.yatharth.musitify.network

import com.yatharth.musitify.data.MusitifyDatabase
import com.yatharth.musitify.data.dataModule
import com.yatharth.musitify.data.entity.ActionType
import com.yatharth.musitify.data.entity.Playlist
import com.yatharth.musitify.data.entity.Song
import com.yatharth.musitify.data.entity.SyncAction
import com.yatharth.musitify.data.repository.ActionRepository
import com.yatharth.musitify.data.repository.PlaylistRepository
import com.yatharth.musitify.data.repository.SettingsRepository
import com.yatharth.musitify.data.repository.SongRepository
import com.yatharth.musitify.data.repository.SyncGateway
import com.yatharth.musitify.data.repositoryModule
import com.yatharth.musitify.engine.DownloadedSong
import com.yatharth.musitify.engine.EnginePaths
import com.yatharth.musitify.engine.PlaylistInfo
import com.yatharth.musitify.engine.SongDownloader
import com.yatharth.musitify.engine.engineModule
import com.yatharth.musitify.service.RemoteChangesManager
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ActionControllerTest {
    private lateinit var dir: File
    private lateinit var db: MusitifyDatabase
    private lateinit var server: MockWebServer
    private lateinit var settings: SettingsRepository
    private lateinit var actions: ActionRepository
    private lateinit var songs: SongRepository
    private lateinit var playlists: PlaylistRepository
    private lateinit var controller: ActionController

    private object NoDownloads : SongDownloader {
        override suspend fun fetchPlaylist(playlistUrl: String): PlaylistInfo = error("no network")
        override suspend fun downloadSong(url: String, outputDir: File, onProgress: ((Float) -> Unit)?): DownloadedSong =
            throw IOException("no network")
    }

    @BeforeTest
    fun setUp(): Unit = runBlocking {
        dir = Files.createTempDirectory("musitify-sync-test").toFile()
        db = MusitifyDatabase.build(dir.resolve("t.db"))
        server = MockWebServer().apply { start() }
        val paths = EnginePaths(dir.resolve("songs"), dir.resolve("engine"), dir.resolve("covers"))
        settings = SettingsRepository(db.settingsDao())
        actions = ActionRepository(db.syncActionDao(), settings)
        songs = SongRepository(db.songDao(), actions, NoDownloads, paths)
        playlists = PlaylistRepository(db.songDao(), db.playlistDao(), db.songToPlaylistDao(), songs, actions, paths)
        val remote = RemoteChangesManager(songs, playlists, db.songDao(), db.playlistDao(), db.songToPlaylistDao())
        controller = ActionController(settings, actions, remote, OkHttpClient())
        settings.insertSetting("server_url", server.url("/").toString())
        settings.insertSetting("device_id", DEVICE)
    }

    @AfterTest
    fun tearDown() {
        server.close()
        db.close()
        dir.deleteRecursively()
    }

    private suspend fun addSong(name: String, url: String, uuid: UUID = UUID.randomUUID()): Song {
        val id = db.songDao().addSong(
            Song(name = name, artist = "art", length = 1, filePath = "f-$name", artPath = "a-$name",
                timeHeard = 0, downloadedAt = 1, url = url, uuid = uuid)
        )
        return db.songDao().getSongById(id.toInt())!!
    }

    private suspend fun addPlaylist(name: String, uuid: UUID = UUID.randomUUID()): Playlist {
        val id = db.playlistDao().addPlaylist(Playlist(name = name, createdAt = 1, uuid = uuid))
        return db.playlistDao().getPlaylistById(id.toInt())!!
    }

    private fun ok(body: String = "{}") = MockResponse.Builder().code(200).body(body).build()

    private fun action(
        id: Int, type: ActionType, song: String? = null, playlist: String? = null,
        payload: String? = null, uuid: String = UUID.randomUUID().toString(), extra: String = ""
    ): String {
        val fields = buildList {
            add("\"localId\":$id")
            add("\"actionUuid\":\"$uuid\"")
            add("\"type\":\"$type\"")
            add("\"songUuid\":" + (song?.let { "\"$it\"" } ?: "null"))
            add("\"playlistUuid\":" + (playlist?.let { "\"$it\"" } ?: "null"))
            add("\"payload\":" + (payload?.let { "\"" + it.replace("\"", "\\\"") + "\"" } ?: "null"))
            add("\"createdAt\":1000")
        }.joinToString(",")
        return "{$fields$extra}"
    }

    @Test
    fun pushSendsAndroidShapeAndMarksSynced() = runBlocking {
        val song = addSong("song", "https://y/1", UUID.fromString("00000000-0000-0000-0000-000000000001"))
        actions.likedSong(song)
        // Legacy null uuid recorded as the literal "null" (Android SONG_UNLIKED path).
        db.syncActionDao().insert(SyncAction(actionUuid = "u-2", type = ActionType.SONG_UNLIKED, songUuid = "null", createdAt = 99))
        val liked = actions.getUnsyncedActions().first()

        server.enqueue(ok("{\"ok\":true}"))
        assertEquals(2, controller.pushActions())

        val req = server.takeRequest(5, TimeUnit.SECONDS)!!
        assertEquals("POST", req.method)
        assertEquals("/actions", req.url.encodedPath)
        assertEquals("application/json; charset=utf-8", req.headers["Content-Type"])
        val payload = """{\"url\":\"https://y/1\",\"title\":\"song\",\"artist\":\"art\",\"downloaded_at\":1}"""
        assertEquals(
            """{"deviceId":"$DEVICE","actions":[""" +
                """{"localId":${liked.localId},"actionUuid":"${liked.actionUuid}","type":"SONG_LIKED",""" +
                """"songUuid":"00000000-0000-0000-0000-000000000001","payload":"$payload","createdAt":${liked.createdAt}},""" +
                """{"localId":${liked.localId + 1},"actionUuid":"u-2","type":"SONG_UNLIKED","createdAt":99}]}""",
            req.body!!.utf8()
        )
        assertTrue(actions.getUnsyncedActions().isEmpty())

        // Nothing left: the next push sends an empty list (as Android does).
        server.enqueue(ok())
        assertEquals(0, controller.pushActions())
        assertEquals("""{"deviceId":"$DEVICE","actions":[]}""", server.takeRequest(5, TimeUnit.SECONDS)!!.body!!.utf8())
    }

    @Test
    fun failedPushKeepsActionsUnsyncedAndThrows() = runBlocking {
        actions.recordPlaylistDeleted("p")
        server.enqueue(MockResponse.Builder().code(500).body("boom").build())
        val e = assertFailsWith<IOException> { controller.pushActions() }
        assertEquals("Sync failed: 500 boom", e.message)
        assertEquals(1, actions.getUnsyncedActions().size)
    }

    @Test
    fun pullAppliesRenamesSkipsEchoesAndPersistsCursor() = runBlocking {
        val song = addSong("old song", "https://y/1")
        val other = addSong("other", "https://y/2")
        val playlist = addPlaylist("Old")
        actions.recordPlaylistDeleted("whatever") // a local action the server echoes back
        val local = actions.getUnsyncedActions().single()

        val addUuid = UUID.randomUUID().toString()
        val body = """{"actions":[""" + listOf(
            action(3, ActionType.PLAYLIST_DELETED, playlist = playlist.uuid.toString(), uuid = local.actionUuid),
            action(4, ActionType.SONG_UNLIKED, song = song.uuid.toString(), extra = ",\"deviceId\":\"$DEVICE\""),
            action(5, ActionType.PLAYLIST_RENAMED, playlist = playlist.uuid.toString(),
                payload = """{"old_name":"Old","new_name":"New"}"""),
            action(6, ActionType.SONG_RENAMED, song = song.uuid.toString(),
                payload = """{"old_name":"old song","new_name":"new song"}"""),
            action(7, ActionType.SONG_ADDED_TO_PLAYLIST, song = other.uuid.toString(), playlist = playlist.uuid.toString(),
                payload = """{"url":"https://y/2","title":"other","artist":"art","downloaded_at":1,"position":0,"added_on":5}"""),
            action(8, ActionType.SONG_ADDED_TO_PLAYLIST, song = song.uuid.toString(), playlist = playlist.uuid.toString(),
                payload = """{"url":"https://y/1","title":"x","artist":"art","downloaded_at":1,"position":0,"added_on":6}""",
                uuid = addUuid),
            // A second add of the same song with a new uuid must not duplicate the relation.
            action(9, ActionType.SONG_ADDED_TO_PLAYLIST, song = song.uuid.toString(), playlist = playlist.uuid.toString(),
                payload = """{"position":5}"""),
        ).joinToString(",") + """],"cursor":9}"""

        server.enqueue(ok(body))
        assertEquals(5, controller.pullActions())
        val req = server.takeRequest(5, TimeUnit.SECONDS)!!
        assertEquals("GET", req.method)
        assertEquals("/actions?lastFetchedActionId=0", req.target)

        assertEquals("New", db.playlistDao().getPlaylistById(playlist.id)!!.name)
        assertEquals("new song", db.songDao().getSongById(song.id)!!.name)
        assertTrue(db.songDao().getSongById(song.id) != null, "own-device SONG_UNLIKED must be skipped")
        assertTrue(db.playlistDao().getPlaylistById(playlist.id) != null, "echoed PLAYLIST_DELETED must be skipped")
        // Song added at position 0 pushes `other` (also at 0) down; no duplicate rows.
        val rels = db.songToPlaylistDao().getSongsInPlaylist(playlist.id)
        assertEquals(listOf(song.id to 0, other.id to 1), rels.map { it.songId to it.position })
        assertEquals(6L, rels.first().addedOn)
        // Applying recorded nothing new locally (only the original outbox entry remains unsynced).
        assertEquals(listOf(local.actionUuid), actions.getUnsyncedActions().map { it.actionUuid })
        assertEquals(9L, actions.getLastCursor())
        assertEquals("9", settings.getSettingByName("sync_cursor")?.value)

        // The cursor is sent next time; replayed actions are already known and not re-applied.
        server.enqueue(ok(body))
        assertEquals(0, controller.pullActions())
        assertEquals("/actions?lastFetchedActionId=9", server.takeRequest(5, TimeUnit.SECONDS)!!.target)
        assertEquals(2, db.songToPlaylistDao().getSongsInPlaylist(playlist.id).size)
    }

    @Test
    fun pullReconcilesByUrlAndNameWhenUuidsDiffer() = runBlocking {
        val song = addSong("s", "https://y/9")
        val playlist = addPlaylist("Gym")
        val remotePlaylist = UUID.randomUUID().toString()
        val createdPayload = buildJsonObject { put("name", "Gym"); put("created_at", 1) }.toString()
        val body = """{"actions":[""" + listOf(
            action(1, ActionType.PLAYLIST_CREATED, playlist = remotePlaylist, payload = createdPayload),
            action(2, ActionType.SONG_LIKED, song = UUID.randomUUID().toString(),
                payload = """{"url":"https://y/9","title":"s","artist":"a","downloaded_at":1}"""),
            action(3, ActionType.SONG_ADDED_TO_PLAYLIST, song = UUID.randomUUID().toString(),
                playlist = playlist.uuid.toString(), payload = """{"url":"https://y/9","position":0,"added_on":1}"""),
            action(4, ActionType.PLAYLIST_RENAMED, playlist = remotePlaylist,
                payload = """{"old_name":"Gym","new_name":"Gym 2"}"""),
        ).joinToString(",") + "]}"
        server.enqueue(ok(body))
        assertEquals(4, controller.pullActions())

        val all = db.playlistDao().getAllPlaylistsSuspend()
        assertEquals(listOf("Gym 2"), all.map { it.name })
        assertEquals(1, db.songDao().getAllSongsSuspend().size)
        assertEquals(listOf(song.id), db.songToPlaylistDao().getSongIdsInPlaylist(playlist.id))
        assertTrue(actions.getUnsyncedActions().isEmpty())
    }

    @Test
    fun pullErrorThrowsAndKeepsCursor() = runBlocking {
        server.enqueue(MockResponse.Builder().code(503).body("down").build())
        assertFailsWith<IOException> { controller.pullActions() }
        assertEquals(0L, actions.getLastCursor())
    }

    @Test
    fun syncModuleOverridesNoOpGateway() {
        val paths = EnginePaths(dir.resolve("songs"), dir.resolve("engine"), dir.resolve("covers"))
        val app = koinApplication {
            modules(
                dataModule, engineModule(paths), repositoryModule, syncModule,
                module { single { db } },
            )
        }
        try {
            assertIs<ActionController>(app.koin.get<SyncGateway>())
        } finally {
            app.close()
        }
    }

    private companion object {
        const val DEVICE = "dev-1"
    }
}
