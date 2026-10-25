package com.yatharth.musitify.engine

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.jetbrains.skia.Color
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.jetbrains.skia.Surface
import java.io.File
import java.nio.file.Files
import java.util.Collections
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EngineLogicTest {

    // ---- QueueNavigator ----

    @Test
    fun navigationMatchesExoPlayer() {
        val nav = QueueNavigator().apply { setQueue(3) }
        assertEquals(1, nav.nextIndex(0, RepeatMode.OFF))
        assertNull(nav.nextIndex(2, RepeatMode.OFF))
        assertNull(nav.nextIndex(2, RepeatMode.ONE)) // ONE treated as OFF for navigation
        assertEquals(0, nav.nextIndex(2, RepeatMode.ALL))
        assertNull(nav.previousIndex(0, RepeatMode.OFF))
        assertEquals(2, nav.previousIndex(0, RepeatMode.ALL))
        assertEquals(2, nav.autoAdvanceIndex(2, RepeatMode.ONE))
        assertNull(nav.autoAdvanceIndex(2, RepeatMode.OFF))
        assertEquals(0, nav.autoAdvanceIndex(2, RepeatMode.ALL))
        assertTrue(QueueNavigator.previousGoesBack(3_000, true))
        assertFalse(QueueNavigator.previousGoesBack(3_001, true))
        assertFalse(QueueNavigator.previousGoesBack(0, false))
        assertEquals(RepeatMode.ALL, RepeatMode.OFF.next())
        assertEquals(RepeatMode.ONE, RepeatMode.ALL.next())
        assertEquals(RepeatMode.OFF, RepeatMode.ONE.next())
    }

    @Test
    fun shuffleFollowsPermutationFromStart() {
        val nav = QueueNavigator(Random(42)).apply { setQueue(5, startIndex = 3) }
        nav.shuffleEnabled = true
        val order = nav.order()
        assertEquals(3, order.first())
        assertEquals((0 until 5).toSet(), order.toSet())
        var cur = 3
        val visited = mutableListOf(cur)
        while (true) { cur = nav.nextIndex(cur, RepeatMode.OFF) ?: break; visited += cur }
        assertEquals(order, visited)
    }

    // ---- Downloader helpers ----

    @Test
    fun artistFallbackChainNeverNone() {
        fun a(json: String) = Downloader.artistOf(Json.parseToJsonElement(json).jsonObject)
        assertEquals("A", a("""{"artist":"A","artists":["X"],"uploader":"U"}"""))
        assertEquals("X, Y", a("""{"artist":null,"artists":["X","Y"],"uploader":"U"}"""))
        assertEquals("U", a("""{"artists":[],"uploader":"U"}"""))
        assertEquals("Unknown Artist", a("""{"artist":"","uploader":null}"""))
    }

    // ---- SleepTimer ----

    @Test
    fun sleepTimerCompletesAndExtends() = runBlocking {
        val timer = SleepTimer(tickMs = 50)
        val done = CompletableDeferred<Long>()
        val start = System.currentTimeMillis()
        timer.start(null, 300) { done.complete(System.currentTimeMillis()) }
        assertTrue(timer.state.value.isActive)
        timer.extendBy(400)
        val finishedAt = withTimeout(5_000) { done.await() }
        assertTrue(finishedAt - start >= 650, "extend should delay completion (took ${finishedAt - start} ms)")
        assertFalse(timer.state.value.isActive)

        timer.start(SleepTimerDuration.FIVE_MINUTES, SleepTimerDuration.FIVE_MINUTES.toMillis()) {}
        assertEquals(SleepTimerDuration.FIVE_MINUTES, timer.state.value.duration)
        timer.cancelTimer()
        assertFalse(timer.state.value.isActive)
    }

    // ---- CoverCollage ----

    @Test
    fun collageCropsIntoQuadrantsAndLeavesGapsTransparent() {
        val dir = Files.createTempDirectory("collage").toFile()
        fun img(name: String, w: Int, h: Int, color: Int, format: EncodedImageFormat): String {
            val s = Surface.makeRasterN32Premul(w, h)
            s.canvas.clear(color)
            val f = File(dir, name)
            f.writeBytes(s.makeImageSnapshot().encodeToData(format, 90)!!.bytes)
            return f.absolutePath
        }
        val red = img("a.webp", 300, 100, Color.RED, EncodedImageFormat.WEBP)
        val green = img("b.png", 50, 80, Color.GREEN, EncodedImageFormat.PNG)
        val out = File(dir, "covers/playlist_1.png")
        CoverCollage.write(listOf(red, "missing.webp", green), out)
        val image = Image.makeFromEncoded(out.readBytes())
        assertEquals(512, image.width)
        val bmp = org.jetbrains.skia.Bitmap.makeFromImage(image)
        val tl = bmp.getColor(128, 128)                       // TL (lossy WebP → approx red)
        assertTrue(Color.getR(tl) > 240 && Color.getG(tl) < 16 && Color.getB(tl) < 16 && Color.getA(tl) == 255)
        assertEquals(0, Color.getA(bmp.getColor(384, 128)))  // TR: undecodable → transparent
        assertEquals(Color.GREEN, bmp.getColor(128, 384))    // BL
        assertEquals(0, Color.getA(bmp.getColor(384, 384)))  // BR: < 4 images → transparent
        dir.deleteRecursively()
    }

    // ---- DownloadQueue ----

    private class FakeDownloader : SongDownloader {
        val gates = Collections.synchronizedMap(mutableMapOf<String, CompletableDeferred<Unit>>())
        override suspend fun fetchPlaylist(playlistUrl: String) =
            PlaylistInfo("PL", listOf("u1", "owned", "bad", "u2"))

        override suspend fun downloadSong(url: String, outputDir: File, onProgress: ((Float) -> Unit)?): DownloadedSong {
            if (url == "hang") awaitCancellation()
            if (url == "bad") throw DownloadException("boom")
            gates[url]?.await()
            return DownloadedSong(url, url, url, "A", 1000, "$url.mp3", "$url.webp")
        }
    }

    private class FakeLibrary : DownloadLibrary {
        val songs = Collections.synchronizedList(mutableListOf<String>())
        var playlist: Pair<String, List<String>>? = null
        override suspend fun isInLibrary(url: String) = url == "owned" || url in songs
        override suspend fun persistSong(song: DownloadedSong) { songs += song.url }
        override suspend fun persistPlaylist(name: String, songUrls: List<String>) { playlist = name to songUrls }
    }

    @Test
    fun downloadQueueStatesDedupeAndPlaylist() = runBlocking {
        val dl = FakeDownloader()
        val lib = FakeLibrary()
        val q = DownloadQueue(dl, lib, { File(".") })
        val gate = CompletableDeferred<Unit>().also { dl.gates["s1"] = it }

        assertTrue(q.enqueueSong(" s1 "))
        assertFalse(q.enqueueSong("s1"), "duplicate active URL must be rejected")
        assertTrue(q.enqueueSong("bad"))
        gate.complete(Unit)

        suspend fun awaitStatus(url: String, st: DownloadStatus) =
            withTimeout(5_000) { q.jobs.first { l -> l.any { it.url == url && it.status == st } } }

        awaitStatus("s1", DownloadStatus.COMPLETED)
        val failed = awaitStatus("bad", DownloadStatus.FAILED).first { it.url == "bad" }
        assertEquals("boom", failed.error) // single-song failure is FAILED (Android said COMPLETED)

        assertTrue(q.enqueueSong("s1")) // finished jobs don't block
        awaitStatus("s1", DownloadStatus.COMPLETED)

        assertTrue(q.enqueuePlaylist("pl"))
        val pl = awaitStatus("pl", DownloadStatus.COMPLETED).first { it.url == "pl" }
        assertEquals(4, pl.total)
        assertEquals(4, pl.completed)
        assertEquals(1, pl.failed)
        assertEquals("PL" to listOf("u1", "owned", "u2"), lib.playlist)

        assertTrue(q.enqueueSong("hang"))
        withTimeout(5_000) { q.jobs.first { l -> l.any { it.url == "hang" && it.status == DownloadStatus.RUNNING } } }
        val hang = q.jobs.value.first { it.url == "hang" }
        q.cancel(hang.id)
        awaitStatus("hang", DownloadStatus.CANCELLED)
        q.retry(hang.id)
        awaitStatus("hang", DownloadStatus.RUNNING)
        q.cancel(hang.id)
        awaitStatus("hang", DownloadStatus.CANCELLED)
        q.clearFinished()
        assertTrue(q.jobs.value.isEmpty())
        q.shutdown()
    }

    // ---- ProcessRunner cancellation kills the child ----

    @Test
    fun cancellingKillsSubprocess() = runBlocking {
        val pid = CompletableDeferred<Long>()
        val job = launch(kotlinx.coroutines.Dispatchers.IO) {
            ProcessRunner.run(listOf("ping", "-n", "30", "127.0.0.1"), onStdoutLine = {
                ProcessHandle.current().children()
                    .filter { c -> c.info().command().orElse("").lowercase().contains("ping") }
                    .findFirst().ifPresent { c -> pid.complete(c.pid()) }
            })
        }
        val childPid = withTimeout(10_000) { pid.await() }
        job.cancel()
        job.join()
        Thread.sleep(500)
        assertFalse(ProcessHandle.of(childPid).map { it.isAlive }.orElse(false), "child should be killed")
    }
}
