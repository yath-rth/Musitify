package com.yatharth.musitify.platform

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import java.io.File
import java.io.RandomAccessFile
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.channels.OverlappingFileLockException

/**
 * Ensures only one Musitify runs per user.
 *
 * The first instance holds an exclusive lock on `AppPaths.dataDir/instance.lock` and listens on
 * a loopback socket whose port is written to `instance.port`. A second launch fails to get the
 * lock, pings that socket (so the first instance can show its window) and should exit.
 *
 * ```
 * fun main() {
 *     if (!SingleInstance.acquire()) exitProcess(0) // first instance was asked to show itself
 *     application {
 *         LaunchedEffect(Unit) {
 *             SingleInstance.activations.collect { visible = true; windowState.isMinimized = false }
 *         }
 *         ...
 *         // on Quit: SingleInstance.release() (optional — the OS drops the lock on exit)
 *     }
 * }
 * ```
 */
object SingleInstance {

    private const val LOCK_FILE = "instance.lock"
    private const val PORT_FILE = "instance.port"
    private const val SHOW_TOKEN = "MUSITIFY_SHOW"

    private var channel: FileChannel? = null
    private var lock: FileLock? = null
    private var server: ServerSocket? = null

    /** Conflated so a signal arriving before the UI starts collecting isn't lost. */
    private val _activations = Channel<Unit>(Channel.CONFLATED)

    /**
     * Emits whenever another launch asked this instance to bring its window to front.
     * Single consumer (collect it once, e.g. in a `LaunchedEffect` in `application {}`).
     */
    val activations: Flow<Unit> = _activations.receiveAsFlow()

    /**
     * Tries to become the primary instance.
     *
     * @return true if this process is the only instance (keep running); false if another
     * instance is already running — it has been signalled to show itself and the caller
     * should exit. If locking itself fails for an unexpected reason, returns true (better to
     * allow a duplicate than to refuse to start).
     */
    @Synchronized
    fun acquire(dir: File = AppPaths.dataDir): Boolean {
        if (lock != null) return true
        val acquired = try {
            val ch = RandomAccessFile(File(dir, LOCK_FILE), "rw").channel
            val l = try {
                ch.tryLock()
            } catch (_: OverlappingFileLockException) {
                null
            }
            if (l == null) {
                ch.close()
                false
            } else {
                channel = ch
                lock = l
                true
            }
        } catch (t: Throwable) {
            System.err.println("Musitify: single-instance lock unavailable: $t")
            return true
        }

        if (!acquired) {
            signalExisting(dir)
            return false
        }
        startServer(dir)
        return true
    }

    /** Releases the lock and stops the activation listener. */
    @Synchronized
    fun release() {
        runCatching { server?.close() }
        server = null
        runCatching { lock?.release() }
        runCatching { channel?.close() }
        lock = null
        channel = null
    }

    private fun startServer(dir: File) {
        try {
            val ss = ServerSocket()
            ss.bind(InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 4)
            server = ss
            File(dir, PORT_FILE).writeText(ss.localPort.toString())
            Thread({
                while (!ss.isClosed) {
                    try {
                        ss.accept().use { s ->
                            s.soTimeout = 1_000
                            val msg = s.getInputStream().readNBytes(SHOW_TOKEN.length).decodeToString()
                            if (msg == SHOW_TOKEN) _activations.trySend(Unit)
                        }
                    } catch (_: Exception) {
                        // closed on release() or a bad client; loop condition decides
                    }
                }
            }, "musitify-single-instance").apply { isDaemon = true }.start()
        } catch (t: Throwable) {
            System.err.println("Musitify: activation listener unavailable: $t")
        }
    }

    private fun signalExisting(dir: File) {
        try {
            val port = File(dir, PORT_FILE).readText().trim().toInt()
            Socket().use { s ->
                s.connect(InetSocketAddress(InetAddress.getLoopbackAddress(), port), 1_000)
                s.getOutputStream().apply { write(SHOW_TOKEN.toByteArray()); flush() }
            }
        } catch (t: Throwable) {
            System.err.println("Musitify: could not signal running instance: $t")
        }
    }
}
