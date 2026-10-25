package com.yatharth.musitify.platform

import java.io.File
import java.util.Properties

/**
 * Filesystem locations used by the desktop app.
 *
 * Paths are resolved before the database exists, so user-overridable values
 * (currently only [musicDir]) live in a tiny `config.properties` file next to
 * the DB rather than in the Settings table.
 */
object AppPaths {

    private const val CONFIG_FILE = "config.properties"
    private const val KEY_MUSIC_DIR = "musicDir"

    /** `%APPDATA%\Musitify`, or `~/.musitify` when APPDATA is unavailable. */
    val dataDir: File by lazy {
        val appData = System.getenv("APPDATA")?.takeIf { it.isNotBlank() }
        val dir = if (appData != null) File(appData, "Musitify")
        else File(System.getProperty("user.home"), ".musitify")
        dir.ensureDir()
    }

    val dbFile: File get() = File(dataDir, "musitify.db")

    val coversDir: File get() = File(dataDir, "playlist_covers").ensureDir()

    private val configFile: File get() = File(dataDir, CONFIG_FILE)

    val defaultMusicDir: File
        get() = File(File(System.getProperty("user.home"), "Music"), "Musitify")

    /** Where downloaded songs (mp3 + webp art) are stored. Overridable via [setMusicDir]. */
    val musicDir: File
        @Synchronized get() {
            val custom = loadConfig().getProperty(KEY_MUSIC_DIR)?.takeIf { it.isNotBlank() }
            return (if (custom != null) File(custom) else defaultMusicDir).ensureDir()
        }

    /** Persists a new music directory to `config.properties`; `null` resets to the default. */
    @Synchronized
    fun setMusicDir(dir: File?) {
        val props = loadConfig()
        if (dir == null) props.remove(KEY_MUSIC_DIR)
        else {
            dir.ensureDir()
            props.setProperty(KEY_MUSIC_DIR, dir.absolutePath)
        }
        configFile.outputStream().use { props.store(it, "Musitify desktop config") }
    }

    private fun loadConfig(): Properties = Properties().apply {
        val f = configFile
        if (f.isFile) f.inputStream().use { load(it) }
    }

    private fun File.ensureDir(): File = apply { if (!isDirectory) mkdirs() }
}
