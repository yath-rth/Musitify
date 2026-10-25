package com.yatharth.musitify.engine

import java.io.File

/**
 * Resolves the bundled native tools (yt-dlp.exe, ffmpeg.exe, ffprobe.exe).
 *
 * Lookup order:
 *  1. `System.getProperty("compose.application.resources.dir")` — set by Compose for
 *     packaged apps and `./gradlew run` (contents of appResourcesRootDir/windows).
 *  2. `./bin` relative to the working directory (dev; filled by `./gradlew downloadTools`).
 *  3. Every directory on `PATH`.
 *
 * yt-dlp may be overridden by [YtDlpUpdater] with a writable copy (see [ytDlpOverride]).
 */
object Tools {
    const val YT_DLP = "yt-dlp.exe"
    const val FFMPEG = "ffmpeg.exe"
    const val FFPROBE = "ffprobe.exe"

    /** Set by [YtDlpUpdater] when the bundled yt-dlp is read-only and a writable copy is used. */
    @Volatile
    var ytDlpOverride: File? = null

    fun candidateDirs(): List<File> = buildList {
        System.getProperty("compose.application.resources.dir")?.let { add(File(it)) }
        add(File("bin").absoluteFile)
        System.getenv("PATH")?.split(File.pathSeparatorChar)
            ?.filter { it.isNotBlank() }
            ?.forEach { add(File(it.trim('"'))) }
    }

    /** Returns the first existing file named [exeName] (also tries without `.exe`), or null. */
    fun find(exeName: String): File? {
        val names = listOf(exeName, exeName.removeSuffix(".exe")).distinct()
        for (dir in candidateDirs()) {
            for (n in names) {
                val f = File(dir, n)
                if (f.isFile) return f
            }
        }
        return null
    }

    fun require(exeName: String): File =
        find(exeName) ?: throw IllegalStateException(
            "$exeName not found (looked in ${candidateDirs().take(2).joinToString()} and PATH). " +
                "Run `./gradlew downloadTools`."
        )

    fun ytDlp(): File = ytDlpOverride?.takeIf { it.isFile } ?: require(YT_DLP)
    fun ffmpeg(): File = require(FFMPEG)
    fun ffprobe(): File = require(FFPROBE)
}
