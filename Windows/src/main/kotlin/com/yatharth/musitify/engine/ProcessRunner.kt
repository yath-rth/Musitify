package com.yatharth.musitify.engine

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.coroutines.resume

/** Result of a finished subprocess. stdout and stderr are captured separately. */
data class ProcessResult(val exitCode: Int, val stdout: String, val stderr: String) {
    val isSuccess get() = exitCode == 0
}

class ProcessFailedException(
    val command: List<String>,
    val result: ProcessResult,
) : RuntimeException(
    "${File(command.first()).name} exited with ${result.exitCode}: " +
        result.stderr.lines().lastOrNull { it.isNotBlank() }.orEmpty().ifBlank {
            result.stdout.lines().lastOrNull { it.isNotBlank() }.orEmpty()
        }
)

/**
 * Runs native tools as subprocesses. Streams are drained on background threads (so the
 * child never blocks on a full pipe) and the process tree is force-killed when the calling
 * coroutine is cancelled.
 */
object ProcessRunner {

    /**
     * Runs [command] and suspends until it exits.
     * @param onStdoutLine optional line callback invoked from a reader thread.
     */
    suspend fun run(
        command: List<String>,
        workingDir: File? = null,
        onStdoutLine: ((String) -> Unit)? = null,
        onStderrLine: ((String) -> Unit)? = null,
    ): ProcessResult = withContext(Dispatchers.IO) {
        val process = ProcessBuilder(command)
            .apply { if (workingDir != null) directory(workingDir) }
            .redirectInput(ProcessBuilder.Redirect.PIPE)
            .start()
        runCatching { process.outputStream.close() } // child sees EOF on stdin

        val out = StringBuilder()
        val err = StringBuilder()
        val outThread = drain(process.inputStream, out, onStdoutLine)
        val errThread = drain(process.errorStream, err, onStderrLine)

        try {
            val code = awaitExit(process)
            outThread.join()
            errThread.join()
            ProcessResult(code, out.toString(), err.toString())
        } finally {
            if (process.isAlive) kill(process)
        }
    }

    private suspend fun awaitExit(process: Process): Int = suspendCancellableCoroutine { cont ->
        val future = process.onExit()
        future.thenAccept { p -> cont.resume(p.exitValue()) }
        cont.invokeOnCancellation { kill(process) }
    }

    fun kill(process: Process) {
        runCatching { process.descendants().forEach { it.destroyForcibly() } }
        process.destroyForcibly()
        runCatching { process.waitFor(2, TimeUnit.SECONDS) }
    }

    private fun drain(
        stream: java.io.InputStream,
        sink: StringBuilder,
        onLine: ((String) -> Unit)?,
    ): Thread = thread(isDaemon = true, name = "proc-drain") {
        runCatching {
            stream.bufferedReader(Charsets.UTF_8).useLines { lines ->
                for (line in lines) {
                    synchronized(sink) { sink.append(line).append('\n') }
                    onLine?.let { cb -> runCatching { cb(line) } }
                }
            }
        }
    }
}
