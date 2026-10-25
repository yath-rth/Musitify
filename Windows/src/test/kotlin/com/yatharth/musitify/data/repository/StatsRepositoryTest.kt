package com.yatharth.musitify.data.repository

import com.yatharth.musitify.data.MusitifyDatabase
import com.yatharth.musitify.data.entity.ListeningSession
import com.yatharth.musitify.data.entity.Playlist
import com.yatharth.musitify.data.entity.Song
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import java.util.Calendar
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StatsRepositoryTest {

    private fun at(year: Int, month: Int, day: Int, hour: Int): Long = Calendar.getInstance().apply {
        clear()
        set(year, month - 1, day, hour, 0, 0)
    }.timeInMillis

    private fun dayStart(ms: Long): Long = Calendar.getInstance().apply {
        timeInMillis = ms
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    // Wednesday 17 June 2026 (no DST change nearby; Mon..Wed are in the same week for Sun- or Mon-first locales).
    private val now = at(2026, 6, 17, 12)

    private val sessions = listOf(
        // legacy backfill rows (epoch 0), in DAO order (startTimeMs ASC)
        ListeningSession(playlistId = 5, playlistName = "P", startTimeMs = 0, durationMs = 1_000_000),
        ListeningSession(songId = 3, songName = "C", artistName = "Y", songLength = 10_000, startTimeMs = 0, durationMs = 25_000),
        ListeningSession(songId = 2, songName = "B", artistName = "", playlistId = 6, songLength = 0,
            startTimeMs = at(2026, 6, 15, 10), durationMs = 7_000),
        ListeningSession(songId = 1, songName = "A", artistName = "X", songLength = 100_000,
            startTimeMs = at(2026, 6, 16, 10), durationMs = 50_000),
        ListeningSession(songId = 1, songName = "A", artistName = "X", playlistId = 5, playlistName = "P",
            songLength = 100_000, startTimeMs = at(2026, 6, 17, 10), durationMs = 90_000),
    )

    @Test
    fun computeMatchesAndroidFormulas() {
        val s = StatsComputer.compute(sessions, now)

        assertEquals(172_000, s.totalListenMs) // playlist-only row excluded
        assertEquals(90_000, s.todayListenMs)
        assertEquals(147_000, s.thisWeekListenMs)
        assertEquals(147_000, s.thisMonthListenMs)
        assertEquals(1, s.listensToday)
        assertEquals(3, s.listensThisMonth)
        assertEquals(4, s.totalSessions)
        assertEquals(43_000, s.averageSessionMs)
        assertEquals(DailyStat(dayStart(now), 90_000), s.mostActiveDay)
        assertEquals(3, s.currentStreak)
        assertEquals(4, s.longestStreak) // quirk: number of distinct listening days
        assertEquals(43_000, s.averageDailyListenMs)
        assertEquals(200.0 / 3, s.completionRate, 1e-9) // per chunk, songLength 0 excluded
        assertTrue(s.hasAnyListenTime)

        assertEquals(14, s.dailySeries.size)
        assertEquals(DailyStat(dayStart(now), 90_000), s.dailySeries.last())
        assertEquals(50_000, s.dailySeries[12].totalMs)
        assertEquals(7_000, s.dailySeries[11].totalMs)

        assertEquals(listOf(3, 2, 1), s.topSongsAllTime.map { it.songId })
        assertEquals(listOf(2, 1, 1), s.topSongsAllTime.map { it.fullPlays })
        assertEquals("", s.topSongsAllTime[1].artistName) // blank artist kept on song stats
        assertEquals(listOf(2, 1), s.topSongsThisMonth.map { it.songId })

        assertEquals(listOf("X", "Y", "Unknown Artist"), s.topArtists.map { it.name })
        assertEquals(listOf(140_000L, 25_000L, 7_000L), s.topArtists.map { it.totalMs })

        assertEquals(listOf("P", "Playlist #6"), s.topPlaylists.map { it.name })
        assertEquals(listOf(90_000L, 7_000L), s.topPlaylists.map { it.totalMs })
        assertEquals(listOf(1, 1), s.topPlaylists.map { it.sessionCount })
    }

    @Test
    fun emptySessionsGiveDefaultSnapshot() {
        assertEquals(StatsSnapshot(), StatsComputer.compute(emptyList(), now))
    }

    @Test
    fun legacyBackfillRunsOnce() = runBlocking {
        val dir = Files.createTempDirectory("musitify-stats-test").toFile()
        val db = MusitifyDatabase.build(dir.resolve("t.db"))
        try {
            db.songDao().addSong(
                Song(name = "s", artist = "a", length = 1000, filePath = "f", artPath = "g",
                    timeHeard = 5000, downloadedAt = 1, url = "u")
            )
            db.playlistDao().addPlaylist(Playlist(name = "p", timeHeard = 3000, createdAt = 1))
            val repo = StatsRepository(db.listeningSessionDao(), db.songDao(), db.playlistDao(), SettingsRepository(db.settingsDao()))
            repo.backfillLegacyListenTime()
            repo.backfillLegacyListenTime()
            val rows = repo.sessions.first()
            assertEquals(2, rows.size)
            assertTrue(rows.all { it.startTimeMs == 0L })
            assertEquals(setOf(5000L, 3000L), rows.map { it.durationMs }.toSet())
            assertEquals(1L, repo.totalCount.first())
        } finally {
            db.close(); dir.deleteRecursively()
        }
    }
}
