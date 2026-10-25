package com.yatharth.musitify.data

import com.yatharth.musitify.data.repository.ActionRepository
import com.yatharth.musitify.data.repository.LibraryDownloadAdapter
import com.yatharth.musitify.data.repository.MediaControllerRepository
import com.yatharth.musitify.data.repository.NoOpSyncGateway
import com.yatharth.musitify.data.repository.PlaylistRepository
import com.yatharth.musitify.data.repository.SettingsRepository
import com.yatharth.musitify.data.repository.SongRepository
import com.yatharth.musitify.data.repository.StatsRepository
import com.yatharth.musitify.data.repository.SyncGateway
import com.yatharth.musitify.data.viewmodel.AddSongsViewModel
import com.yatharth.musitify.data.viewmodel.HomeScreenViewModel
import com.yatharth.musitify.data.viewmodel.PlaybackControllerViewModel
import com.yatharth.musitify.data.viewmodel.SettingsViewModel
import com.yatharth.musitify.data.viewmodel.StatsViewModel
import com.yatharth.musitify.engine.AudioPlayer
import com.yatharth.musitify.engine.DownloadLibrary
import com.yatharth.musitify.engine.EnginePaths
import com.yatharth.musitify.engine.engineModule
import com.yatharth.musitify.platform.AppPaths
import com.yatharth.musitify.service.IdManager
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import java.io.File

/** Koin replacement for the Hilt-injected repositories and ViewModels. */
val repositoryModule = module {
    single { SettingsRepository(get()) }
    single { ActionRepository(get(), get()) }
    single { SongRepository(get(), get(), get(), get()) }
    single { PlaylistRepository(get(), get(), get(), get(), get(), get()) }
    single { StatsRepository(get(), get(), get(), get()) }
    single {
        // The JavaFX player is created lazily on MediaControllerRepository.connect().
        MediaControllerRepository(get(), get(), get(), get(), playerProvider = { get<AudioPlayer>() })
    }
    single { IdManager(get()) }
    single<DownloadLibrary> { LibraryDownloadAdapter(get(), get()) }
    // Replaced by the network port (ActionController) — a later module definition overrides this.
    single<SyncGateway> { NoOpSyncGateway }

    viewModelOf(::AddSongsViewModel)
    viewModelOf(::HomeScreenViewModel)
    viewModelOf(::PlaybackControllerViewModel)
    viewModelOf(::StatsViewModel)
    viewModelOf(::SettingsViewModel)
}

/** Engine paths for the app: songs in [AppPaths.musicDir], covers in [AppPaths.coversDir]. */
fun appEnginePaths(): EnginePaths = EnginePaths(
    songsDir = AppPaths.musicDir,
    engineDataDir = File(AppPaths.dataDir, "engine"),
    playlistCoversDir = AppPaths.coversDir,
)

/** Every Koin module the app needs: `startKoin { modules(appModules()) }`. */
fun appModules(): List<Module> = listOf(dataModule, engineModule(appEnginePaths()), repositoryModule)
