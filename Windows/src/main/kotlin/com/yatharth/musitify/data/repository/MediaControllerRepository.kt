package com.yatharth.musitify.data.repository

import com.yatharth.musitify.data.dao.ListeningSessionDao
import com.yatharth.musitify.data.dao.PlaylistDao
import com.yatharth.musitify.data.dao.SongDao
import com.yatharth.musitify.data.entity.ListeningSession
import com.yatharth.musitify.data.entity.Song
import com.yatharth.musitify.engine.AudioPlayer
import com.yatharth.musitify.engine.ListenTracker
import com.yatharth.musitify.engine.PlayerState
import com.yatharth.musitify.engine.RepeatMode
import com.yatharth.musitify.engine.SleepTimer
import com.yatharth.musitify.engine.SleepTimerDuration
import com.yatharth.musitify.engine.SleepTimerState
import com.yatharth.musitify.engine.Track
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * Port of Android `MediaControllerRepository`: glue between the UI and the engine [AudioPlayer]
 * (which replaces the Media3 MediaController + MediaSessionService).
 *
 * - [controller] is null until [connect] (Android connects in `MusitifyApp.onCreate`; desktop in
 *   `bootstrapApp`). [startPlayback] connects lazily if needed.
 * - [uiState] mirrors the player state into Android's [PlayerUiState] (repeat 0 OFF / 1 ONE / 2 ALL).
 * - Listen time follows Android's rules via the engine [ListenTracker] (7 s ticks, 15 s cap); each
 *   flush adds to Song.timeHeard, to the active playlist's timeHeard and inserts a [ListeningSession].
 *
 * Desktop difference: [PlayerUiState.currentSongId] is filled in (Android never set it).
 */
class MediaControllerRepository(
    private val songDao: SongDao,
    private val playlistDao: PlaylistDao,
    private val listeningSessionDao: ListeningSessionDao,
    private val sleepTimerManager: SleepTimer,
    private val playerProvider: () -> AudioPlayer,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _controller = MutableStateFlow<AudioPlayer?>(null)
    val controller: StateFlow<AudioPlayer?> = _controller.asStateFlow()

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    val sleepTimerState: StateFlow<SleepTimerState> = sleepTimerManager.state

    @Volatile
    private var activePlaylistId: Int? = null

    @Volatile
    private var activePlaylistName: String? = null
    private val songDetailsById = ConcurrentHashMap<Int, Song>()

    private val listenTracker = ListenTracker { trackId, deltaMs -> recordListen(trackId.toInt(), deltaMs) }
    private var stateJob: Job? = null
    private var trackingJob: Job? = null

    /** Creates/attaches the player (idempotent). Repeat starts at ALL, as Android sets on connect. */
    @Synchronized
    fun connect() {
        if (_controller.value != null) return
        val player = playerProvider()
        player.setRepeatMode(RepeatMode.ALL)
        stateJob = scope.launch {
            player.state.collect { _uiState.value = it.toUiState() }
        }
        trackingJob = listenTracker.bind(scope, player)
        _controller.value = player
    }

    /**
     * Builds the queue and starts playback. Also snapshots the songs and the active playlist so
     * listen-time tracking can record denormalized stat rows (names survive library deletions).
     */
    fun startPlayback(songs: List<Song>, startIndex: Int, playlistId: Int?) {
        connect()
        prepareSession(songs, playlistId)
        val tracks = songs.map { song ->
            Track(
                id = song.id.toLong(),
                path = song.filePath,
                title = song.name,
                artist = song.artist,
                artPath = song.artPath,
                durationMs = song.length,
            )
        }
        controller.value?.playQueue(tracks, startIndex)
    }

    /** Bookkeeping half of [startPlayback]; returns the playlist-name lookup job. */
    internal fun prepareSession(songs: List<Song>, playlistId: Int?): Job {
        activePlaylistId = playlistId
        activePlaylistName = null
        songDetailsById.clear()
        songs.forEach { songDetailsById[it.id] = it }
        return scope.launch(ioDispatcher) {
            val name = playlistId?.let { playlistDao.getPlaylistById(it)?.name }
            if (activePlaylistId == playlistId) activePlaylistName = name
        }
    }

    fun startSleepTimer(duration: SleepTimerDuration) {
        sleepTimerManager.startTimer(duration) {
            // When the timer completes, pause playback (immediate, as Android).
            controller.value?.pause()
        }
    }

    fun cancelSleepTimer() {
        sleepTimerManager.cancelTimer()
    }

    /** Android `flushListenTime` persistence: one flush of [msToWrite] for [songId]. */
    internal fun recordListen(songId: Int, msToWrite: Long): Job {
        val song = songDetailsById[songId]
        val playlistId = activePlaylistId
        val playlistName = activePlaylistName
        val now = System.currentTimeMillis()

        return scope.launch(ioDispatcher) {
            songDao.addListenTime(songId, msToWrite)
            if (playlistId != null) {
                playlistDao.addListenTime(playlistId, msToWrite)
            }
            listeningSessionDao.insert(
                ListeningSession(
                    songId = songId,
                    playlistId = playlistId,
                    songName = song?.name,
                    artistName = song?.artist,
                    playlistName = playlistName,
                    songArtPath = song?.artPath,
                    songLength = song?.length ?: 0,
                    startTimeMs = now - msToWrite,
                    durationMs = msToWrite
                )
            )
        }
    }

    /** Flushes pending listen time and releases the player (app shutdown). */
    @Synchronized
    fun release() {
        listenTracker.flush()
        trackingJob?.cancel()
        stateJob?.cancel()
        controller.value?.shutdown()
        _controller.value = null
    }

    private fun PlayerState.toUiState() = PlayerUiState(
        isPlaying = isPlaying,
        currentSongId = currentTrack?.id?.toInt(),
        currentTitle = currentTrack?.title,
        repeatMode = repeatMode.value,
        shuffleMode = shuffleEnabled,
        positionMs = positionMs,
        durationMs = durationMs,
    )
}

data class PlayerUiState(
    val isPlaying: Boolean = false,
    val currentSongId: Int? = null,
    val currentTitle: String? = null,
    val repeatMode: Int = 0,
    val shuffleMode: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0
)
