package com.yatharth.musitify.engine

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Keeps yt-dlp fresh: runs `yt-dlp -U` at most once per [intervalMs] (default 24h).
 *
 * The bundled exe may live in a read-only location (e.g. `C:\Program Files\Musitify`). In that
 * case (or when the update fails with a permissions error) the exe is copied to [writableDir]
 * and that copy is used from then on via [Tools.ytDlpOverride].
 *
 * @param stampFile file holding the epoch-ms of the last update attempt.
 * @param writableDir user-writable directory for the fallback copy of yt-dlp.exe.
 */
class YtDlpUpdater(
    private val stampFile: File,
    private val writableDir: File,
    private val intervalMs: Long = 24L * 60 * 60 * 1000,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    sealed interface Result {
        data object Skipped : Result
        data class Updated(val exe: File, val output: String) : Result
        data class Failed(val message: String) : Result
    }

    private val mutex = Mutex()
    private val writableCopy get() = File(writableDir, Tools.YT_DLP)

    init {
        // "use that copy thereafter": once a writable copy exists it wins over the bundled one.
        if (writableCopy.isFile) Tools.ytDlpOverride = writableCopy
    }

    fun isDue(): Boolean {
        val last = runCatching { stampFile.readText().trim().toLong() }.getOrNull() ?: return true
        return clock() - last >= intervalMs || clock() < last
    }

    /** Updates yt-dlp if the last attempt was more than [intervalMs] ago. Never throws (except cancellation). */
    suspend fun updateIfDue(): Result = mutex.withLock {
        if (!isDue()) return@withLock Result.Skipped
        val result = update()
        writeStamp()
        result
    }

    /** Unconditionally runs `yt-dlp -U`. */
    suspend fun update(): Result = withContext(Dispatchers.IO) {
        val exe = runCatching { Tools.ytDlp() }.getOrNull()
            ?: return@withContext Result.Failed("yt-dlp.exe not found")

        var target = exe
        if (!isWritable(target)) target = copyToWritable(exe) ?: return@withContext Result.Failed(
            "yt-dlp at ${exe.path} is read-only and could not be copied to ${writableDir.path}"
        )

        val first = runUpdate(target)
        if (first.isSuccess && !looksLikePermissionError(first)) {
            return@withContext Result.Updated(target, first.stdout + first.stderr)
        }
        if (looksLikePermissionError(first) && target != writableCopy) {
            val copy = copyToWritable(exe)
                ?: return@withContext Result.Failed("Permission denied and copy failed")
            val second = runUpdate(copy)
            return@withContext if (second.isSuccess) Result.Updated(copy, second.stdout + second.stderr)
            else Result.Failed(lastLine(second))
        }
        Result.Failed(lastLine(first))
    }

    private suspend fun runUpdate(exe: File): ProcessResult =
        ProcessRunner.run(listOf(exe.absolutePath, "-U", "--no-colors"))

    private fun copyToWritable(exe: File): File? = runCatching {
        writableDir.mkdirs()
        val dest = writableCopy
        if (exe.canonicalFile != dest.canonicalFile) {
            Files.copy(exe.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
        Tools.ytDlpOverride = dest
        dest
    }.getOrNull()

    private fun isWritable(f: File): Boolean =
        Files.isWritable(f.toPath()) && Files.isWritable(f.absoluteFile.parentFile.toPath())

    private fun looksLikePermissionError(r: ProcessResult): Boolean {
        val text = (r.stdout + "\n" + r.stderr).lowercase()
        return listOf("permission denied", "unable to write", "access is denied", "administrator", "errno 13")
            .any { it in text }
    }

    private fun lastLine(r: ProcessResult) =
        (r.stderr + "\n" + r.stdout).lines().lastOrNull { it.isNotBlank() } ?: "exit ${r.exitCode}"

    private fun writeStamp() {
        runCatching {
            stampFile.absoluteFile.parentFile?.mkdirs()
            stampFile.writeText(clock().toString())
        }
    }
}
