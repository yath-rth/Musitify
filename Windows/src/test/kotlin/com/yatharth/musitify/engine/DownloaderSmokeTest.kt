package com.yatharth.musitify.engine

import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Real-network end-to-end check of the yt-dlp → ffmpeg pipeline. Opt-in:
 *
 *   MUSITIFY_SMOKE_DIR=<output dir> ./gradlew test --tests "*DownloaderSmokeTest*"
 *
 * Optional MUSITIFY_SMOKE_URL overrides the video (default: "Me at the zoo", 19 s).
 */
class DownloaderSmokeTest {
    @Test
    fun downloadsShortVideoToMp3() {
        val dir = System.getenv("MUSITIFY_SMOKE_DIR") ?: return // skipped unless opted in
        val url = System.getenv("MUSITIFY_SMOKE_URL") ?: "https://www.youtube.com/watch?v=jNQXAC9IVRw"
        val out = File(dir).apply { mkdirs() }
        val progress = mutableListOf<Float>()
        val song = runBlocking { Downloader().downloadSong(url, out) { synchronized(progress) { progress += it } } }
        println("SMOKE RESULT: $song")
        println("SMOKE PROGRESS SAMPLES: ${progress.size} last=${progress.lastOrNull()}")
        assertTrue(File(song.audioPath).isFile && File(song.audioPath).length() > 0, "mp3 missing")
        assertTrue(song.audioPath.endsWith(".mp3"))
        assertTrue(song.artPath?.endsWith(".webp") == true, "webp thumbnail missing: ${song.artPath}")
        assertTrue(song.artist.isNotBlank() && song.artist != "None")
        assertTrue(song.durationMs > 0)
        val probed = runBlocking { Downloader().probeDurationMs(song.audioPath) }
        println("SMOKE PROBED DURATION: $probed")
        assertTrue(probed > 0)
    }
}
