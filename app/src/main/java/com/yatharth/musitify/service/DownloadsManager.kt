package com.yatharth.musitify.service

import com.yatharth.musitify.data.repository.PlaylistRepository
import com.yatharth.musitify.data.repository.SongRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

enum class DownloadType { SONG, PLAYLIST }

enum class DownloadStatus { QUEUED, RUNNING, COMPLETED, FAILED, CANCELLED }

data class DownloadJob(
    val id: Long,
    val url: String,
    val type: DownloadType,
    val status: DownloadStatus = DownloadStatus.QUEUED,
    val completed: Int = 0,
    val total: Int = 0,          // 0 = unknown / indeterminate
    val error: String? = null
) {
    val isActive get() = status == DownloadStatus.QUEUED || status == DownloadStatus.RUNNING
}

@Singleton
class DownloadsManager @Inject constructor(
    private val playlistRepository: PlaylistRepository,
    private val songRepository: SongRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _jobs = MutableStateFlow<List<DownloadJob>>(emptyList())
    val jobs: StateFlow<List<DownloadJob>> = _jobs.asStateFlow()

    private val queue = Channel<Long>(Channel.UNLIMITED)
    private val running = ConcurrentHashMap<Long, Job>()
    private val nextId = AtomicLong(1)

    init {
        // N workers pulling from the queue = N parallel downloads
        repeat(MAX_PARALLEL) {
            scope.launch {
                for (id in queue) process(id)
            }
        }
    }

    /** Returns false if the same URL is already queued or running. */
    fun enqueueSong(url: String): Boolean = enqueue(url, DownloadType.SONG)

    fun enqueuePlaylist(url: String): Boolean = enqueue(url, DownloadType.PLAYLIST)

    fun cancel(id: Long) {
        val task = running[id]
        if (task != null) {
            task.cancel() // status flips to CANCELLED in the catch block
        } else {
            _jobs.update { list ->
                list.map {
                    if (it.id == id && it.status == DownloadStatus.QUEUED)
                        it.copy(status = DownloadStatus.CANCELLED)
                    else it
                }
            }
        }
    }

    fun retry(id: Long) {
        val job = _jobs.value.firstOrNull { it.id == id } ?: return
        if (job.status != DownloadStatus.FAILED && job.status != DownloadStatus.CANCELLED) return
        update(id) {
            it.copy(status = DownloadStatus.QUEUED, completed = 0, total = 0, error = null)
        }
        queue.trySend(id)
    }

    fun clearFinished() {
        _jobs.update { list -> list.filter { it.isActive } }
    }

    private fun enqueue(url: String, type: DownloadType): Boolean {
        val trimmed = url.trim()
        if (_jobs.value.any { it.url == trimmed && it.isActive }) return false

        val id = nextId.getAndIncrement()
        _jobs.update { it + DownloadJob(id = id, url = trimmed, type = type) }
        queue.trySend(id)
        return true
    }

    private suspend fun process(id: Long) {
        val job = _jobs.value.firstOrNull { it.id == id } ?: return
        if (job.status != DownloadStatus.QUEUED) return // cancelled while waiting

        update(id) { it.copy(status = DownloadStatus.RUNNING) }

        val task = scope.launch {
            try {
                when (job.type) {
                    DownloadType.SONG -> songRepository.downloadSong(job.url)
                    DownloadType.PLAYLIST -> playlistRepository.downloadPlaylist(
                        playlistUrl = job.url,
                        onProgress = { completed, total ->
                            update(id) { it.copy(completed = completed, total = total) }
                        }
                    )
                }
                update(id) {
                    it.copy(
                        status = DownloadStatus.COMPLETED,
                        completed = if (it.total > 0) it.total else 1,
                        total = if (it.total > 0) it.total else 1
                    )
                }
            } catch (e: CancellationException) {
                update(id) { it.copy(status = DownloadStatus.CANCELLED) }
                throw e
            } catch (e: Exception) {
                update(id) {
                    it.copy(status = DownloadStatus.FAILED, error = e.message ?: "Unknown error")
                }
            }
        }
        running[id] = task
        task.join()
        running.remove(id)
    }

    private fun update(id: Long, transform: (DownloadJob) -> DownloadJob) {
        _jobs.update { list -> list.map { if (it.id == id) transform(it) else it } }
    }

    companion object {
        private const val MAX_PARALLEL = 2
    }
}