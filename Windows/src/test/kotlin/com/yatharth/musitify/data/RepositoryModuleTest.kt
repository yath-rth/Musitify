package com.yatharth.musitify.data

import com.yatharth.musitify.data.entity.Song
import com.yatharth.musitify.data.repository.LibraryDownloadAdapter
import com.yatharth.musitify.data.repository.SettingsRepository
import com.yatharth.musitify.data.viewmodel.AddSongsUiState
import com.yatharth.musitify.data.viewmodel.AddSongsViewModel
import com.yatharth.musitify.data.viewmodel.HomeScreenViewModel
import com.yatharth.musitify.data.viewmodel.PlaybackControllerViewModel
import com.yatharth.musitify.data.viewmodel.PlaylistDialogUiState
import com.yatharth.musitify.data.viewmodel.StatsViewModel
import com.yatharth.musitify.engine.DownloadLibrary
import com.yatharth.musitify.engine.EnginePaths
import com.yatharth.musitify.engine.engineModule
import com.yatharth.musitify.service.bootstrapApp
import com.yatharth.musitify.ui.elements.SortMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RepositoryModuleTest {

    @Test
    fun graphResolvesAndBootstrapWorks() = runBlocking {
        val dir = Files.createTempDirectory("musitify-koin-test").toFile()
        val paths = EnginePaths(dir.resolve("songs"), dir.resolve("engine"), dir.resolve("covers"))
        val app = koinApplication {
            modules(
                dataModule,
                engineModule(paths),
                repositoryModule,
                module { single { MusitifyDatabase.build(dir.resolve("t.db")) } },
            )
        }
        val koin = app.koin
        val scope = CoroutineScope(Dispatchers.Default)
        try {
            assertIs<LibraryDownloadAdapter>(koin.get<DownloadLibrary>())

            // First run creates the device id once.
            assertNull(bootstrapApp(koin, scope, connectPlayer = false, updateYtDlp = false))
            val settings = koin.get<SettingsRepository>()
            val deviceId = assertNotNull(settings.getSettingByName("device_id")?.value)
            assertTrue(settings.hasUserOpenedApp())
            bootstrapApp(koin, scope, connectPlayer = false, updateYtDlp = false)
            assertEquals(deviceId, settings.getSettingByName("device_id")?.value)

            val addSongs = koin.get<AddSongsViewModel>()
            addSongs.addSingleSong("  ")
            assertEquals(AddSongsUiState.Error("URL is empty"), addSongs.state.value)
            assertEquals(emptyList(), addSongs.jobs.value)

            val playback = koin.get<PlaybackControllerViewModel>()
            assertNull(playback.controller.value)
            assertEquals(false, playback.sleepTimerState.value.isActive)

            val stats = koin.get<StatsViewModel>()
            withTimeout(10_000) {
                while (settings.getSettingByName("legacy_listen_backfill")?.value != "done") delay(20)
            }
            assertEquals(0L, stats.songCount.value)

            val home = koin.get<HomeScreenViewModel>()
            assertEquals(SortMode.PLAYS_DESC, home.songSortMode.value)
            assertEquals(SortMode.PLAYS_DESC, home.playlistSortMode.value)
            val db = koin.get<MusitifyDatabase>()
            val id = db.songDao().addSong(
                Song(name = "a", artist = "b", length = 1, filePath = "f", artPath = "g", timeHeard = 0,
                    downloadedAt = 1, url = "u")
            ).toInt()
            home.createPlaylistWithSongs("Mix", setOf(db.songDao().getSongById(id)!!))
            withTimeout(10_000) { home.state.first { it is PlaylistDialogUiState.Success } }
            assertEquals(listOf("Mix"), home.playlists.first { it.isNotEmpty() }.map { it.playlist.name })
        } finally {
            scope.cancel()
            koin.get<MusitifyDatabase>().close()
            app.close()
            dir.deleteRecursively()
        }
    }
}
