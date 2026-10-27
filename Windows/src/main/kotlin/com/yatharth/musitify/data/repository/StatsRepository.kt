package com.yatharth.musitify.data.repository

import com.yatharth.musitify.data.dao.ListeningSessionDao
import com.yatharth.musitify.data.dao.PlaylistDao
import com.yatharth.musitify.data.dao.SongDao
import com.yatharth.musitify.data.entity.ListeningSession
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class StatsRepository(
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

    val totalCount = songDao.getTotalNumOfSongs()
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
    val sessionCount: Int,
)

data class StatsSnapshot(
    val totalListenMs: Long = 0,
    val todayListenMs: Long = 0,
    /** Last 7 days including today (rolling, not the calendar week). */
    val thisWeekListenMs: Long = 0,
    /** Last 30 days including today (rolling, not the calendar month). */
    val thisMonthListenMs: Long = 0,
    val listensToday: Int = 0,
    val listensThisMonth: Int = 0,
    val totalSessions: Int = 0,
    val averageSessionMs: Long = 0,
    val mostActiveDay: DailyStat? = null,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val averageDailyListenMs: Long = 0,
    val completionRate: Double = 0.0,
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
        // Rolling windows, not calendar ones: a calendar week reset everything
        // to 0 every Sunday/Monday morning (and the month on the 1st), which
        // read as lost history.
        val weekStart = daysBefore(todayStart, 6)    // last 7 days incl. today
        val monthStart = daysBefore(todayStart, 29)  // last 30 days incl. today

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

        // Calculate streaks
        val (currentStreak, longestStreak) = calculateStreaks(dailyMap, todayStart)

        // Calculate average session length (only from song sessions)
        val songSessions = sessions.filter { it.songId != null }
        val totalSessions = songSessions.size
        val averageSessionMs = if (totalSessions > 0) total / totalSessions else 0L

        // Calculate most active day
        val mostActiveDay = dailyMap.maxByOrNull { it.value }?.let {
            DailyStat(it.key, it.value)
        }

        // Calculate average daily listening (from all days with data)
        val daysWithListens = dailyMap.filter { it.value > 0 }.size
        val averageDailyListenMs = if (daysWithListens > 0) total / daysWithListens else 0L

        // Calculate completion rate (songs fully played vs partially)
        val completionRate = calculateCompletionRate(songSessions)

        val topSongsAllTime = songAgg.mapNotNull { (id, list) ->
            toSongStat(id, list)
        }.sortedByDescending { it.fullPlays }.take(10)

        val topSongsThisMonth = songAgg.mapNotNull { (id, list) ->
            val monthList = list.filter { it.startTimeMs >= monthStart }
            if (monthList.isEmpty()) null else toSongStat(id, monthList)
        }.sortedByDescending { it.fullPlays }.take(10)

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
            totalSessions = totalSessions,
            averageSessionMs = averageSessionMs,
            mostActiveDay = mostActiveDay,
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            averageDailyListenMs = averageDailyListenMs,
            completionRate = completionRate,
            dailySeries = dailySeries,
            topSongsAllTime = topSongsAllTime,
            topSongsThisMonth = topSongsThisMonth,
            topArtists = topArtists,
            topPlaylists = topPlaylists
        )
    }

    private fun calculateStreaks(dailyMap: Map<Long, Long>, todayStart: Long): Pair<Int, Int> {
        if (dailyMap.isEmpty()) return 0 to 0

        // Current streak: consecutive days back from today. Today doesn't
        // count against it until it's over, so a streak doesn't show 0 just
        // because you haven't listened yet this morning.
        var currentStreak = 0
        var checkDay = todayStart
        if (dailyMap.getOrDefault(todayStart, 0L) <= 0) checkDay = previousDay(todayStart)
        while (dailyMap.getOrDefault(checkDay, 0L) > 0) {
            currentStreak++
            checkDay = previousDay(checkDay)
        }

        // Longest streak: longest run of consecutive days. (It used to count
        // every day with listening, gaps included.)
        var longestStreak = 0
        var run = 0
        var previous: Long? = null
        for (day in dailyMap.filterValues { it > 0 }.keys.sorted()) {
            run = if (previous != null && previousDay(day) == previous) run + 1 else 1
            longestStreak = maxOf(longestStreak, run)
            previous = day
        }

        return currentStreak to maxOf(longestStreak, currentStreak)
    }

    private fun calculateCompletionRate(sessions: List<ListeningSession>): Double {
        if (sessions.isEmpty()) return 0.0

        val validSessions = sessions.filter { it.songLength > 0 }
        if (validSessions.isEmpty()) return 0.0

        val completedSessions = validSessions.count { session ->
            // Consider a song "completed" if listened to at least 80% of its length
            session.durationMs >= (session.songLength * 0.8)
        }

        return (completedSessions.toDouble() / validSessions.size) * 100
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

    private fun startOfDay(ms: Long): Long =
        Calendar.getInstance().apply {
            timeInMillis = ms
            set(HOUR_OF_DAY, 0)
            set(MINUTE, 0)
            set(SECOND, 0)
            set(MILLISECOND, 0)
        }.timeInMillis

    /** Start of the day before [dayStart]; safe across daylight-saving changes. */
    private fun previousDay(dayStart: Long): Long = startOfDay(dayStart - 1)

    /** Start of the day [days] days before [dayStart]. */
    private fun daysBefore(dayStart: Long, days: Int): Long =
        Calendar.getInstance().apply {
            timeInMillis = dayStart
            add(DAY_OF_MONTH, -days)
        }.timeInMillis.let(::startOfDay)

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
