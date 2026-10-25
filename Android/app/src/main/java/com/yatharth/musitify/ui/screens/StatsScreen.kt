package com.yatharth.musitify.ui.screens

import com.yatharth.musitify.util.SongDisplay
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.yatharth.musitify.data.repository.ArtistStat
import com.yatharth.musitify.data.repository.DailyStat
import com.yatharth.musitify.data.repository.PlaylistStat
import com.yatharth.musitify.data.repository.SongStat
import com.yatharth.musitify.data.repository.StatsSnapshot
import com.yatharth.musitify.data.viewmodel.PlaybackControllerViewModel
import com.yatharth.musitify.data.viewmodel.StatsViewModel
import com.yatharth.musitify.ui.elements.FillBar
import com.yatharth.musitify.ui.elements.PlaybackControls
import com.yatharth.musitify.ui.elements.ShadowBox
import com.yatharth.musitify.ui.theme.accentColor1
import com.yatharth.musitify.ui.theme.accentColor2
import com.yatharth.musitify.ui.theme.accentColor3
import com.yatharth.musitify.ui.theme.adaptiveTypography
import com.yatharth.musitify.ui.theme.background
import com.yatharth.musitify.ui.theme.cardBorder
import com.yatharth.musitify.ui.theme.itemsBg
import com.yatharth.musitify.ui.theme.text
import com.yatharth.musitify.ui.theme.textSecondary
import com.yatharth.musitify.util.Formatter
import java.io.File
import java.util.Calendar

/**
 * The Stats tab. Rendered inside the home screen's frame, so it shares the
 * header tabs and the mini player instead of drawing its own copy of the bar.
 */
@Composable
fun StatsContent(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    statsViewModel: StatsViewModel = hiltViewModel()
) {
    val stats by statsViewModel.stats.collectAsState()

    LaunchedEffect(Unit) { statsViewModel.refresh() }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 12.dp,
            end = 12.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 8.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (!stats.hasAnyListenTime) {
            item { EmptyStatsCard() }
        } else if (stats.isEarly) {
            // Rows of zeros and a one-bar chart read as failure; show a simpler
            // view until there's enough history for trends.
            item { GettingStartedCard(stats) }
            item { StreakCard(stats) }
            item {
                TopListCard(
                    title = "MOST PLAYED SO FAR",
                    subtitle = "your top songs fill in as you listen",
                    emptyLabel = "Keep listening to fill this up",
                    hasData = stats.topSongsAllTime.isNotEmpty()
                ) {
                    stats.topSongsAllTime.forEachIndexed { index, song ->
                        SongStatRow(index, song, stats.topSongsAllTime.firstOrNull()?.totalMs ?: 0)
                    }
                }
            }
        } else {
            item { HeroTotalsCard(stats) }
            item { InsightsCardsRow(stats) }
            item { PeriodCardsRow(stats) }
            if (stats.dailySeries.any { it.totalMs > 0 }) {
                item { ListeningChartCard(stats.dailySeries) }
            }
            item {
                TopListCard(
                    title = "TOP SONGS · THIS MONTH",
                    subtitle = "your most-played tracks lately",
                    emptyLabel = "No listens this month yet",
                    hasData = stats.topSongsThisMonth.isNotEmpty()
                ) {
                    stats.topSongsThisMonth.forEachIndexed { index, song ->
                        SongStatRow(index, song, stats.topSongsThisMonth.firstOrNull()?.totalMs ?: 0)
                    }
                }
            }
            item {
                TopListCard(
                    title = "TOP SONGS · ALL TIME",
                    subtitle = "your most-played tracks ever",
                    emptyLabel = "Keep listening to fill this up",
                    hasData = stats.topSongsAllTime.isNotEmpty()
                ) {
                    stats.topSongsAllTime.forEachIndexed { index, song ->
                        SongStatRow(index, song, stats.topSongsAllTime.firstOrNull()?.totalMs ?: 0)
                    }
                }
            }
            item {
                TopListCard(
                    title = "TOP ARTISTS",
                    subtitle = "artists you listen to the most",
                    emptyLabel = "No artist data yet",
                    hasData = stats.topArtists.isNotEmpty()
                ) {
                    stats.topArtists.forEachIndexed { index, artist ->
                        ArtistStatRow(index, artist, stats.topArtists.firstOrNull()?.totalMs ?: 0)
                    }
                }
            }
            item {
                TopListCard(
                    title = "TOP PLAYLISTS",
                    subtitle = "playlists you play the most",
                    emptyLabel = "Play a playlist to see its stats here",
                    hasData = stats.topPlaylists.isNotEmpty()
                ) {
                    stats.topPlaylists.forEachIndexed { index, playlist ->
                        PlaylistStatRow(index, playlist, stats.topPlaylists.firstOrNull()?.totalMs ?: 0)
                    }
                }
            }
        }
    }
}

/** Less than 3 days with listening, or under an hour in total. */
private val StatsSnapshot.isEarly: Boolean
    get() = dailySeries.count { it.totalMs > 0 } < 3 || totalListenMs < 60 * 60_000L

@Composable
private fun GettingStartedCard(stats: StatsSnapshot) {
    ShadowBox(modifier = Modifier.fillMaxWidth(), alignment = Alignment.TopStart) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "GETTING STARTED",
                color = text,
                style = adaptiveTypography().labelSmall,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "${Formatter.formatListenTime(stats.totalListenMs)} listened",
                color = accentColor1,
                style = adaptiveTypography().titleMedium
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Across ${Formatter.count(stats.totalSessions, "session")}. Trends, your top artists " +
                        "and playlists show up here after a few days of listening.",
                color = textSecondary,
                style = adaptiveTypography().bodySmall
            )
        }
    }
}

/** Current streak plus the last seven days as filled / empty dots. */
@Composable
private fun StreakCard(stats: StatsSnapshot, modifier: Modifier = Modifier.fillMaxWidth()) {
    val lastWeek = stats.dailySeries.takeLast(7)
    ShadowBox(modifier = modifier, alignment = Alignment.CenterStart) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "STREAK",
                color = textSecondary,
                style = adaptiveTypography().labelSmall
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = Formatter.count(stats.currentStreak, "day"),
                color = text,
                style = adaptiveTypography().headlineMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                lastWeek.forEach { day ->
                    val listened = day.totalMs > 0
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (listened) accentColor1 else Color.Transparent)
                            .border(1.5.dp, cardBorder, RoundedCornerShape(3.dp))
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = "last 7 days · best ${stats.longestStreak}",
                color = textSecondary,
                style = adaptiveTypography().labelSmall
            )
        }
    }
}

@Composable
private fun EmptyStatsCard() {
    ShadowBox(
        modifier = Modifier.fillMaxWidth(),
        alignment = Alignment.TopStart
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "NO LISTENING HISTORY YET",
                color = text,
                style = adaptiveTypography().labelSmall,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.5.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Play any song and your listening time, top songs and favourite playlists show up here.",
                color = textSecondary,
                style = adaptiveTypography().bodySmall
            )
        }
    }
}

@Composable
private fun HeroTotalsCard(stats: StatsSnapshot) {
    ShadowBox(
        modifier = Modifier.fillMaxWidth(),
        bgColor = itemsBg,
        alignment = Alignment.TopStart
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "ALL-TIME LISTENING",
                color = text,
                style = adaptiveTypography().labelSmall,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.5.sp
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = Formatter.formatListenTime(stats.totalListenMs),
                color = accentColor1,
                style = adaptiveTypography().titleMedium,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = Formatter.count(stats.totalSessions, "session"),
                    color = textSecondary,
                    style = adaptiveTypography().bodySmall
                )
                Text(
                    text = "avg ${Formatter.formatListenTime(stats.averageSessionMs)}/session",
                    color = textSecondary,
                    style = adaptiveTypography().bodySmall
                )
            }
        }
    }
}

@Composable
private fun InsightsCardsRow(stats: StatsSnapshot) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StreakCard(stats, modifier = Modifier.weight(1f))
        InsightCard(
            modifier = Modifier.weight(1f),
            label = "COMPLETION",
            value = "${stats.completionRate.toInt()}%",
            sub = "songs finished",
            color = text,
            icon = ""
        )
    }
}

@Composable
private fun PeriodCardsRow(stats: StatsSnapshot) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        PeriodCard(
            modifier = Modifier.weight(1f),
            label = "TODAY",
            value = Formatter.formatListenTime(stats.todayListenMs),
            sub = if (stats.listensToday > 0) Formatter.count(stats.listensToday, "session") else "no listens",
            color = accentColor1
        )
        PeriodCard(
            modifier = Modifier.weight(1f),
            label = "THIS WEEK",
            value = Formatter.formatListenTime(stats.thisWeekListenMs),
            sub = "avg ${Formatter.formatListenTime(stats.thisWeekListenMs / 7)}/day",
            color = accentColor2
        )
        PeriodCard(
            modifier = Modifier.weight(1f),
            label = "THIS MONTH",
            value = Formatter.formatListenTime(stats.thisMonthListenMs),
            sub = Formatter.count(stats.listensThisMonth, "session"),
            color = accentColor3
        )
    }
}

@Composable
private fun InsightCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    sub: String,
    color: Color,
    icon: String
) {
    ShadowBox(
        modifier = modifier,
        alignment = Alignment.CenterStart
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = label,
                    color = text.copy(alpha = 0.7f),
                    style = adaptiveTypography().labelSmall,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = 1.sp
                )
                if (icon.isNotEmpty()) {
                    Text(
                        text = icon,
                        style = adaptiveTypography().bodyMedium
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                color = color,
                style = adaptiveTypography().headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = sub,
                color = textSecondary,
                style = adaptiveTypography().labelSmall
            )
        }
    }
}

@Composable
private fun PeriodCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    sub: String?,
    color: Color
) {
    ShadowBox(
        modifier = modifier,
        alignment = Alignment.CenterStart
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = label,
                color = text.copy(alpha = 0.7f),
                style = adaptiveTypography().labelSmall,
                fontWeight = FontWeight.Normal,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                color = text,
                style = adaptiveTypography().bodyLarge,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            sub?.let {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = it,
                    color = textSecondary,
                    style = adaptiveTypography().labelSmall
                )
            }
        }
    }
}

@Composable
private fun ListeningChartCard(series: List<DailyStat>) {
    ShadowBox(
        modifier = Modifier.fillMaxWidth(),
        alignment = Alignment.TopStart
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "LISTENING · LAST ${series.size} DAYS",
                color = text,
                style = adaptiveTypography().labelSmall,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.5.sp
            )
            Spacer(Modifier.height(12.dp))
            ListeningBarChart(series)
        }
    }
}

@Composable
private fun ListeningBarChart(series: List<DailyStat>) {
    val max = series.maxOfOrNull { it.totalMs } ?: 0L

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        series.forEach { day ->
            val fraction = if (max > 0) (day.totalMs.toFloat() / max).coerceIn(0.03f, 1f) else 0.03f
            val hasListen = day.totalMs > 0

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(96.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(fraction)
                            .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                            .background(if (hasListen) accentColor1 else cardBorder.copy(alpha = 0.25f))
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = Formatter.formatDayNumber(dayOfMonth(day.dayStartMs)),
                    color = textSecondary,
                    style = adaptiveTypography().labelSmall,
                    fontSize = 9.sp
                )
            }
        }
    }
}

@Composable
private fun TopListCard(
    title: String,
    subtitle: String,
    emptyLabel: String,
    hasData: Boolean,
    content: @Composable () -> Unit
) {
    ShadowBox(
        modifier = Modifier.fillMaxWidth(),
        alignment = Alignment.TopStart
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                color = text,
                style = adaptiveTypography().labelSmall,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.5.sp
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = textSecondary,
                style = adaptiveTypography().bodySmall
            )
            Spacer(Modifier.height(12.dp))

            if (hasData) content()
            else {
                Text(
                    text = emptyLabel,
                    color = textSecondary,
                    style = adaptiveTypography().bodySmall
                )
            }
        }
    }
}

@Composable
private fun SongStatRow(index: Int, song: SongStat, maxMs: Long) {
    StatRowBase(
        rank = index,
        title = SongDisplay.title(song.name, song.artistName),
        subtitle = song.artistName,
        valueLabel = "${Formatter.formatListenTime(song.totalMs)} · ${Formatter.count(song.fullPlays, "play")}",
        shareValue = song.totalMs.toDouble(),
        shareMax = if (maxMs > 0) maxMs.toDouble() else 1.0,
        color = accentColor1
    ) {
        AsyncImage(
            model = File(song.artPath.orEmpty()),
            contentDescription = song.name,
            modifier = Modifier
                .size(40.dp)
                .aspectRatio(1f)
                .clip(RoundedCornerShape(3.dp)),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
private fun ArtistStatRow(index: Int, artist: ArtistStat, maxMs: Long) {
    StatRowBase(
        rank = index,
        title = artist.name,
        subtitle = Formatter.count(artist.songCount, "song"),
        valueLabel = Formatter.formatListenTime(artist.totalMs),
        shareValue = artist.totalMs.toDouble(),
        shareMax = if (maxMs > 0) maxMs.toDouble() else 1.0,
        color = accentColor1
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(accentColor2.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = artist.name.take(2).uppercase(),
                color = text,
                style = adaptiveTypography().bodySmall,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
private fun PlaylistStatRow(index: Int, playlist: PlaylistStat, maxMs: Long) {
    StatRowBase(
        rank = index,
        title = playlist.name,
        subtitle = Formatter.count(playlist.sessionCount, "session"),
        valueLabel = Formatter.formatListenTime(playlist.totalMs),
        shareValue = playlist.totalMs.toDouble(),
        shareMax = if (maxMs > 0) maxMs.toDouble() else 1.0,
        color = accentColor1
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(accentColor3.copy(alpha = 0.25f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = playlist.name.take(2).uppercase(),
                color = text,
                style = adaptiveTypography().bodySmall,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
private fun StatRowBase(
    rank: Int,
    title: String,
    subtitle: String,
    valueLabel: String,
    shareValue: Double,
    shareMax: Double,
    color: Color,
    leading: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${rank + 1}",
            color = textSecondary,
            style = adaptiveTypography().labelLarge,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.width(28.dp)
        )

        leading()

        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = text,
                style = adaptiveTypography().bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                color = textSecondary,
                style = adaptiveTypography().labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            FillBar(
                value = shareValue,
                max = shareMax,
                color = color,
                secondaryLabel = valueLabel
            )
        }
    }
}

private fun dayOfMonth(ms: Long): Long =
    Calendar.getInstance().apply { timeInMillis = ms }.get(Calendar.DAY_OF_MONTH).toLong()