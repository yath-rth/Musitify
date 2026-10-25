package com.yatharth.musitify.engine

import org.koin.core.module.Module
import org.koin.dsl.module
import java.io.File

/**
 * Filesystem locations used by the engine.
 * Defaults: songs + thumbnails in `%LOCALAPPDATA%\Musitify\songs` (flat, like Android's
 * external files dir), engine state in `%LOCALAPPDATA%\Musitify\engine`, playlist covers in
 * `%APPDATA%\Musitify\playlist_covers`.
 */
class EnginePaths(
    private val songsDirProvider: () -> File,
    val engineDataDir: File,
    val playlistCoversDir: File,
) {
    constructor(songsDir: File, engineDataDir: File, playlistCoversDir: File) :
        this({ songsDir }, engineDataDir, playlistCoversDir)

    /** Re-read on every access so a changed music folder applies without a restart. */
    val songsDir: File get() = songsDirProvider()

    fun playlistCover(playlistId: Long): File = File(playlistCoversDir, "playlist_$playlistId.png")

    companion object {
        fun default(): EnginePaths {
            val home = System.getProperty("user.home")
            val local = File(System.getenv("LOCALAPPDATA") ?: "$home\\AppData\\Local", "Musitify")
            val roaming = File(System.getenv("APPDATA") ?: "$home\\AppData\\Roaming", "Musitify")
            return EnginePaths(
                songsDir = File(local, "songs"),
                engineDataDir = File(local, "engine"),
                playlistCoversDir = File(roaming, "playlist_covers"),
            )
        }
    }
}

/**
 * Engine singletons. The data layer must provide a [DownloadLibrary] binding (resolved lazily
 * when [DownloadQueue] is first requested), e.g. `single<DownloadLibrary> { SongLibraryAdapter(get(), get()) }`.
 */
fun engineModule(paths: EnginePaths = EnginePaths.default()): Module = module {
    single { paths }
    single {
        YtDlpUpdater(
            stampFile = File(paths.engineDataDir, "yt-dlp-last-update.txt"),
            writableDir = File(paths.engineDataDir, "bin"),
        )
    }
    single { Downloader() }
    single<SongDownloader> { get<Downloader>() }
    single {
        val updater = get<YtDlpUpdater>()
        DownloadQueue(
            downloader = get(),
            library = get(),
            songsDir = { paths.songsDir.apply { mkdirs() } },
            beforeJob = { updater.updateIfDue() },
        )
    }
    single { AudioPlayer() }
    single { SleepTimer() }
}

val engineModule: Module = engineModule()
