package com.yatharth.musitify.engine

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

enum class DownloadType { SONG, PLAYLIST }

enum class DownloadStatus { QUEUED, RUNNING, COMPLETED, FAILED, CANCELLED }

data class DownloadJob(
    val id: Long,
    val url: String,
    val type: DownloadType,
    val status: DownloadStatus = DownloadStatus.QUEUED,
    val completed: Int = 0,
    val total: Int = 0,          // 0 = unknown / indeterminate
    val error: String? = null,
    /** Playlist jobs: number of songs that failed (job can still be COMPLETED). */
    val failed: Int = 0,
    /** Single-song jobs: yt-dlp download progress 0..1, null = indeterminate. */
    val progress: Float? = null,
    /** Playlist jobs: playlist title once known. */
    val title: String? = null,
) {
    val isActive get() = status == DownloadStatus.QUEUED || status == DownloadStatus.RUNNING
}

/**
 * Persistence hooks the data layer plugs in (keeps the engine independent of Room entities).
 * All methods are called from background coroutines.
 */
interface DownloadLibrary {
    /** True if a song with this URL is already in the library (download is skipped). */
    suspend fun isInLibrary(url: String): Boolean

    /** Store a freshly downloaded song (Android: legacy merge + insert + sync action). */
    suspend fun persistSong(song: DownloadedSong)

    /**
     * Create (or reuse by name, as Android) the playlist [name] and append the songs identified by
     * [songUrls] — every playlist entry now in the library (pre-existing and newly downloaded),
     * in playlist order. Called only when [songUrls] is non-empty.
     */
    suspend fun persistPlaylist(name: String, songUrls: List<String>)
}

/**
 * Port of Android `DownloadsManager`: in-memory job list, [maxParallel] workers (2), playlist
 * jobs download up to [playlistConcurrency] (5) songs concurrently, enqueue de-dupes identical
 * trimmed URLs that are QUEUED/RUNNING, cancel/retry/clearFinished.
 *
 * Fixes vs Android: single-song failures are FAILED (not COMPLETED); cancel kills yt-dlp/ffmpeg;
 * playlist progress counts already-owned songs as completed; playlist songs keep playlist order.
 */
class DownloadQueue(
    private val downloader: SongDownloader,
    private val library: DownloadLibrary,
    private val songsDir: () -> File,
    private val maxParallel: Int = 2,
    private val playlistConcurrency: Int = 5,
    /** Optional: runs before each job (e.g. YtDlpUpdater.updateIfDue). Errors are ignored. */
    private val beforeJob: (suspend () -> Unit)? = null,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _jobs = MutableStateFlow<List<DownloadJob>>(emptyList())
    val jobs: StateFlow<List<DownloadJob>> = _jobs.asStateFlow()

    private val queue = Channel<Long>(Channel.UNLIMITED)
    private val running = ConcurrentHashMap<Long, Job>()
    private val nextId = AtomicLong(1)

    init {
        repeat(maxParallel) {
            scope.launch { for (id in queue) process(id) }
        }
    }

    /** Returns false if the same URL is already queued or running (or blank). */
    fun enqueueSong(url: String): Boolean = enqueue(url, DownloadType.SONG)

    fun enqueuePlaylist(url: String): Boolean = enqueue(url, DownloadType.PLAYLIST)

    fun cancel(id: Long) {
        // QUEUED → CANCELLED directly (worker skips it); RUNNING → cancel the task, whose catch
        // block flips it to CANCELLED after killing the subprocess.
        update(id) { if (it.status == DownloadStatus.QUEUED) it.copy(status = DownloadStatus.CANCELLED) else it }
        running[id]?.cancel()
    }

    fun retry(id: Long) {
        val job = _jobs.value.firstOrNull { it.id == id } ?: return
        if (job.status != DownloadStatus.FAILED && job.status != DownloadStatus.CANCELLED) return
        update(id) {
            it.copy(status = DownloadStatus.QUEUED, completed = 0, total = 0, error = null, failed = 0, progress = null)
        }
        queue.trySend(id)
    }

    fun clearFinished() {
        _jobs.update { list -> list.filter { it.isActive } }
    }

    /** Cancels everything (app shutdown). */
    fun shutdown() {
        scope.cancel()
    }

    private fun enqueue(url: String, type: DownloadType): Boolean {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) return false
        var newId = -1L
        _jobs.update { list ->
            if (list.any { it.url == trimmed && it.isActive }) {
                newId = -1L
                list
            } else {
                newId = nextId.getAndIncrement()
                list + DownloadJob(id = newId, url = trimmed, type = type)
            }
        }
        if (newId < 0) return false
        queue.trySend(newId)
        return true
    }

    private suspend fun process(id: Long) {
        val job = _jobs.value.firstOrNull { it.id == id } ?: return
        if (job.status != DownloadStatus.QUEUED) return // cancelled while waiting

        // Register the task before flipping to RUNNING so cancel() can never miss it.
        val task = scope.launch(start = CoroutineStart.LAZY) {
            try {
                beforeJob?.let { runCatching { it() }.onFailure { e -> if (e is CancellationException) throw e } }
                when (job.type) {
                    DownloadType.SONG -> downloadSingle(id, job.url)
                    DownloadType.PLAYLIST -> downloadPlaylist(id, job.url)
                }
                update(id) {
                    it.copy(
                        status = DownloadStatus.COMPLETED,
                        completed = if (it.total > 0) it.total else 1,
                        total = if (it.total > 0) it.total else 1,
                        progress = null,
                    )
                }
            } catch (e: CancellationException) {
                update(id) { it.copy(status = DownloadStatus.CANCELLED, progress = null) }
                throw e
            } catch (e: Exception) {
                update(id) {
                    it.copy(status = DownloadStatus.FAILED, error = e.message ?: "Unknown error", progress = null)
                }
            }
        }
        running[id] = task
        var startIt = false
        _jobs.update { list ->
            startIt = false
            list.map {
                if (it.id == id && it.status == DownloadStatus.QUEUED) {
                    startIt = true
                    it.copy(status = DownloadStatus.RUNNING)
                } else it
            }
        }
        if (startIt) {
            task.start()
            task.join()
        } else {
            task.cancel() // cancelled between the check above and now
        }
        running.remove(id)
    }

    private suspend fun downloadSingle(id: Long, url: String) {
        if (library.isInLibrary(url)) return
        val song = downloader.downloadSong(url, songsDir()) { p ->
            update(id) { if (it.status == DownloadStatus.RUNNING) it.copy(progress = p) else it }
        }
        library.persistSong(song)
    }

    private suspend fun downloadPlaylist(id: Long, url: String) {
        val info = downloader.fetchPlaylist(url)
        val urls = info.urls.distinct()
        val total = urls.size
        val owned = urls.filter { library.isInLibrary(it) }.toSet()
        val completed = AtomicInteger(owned.size)
        val failed = AtomicInteger(0)
        update(id) { it.copy(title = info.name, completed = completed.get(), total = total) }

        val semaphore = Semaphore(playlistConcurrency)
        val outDir = songsDir()
        val ok: List<String?> = coroutineScope {
            urls.map { u ->
                async {
                    if (u in owned) return@async u
                    semaphore.withPermit {
                        try {
                            library.persistSong(downloader.downloadSong(u, outDir))
                            u
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            failed.incrementAndGet()
                            null
                        } finally {
                            val c = completed.incrementAndGet()
                            update(id) { it.copy(completed = c, total = total, failed = failed.get()) }
                        }
                    }
                }
            }.awaitAll()
        }
        val present = ok.filterNotNull()
        if (present.isEmpty()) {
            if (total == 0) throw IllegalStateException("Playlist is empty")
            throw IllegalStateException("All ${failed.get()} songs failed to download")
        }
        library.persistPlaylist(info.name, present)
    }

    private fun update(id: Long, transform: (DownloadJob) -> DownloadJob) {
        _jobs.update { list -> list.map { if (it.id == id) transform(it) else it } }
    }
}
