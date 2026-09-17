package com.yatharth.musitify.data.repository

import com.yatharth.musitify.data.dao.ListeningSessionDao
import com.yatharth.musitify.data.dao.PlaylistDao
import com.yatharth.musitify.data.dao.SongDao
import com.yatharth.musitify.data.entity.ListeningSession
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StatsRepository @Inject constructor(
    private val listeningSessionDao: ListeningSessionDao,
    private val songDao: SongDao,
    private val playlistDao: PlaylistDao,
    private val settingsRepository: SettingsRepository
) {
    val sessions: Flow<List<ListeningSession>> = listeningSessionDao.getAll()

    /**
     * One-time migration of the legacy cumulative listen counters
     * (Song.timeHeard / Playlist.timeHeard) into ListeningSession rows so
     * stats include pre-stats listening. Timestamps are backdated to a fixed
     * historical instant so they only ever feed "all-time" figures, never
     * today/week/month windows.
     */
    suspend fun backfillLegacyListenTime() {
        val alreadyDone =
            settingsRepository.getSettingByName(LEGACY_BACKFILL_KEY)?.value == "done"
        if (alreadyDone) return

        val songSessions = songDao.getAllSongsSuspend().mapNotNull { song ->
            if (song.timeHeard <= 0) return@mapNotNull null
            ListeningSession(
                songId = song.id,
                songName = song.name,
                artistName = song.artist,
                songArtPath = song.artPath,
                songLength = song.length,
                startTimeMs = LEGACY_TIMESTAMP,
                durationMs = song.timeHeard.toLong()
            )
        }

        val playlistSessions = playlistDao.getAllPlaylistsSuspend().mapNotNull { playlist ->
            if (playlist.timeHeard <= 0) return@mapNotNull null
            ListeningSession(
                playlistId = playlist.id,
                playlistName = playlist.name,
                startTimeMs = LEGACY_TIMESTAMP,
                durationMs = playlist.timeHeard.toLong()
            )
        }

        if (songSessions.isNotEmpty() || playlistSessions.isNotEmpty()) {
            listeningSessionDao.insertAll(songSessions + playlistSessions)
        }
        settingsRepository.insertSetting(LEGACY_BACKFILL_KEY, "done")
    }

    private companion object {
        const val LEGACY_BACKFILL_KEY = "legacy_listen_backfill"
        const val LEGACY_TIMESTAMP = 0L
    }
}

data class DailyStat(
    val dayStartMs: Long,
    val totalMs: Long
)

data class SongStat(
    val songId: Int?,
    val name: String,
    val artistName: String,
    val artPath: String?,
    val totalMs: Long,
    val fullPlays: Int,
    val sessionCount: Int
)

data class ArtistStat(
    val name: String,
    val totalMs: Long,
    val songCount: Int
)

data class PlaylistStat(
    val playlistId: Int?,
    val name: String,
    val totalMs: Long,
    val sessionCount: Int
)

data class StatsSnapshot(
    val totalListenMs: Long = 0,
    val todayListenMs: Long = 0,
    val thisWeekListenMs: Long = 0,
    val thisMonthListenMs: Long = 0,
    val listensToday: Int = 0,
    val listensThisMonth: Int = 0,
    val dailySeries: List<DailyStat> = emptyList(),
    val topSongsAllTime: List<SongStat> = emptyList(),
    val topSongsThisMonth: List<SongStat> = emptyList(),
    val topArtists: List<ArtistStat> = emptyList(),
    val topPlaylists: List<PlaylistStat> = emptyList()
) {
    val hasAnyListenTime: Boolean get() = totalListenMs > 0
}

object StatsComputer {

    private const val DAILY_CHART_DAYS = 14

    fun compute(sessions: List<ListeningSession>, now: Long): StatsSnapshot {
        if (sessions.isEmpty()) return StatsSnapshot()

        val todayStart = startOfDay(now)
        val weekStart = startOfWeek(now)
        val monthStart = startOfMonth(now)

        var total = 0L
        var today = 0L
        var week = 0L
        var month = 0L
        var todayCount = 0
        var monthCount = 0
        val dailyMap = mutableMapOf<Long, Long>()
        val songAgg = mutableMapOf<Int?, MutableList<ListeningSession>>()
        val artistAgg = mutableMapOf<String, MutableList<ListeningSession>>()
        val playlistAgg = mutableMapOf<Pair<Int?, String>, MutableList<ListeningSession>>()

        for (session in sessions) {
            // Only song-level rows represent real listening time. Playlist-only
            // rows (songId == null, e.g. the legacy backfill) mirror the same
            // listen time and would double-count totals if summed too.
            if (session.songId != null) {
                total += session.durationMs
                if (session.startTimeMs >= todayStart) {
                    today += session.durationMs
                    todayCount++
                }
                if (session.startTimeMs >= weekStart) week += session.durationMs
                if (session.startTimeMs >= monthStart) {
                    month += session.durationMs
                    monthCount++
                }

                val dayStart = startOfDay(session.startTimeMs)
                dailyMap[dayStart] = (dailyMap[dayStart] ?: 0L) + session.durationMs
            }

            if (session.songId != null) {
                songAgg.getOrPut(session.songId) { mutableListOf() }.add(session)
            }

            // Only sessions tied to an actual song contribute to artist stats
            val artist = session.songName?.let {
                session.artistName?.takeIf { s -> s.isNotBlank() } ?: "Unknown Artist"
            } ?: continue
            artistAgg.getOrPut(artist) { mutableListOf() }.add(session)

            val playlistName =
                session.playlistName ?: session.playlistId?.let { "Playlist #$it" } ?: continue
            playlistAgg.getOrPut(session.playlistId to playlistName) { mutableListOf() }.add(session)
        }

        val dailySeries = (0 until DAILY_CHART_DAYS).map { i ->
            val dayStart = todayStart - (DAILY_CHART_DAYS - 1 - i) * DAY_MS
            DailyStat(dayStart, dailyMap[dayStart] ?: 0L)
        }

        val topSongsAllTime = songAgg.mapNotNull { (id, list) ->
            toSongStat(id, list)
        }.sortedByDescending { it.totalMs }.take(10)

        val topSongsThisMonth = songAgg.mapNotNull { (id, list) ->
            val monthList = list.filter { it.startTimeMs >= monthStart }
            if (monthList.isEmpty()) null else toSongStat(id, monthList)
        }.sortedByDescending { it.totalMs }.take(10)

        val topArtists = artistAgg.mapNotNull { (name, list) ->
            ArtistStat(
                name = name,
                totalMs = list.sumOf { it.durationMs },
                songCount = list.mapNotNull { it.songName }.distinct().size
            )
        }.sortedByDescending { it.totalMs }.take(10)

        val topPlaylists = playlistAgg.mapNotNull { (key, list) ->
            PlaylistStat(
                playlistId = key.first,
                name = key.second,
                totalMs = list.sumOf { it.durationMs },
                sessionCount = list.size
            )
        }.sortedByDescending { it.totalMs }.take(10)

        return StatsSnapshot(
            totalListenMs = total,
            todayListenMs = today,
            thisWeekListenMs = week,
            thisMonthListenMs = month,
            listensToday = todayCount,
            listensThisMonth = monthCount,
            dailySeries = dailySeries,
            topSongsAllTime = topSongsAllTime,
            topSongsThisMonth = topSongsThisMonth,
            topArtists = topArtists,
            topPlaylists = topPlaylists
        )
    }

    private fun toSongStat(songId: Int?, sessions: List<ListeningSession>): SongStat? {
        val latest = sessions.maxByOrNull { it.startTimeMs } ?: return null
        val totalMs = sessions.sumOf { it.durationMs }
        val fullPlays = if (latest.songLength > 0) (totalMs / latest.songLength).toInt()
        else sessions.size
        return SongStat(
            songId = songId,
            name = latest.songName ?: songId?.let { "Song #$it" } ?: "Unknown Song",
            artistName = latest.artistName ?: "Unknown Artist",
            artPath = latest.songArtPath,
            totalMs = totalMs,
            fullPlays = fullPlays,
            sessionCount = sessions.size
        )
    }

    private const val DAY_MS = 86_400_000L
    private const val HOUR_OF_DAY = Calendar.HOUR_OF_DAY
    private const val MINUTE = Calendar.MINUTE
    private const val SECOND = Calendar.SECOND
    private const val MILLISECOND = Calendar.MILLISECOND
    private const val DAY_OF_MONTH = Calendar.DAY_OF_MONTH
    private const val DAY_OF_WEEK = Calendar.DAY_OF_WEEK

    private fun startOfDay(ms: Long): Long =
        Calendar.getInstance().apply {
            timeInMillis = ms
            set(HOUR_OF_DAY, 0)
            set(MINUTE, 0)
            set(SECOND, 0)
            set(MILLISECOND, 0)
        }.timeInMillis

    private fun startOfWeek(ms: Long): Long =
        Calendar.getInstance().apply {
            timeInMillis = ms
            set(HOUR_OF_DAY, 0)
            set(MINUTE, 0)
            set(SECOND, 0)
            set(MILLISECOND, 0)
            set(DAY_OF_WEEK, firstDayOfWeek)
        }.timeInMillis

    private fun startOfMonth(ms: Long): Long =
        Calendar.getInstance().apply {
            timeInMillis = ms
            set(DAY_OF_MONTH, 1)
            set(HOUR_OF_DAY, 0)
            set(MINUTE, 0)
            set(SECOND, 0)
            set(MILLISECOND, 0)
        }.timeInMillis
}