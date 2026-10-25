package com.yatharth.musitify.ui.screens

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yatharth.musitify.data.repository.ArtistStat
import com.yatharth.musitify.data.repository.DailyStat
import com.yatharth.musitify.data.repository.PlaylistStat
import com.yatharth.musitify.data.repository.SongStat
import com.yatharth.musitify.data.repository.StatsSnapshot
import com.yatharth.musitify.data.viewmodel.PlaybackControllerViewModel
import com.yatharth.musitify.data.viewmodel.StatsViewModel
import com.yatharth.musitify.ui.elements.ArtImage
import com.yatharth.musitify.ui.elements.CustomCircularButton
import com.yatharth.musitify.ui.elements.FillBar
import com.yatharth.musitify.ui.elements.PlaybackControls
import com.yatharth.musitify.ui.elements.ShadowBox
import com.yatharth.musitify.ui.elements.SleepTimerDialog
import com.yatharth.musitify.ui.theme.accentColor1
import com.yatharth.musitify.ui.theme.accentColor2
import com.yatharth.musitify.ui.theme.accentColor3
import com.yatharth.musitify.ui.theme.background
import com.yatharth.musitify.ui.theme.cardBorder
import com.yatharth.musitify.ui.theme.itemsBg
import com.yatharth.musitify.ui.theme.text
import com.yatharth.musitify.util.Formatter
import org.koin.compose.viewmodel.koinViewModel
import java.util.Calendar

/** At or above this content width the cards flow into a 2-column grid. */
private val TwoColumnMinWidth = 760.dp

/** Content is capped and centred on very wide windows. */
private val MaxContentWidth = 1200.dp

/**
 * "Spotify Wrapped"-style stats (1:1 with Android: same cards, strings and
 * formulas). Desktop: Esc closes (Android BackHandler), a close button sits next
 * to the header, wide windows use a 2-column grid, and the sleep-timer button
 * opens the sleep timer dialog (Android left it a no-op here).
 */
@Composable
fun StatsScreen(
    modifier: Modifier = Modifier,
    onClose: () -> Unit = {},
    playbackControllerViewModel: PlaybackControllerViewModel = koinViewModel(),
    statsViewModel: StatsViewModel = koinViewModel()
) {
    val uiState by playbackControllerViewModel.uiState.collectAsState()
    val stats by statsViewModel.stats.collectAsState()
    val sleepTimerState by playbackControllerViewModel.sleepTimerState.collectAsState()
    var showSleepTimerDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { statsViewModel.refresh() }

    // BackHandler(onBack = onClose) → Esc.
    val currentOnClose by rememberUpdatedState(onClose)
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .onPreviewKeyEvent { event ->
                if (event.key == Key.Escape && event.type == KeyEventType.KeyDown && !showSleepTimerDialog) {
                    currentOnClose()
                    true
                } else false
            }
            .focusRequester(focusRequester)
            .focusable(),
        containerColor = background,
        bottomBar = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.End
            ) {
                PlaybackControls(
                    modifier = Modifier.fillMaxWidth(),
                    uiState = uiState,
                    onBack = { playbackControllerViewModel.previous() },
                    onPause = { playbackControllerViewModel.pause() },
                    onNext = { playbackControllerViewModel.next() },
                    onRepeatToggle = { playbackControllerViewModel.toggleRepeatMode() },
                    onShuffleToggle = { playbackControllerViewModel.toggleShuffleMode() },
                    addSong = {},
                    createPlaylist = {},
                    onStats = onClose,
                    onSleepTimer = { showSleepTimerDialog = true },
                    sleepTimerActive = sleepTimerState.isActive,
                    onSeek = { playbackControllerViewModel.seekTo(it) }
                )
            }
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            val columns = if (maxWidth >= TwoColumnMinWidth) 2 else 1
            val gridState = rememberLazyGridState()

            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                state = gridState,
                modifier = Modifier
                    .widthIn(max = MaxContentWidth)
                    .fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                fullWidth("header") { StatsHeader(onClose) }

                if (!stats.hasAnyListenTime) {
                    fullWidth("empty") { EmptyStatsCard() }
                } else {
                    // Wide: hero + insights side by side; narrow: stacked full width.
                    item(key = "hero") { HeroTotalsCard(stats) }
                    item(key = "insights") { InsightsCardsRow(stats) }
                    fullWidth("periods") { PeriodCardsRow(stats) }
                    if (stats.dailySeries.any { it.totalMs > 0 }) {
                        fullWidth("chart") { ListeningChartCard(stats.dailySeries) }
                    }
                    item(key = "top_month") {
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
                    item(key = "top_all") {
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
                    item(key = "top_artists") {
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
                    item(key = "top_playlists") {
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

            VerticalScrollbar(
                adapter = rememberScrollbarAdapter(gridState),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .padding(vertical = 4.dp, horizontal = 2.dp)
            )
        }
    }

    if (showSleepTimerDialog) {
        SleepTimerDialog(
            sleepTimerState = sleepTimerState,
            onSetTimer = { playbackControllerViewModel.startSleepTimer(it) },
            onCancelTimer = { playbackControllerViewModel.cancelSleepTimer() },
            onDismiss = { showSleepTimerDialog = false }
        )
    }
}

private fun LazyGridScope.fullWidth(key: String, content: @Composable () -> Unit) {
    item(key = key, span = { GridItemSpan(maxLineSpan) }) { content() }
}

@Composable
private fun StatsHeader(onClose: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "YOUR STATS",
            color = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
        CustomCircularButton(onClick = onClose) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = "Close stats",
                tint = cardBorder
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
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.5.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Play any song and your listening stats — total time, top songs, favourite playlists — will show up here, just like Spotify Wrapped.",
                color = text.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodySmall
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
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.5.sp
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = Formatter.formatListenTime(stats.totalListenMs),
                color = accentColor1,
                style = MaterialTheme.typography.titleMedium,
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
                    text = "${stats.totalSessions} sessions",
                    color = text.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "avg ${Formatter.formatListenTime(stats.averageSessionMs)}/session",
                    color = text.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodySmall
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
        InsightCard(
            modifier = Modifier.weight(1f),
            label = "STREAK",
            value = "${stats.currentStreak}",
            sub = "current · ${stats.longestStreak} longest",
            color = accentColor1,
            icon = "🔥"
        )
        InsightCard(
            modifier = Modifier.weight(1f),
            label = "COMPLETION",
            value = "${stats.completionRate.toInt()}%",
            sub = "songs finished",
            color = accentColor2,
            icon = "✓"
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
            sub = if (stats.listensToday > 0) "${stats.listensToday} sessions" else "no listens",
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
            sub = "${stats.listensThisMonth} sessions",
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
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraLight,
                    letterSpacing = 1.sp
                )
                Text(
                    text = icon,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                color = color,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = sub,
                color = text.copy(alpha = 0.5f),
                style = MaterialTheme.typography.labelSmall
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
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraLight,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                color = text,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .height(4.dp)
                    .width(28.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color)
            )
            sub?.let {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = it,
                    color = text.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.labelSmall
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
                style = MaterialTheme.typography.labelSmall,
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
                    color = text.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.labelSmall,
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
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.5.sp
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = text.copy(alpha = 0.6f),
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(12.dp))

            if (hasData) content()
            else {
                Text(
                    text = emptyLabel,
                    color = text.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun SongStatRow(index: Int, song: SongStat, maxMs: Long) {
    StatRowBase(
        rank = index,
        title = song.name,
        subtitle = song.artistName,
        valueLabel = "${Formatter.formatListenTime(song.totalMs)} · ${song.fullPlays} plays",
        shareValue = song.totalMs.toDouble(),
        shareMax = if (maxMs > 0) maxMs.toDouble() else 1.0,
        color = accentColor1
    ) {
        ArtImage(
            path = song.artPath,
            contentDescription = song.name,
            modifier = Modifier
                .size(40.dp)
                .aspectRatio(1f)
                .clip(RoundedCornerShape(3.dp))
        )
    }
}

@Composable
private fun ArtistStatRow(index: Int, artist: ArtistStat, maxMs: Long) {
    StatRowBase(
        rank = index,
        title = artist.name,
        subtitle = "${artist.songCount} songs",
        valueLabel = Formatter.formatListenTime(artist.totalMs),
        shareValue = artist.totalMs.toDouble(),
        shareMax = if (maxMs > 0) maxMs.toDouble() else 1.0,
        color = accentColor2
    ) {
        InitialsTile(artist.name, accentColor2.copy(alpha = 0.2f))
    }
}

@Composable
private fun PlaylistStatRow(index: Int, playlist: PlaylistStat, maxMs: Long) {
    StatRowBase(
        rank = index,
        title = playlist.name,
        subtitle = "${playlist.sessionCount} sessions",
        valueLabel = Formatter.formatListenTime(playlist.totalMs),
        shareValue = playlist.totalMs.toDouble(),
        shareMax = if (maxMs > 0) maxMs.toDouble() else 1.0,
        color = accentColor3
    ) {
        InitialsTile(playlist.name, accentColor3.copy(alpha = 0.25f))
    }
}

@Composable
private fun InitialsTile(name: String, bg: Color) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(bg),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name.take(2).uppercase(),
            color = text,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.ExtraBold
        )
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
            color = text.copy(alpha = 0.5f),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.width(28.dp)
        )

        leading()

        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                color = text.copy(alpha = 0.6f),
                style = MaterialTheme.typography.labelSmall,
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
