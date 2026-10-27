package com.yatharth.musitify.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.outlined.ArrowBackIosNew
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.RepeatOne
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yatharth.musitify.data.entity.Song
import com.yatharth.musitify.data.repository.PlayerUiState
import com.yatharth.musitify.engine.SleepTimerState
import com.yatharth.musitify.ui.elements.CustomButton
import com.yatharth.musitify.ui.elements.CustomCircularButton
import com.yatharth.musitify.ui.elements.MusicRecord
import com.yatharth.musitify.ui.elements.ShadowBox
import com.yatharth.musitify.ui.elements.handCursor
import com.yatharth.musitify.ui.elements.toggleTint
import com.yatharth.musitify.ui.theme.accentColor1
import com.yatharth.musitify.ui.theme.cardBorder
import com.yatharth.musitify.ui.theme.dotGridBackground
import com.yatharth.musitify.ui.theme.itemsBg
import com.yatharth.musitify.ui.theme.secondaryBg
import com.yatharth.musitify.ui.theme.text
import com.yatharth.musitify.ui.theme.textSecondary
import com.yatharth.musitify.util.Formatter
import com.yatharth.musitify.util.SongDisplay
import kotlinx.coroutines.launch
import kotlin.math.ceil

/** The player column stays phone-shaped instead of stretching across a wide window. */
private val PlayerMaxWidth = 560.dp

/**
 * Full-screen player, opened from the mini player. The record spins while
 * playing and coasts to a stop on pause, so it shows playback state at a glance.
 * Drag down, press Esc or the close button to close.
 */
@Composable
fun NowPlayingScreen(
    song: Song?,
    uiState: PlayerUiState,
    sleepTimerState: SleepTimerState,
    onClose: () -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onRepeatToggle: () -> Unit,
    onShuffleToggle: () -> Unit,
    onSleepTimer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val dismissThresholdPx = with(density) { 140.dp.toPx() }
    val dragOffset = remember { Animatable(0f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { translationY = dragOffset.value }
            .dotGridBackground()
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        scope.launch {
                            dragOffset.snapTo((dragOffset.value + dragAmount).coerceAtLeast(0f))
                        }
                    },
                    onDragEnd = {
                        if (dragOffset.value > dismissThresholdPx) onClose()
                        else scope.launch { dragOffset.animateTo(0f) }
                    },
                    onDragCancel = { scope.launch { dragOffset.animateTo(0f) } }
                )
            },
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = PlayerMaxWidth)
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ---- Header ----
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CustomButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Outlined.KeyboardArrowDown,
                        contentDescription = "Close player",
                        tint = cardBorder,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(28.dp)
                    )
                }
                Text(
                    text = "NOW PLAYING",
                    color = textSecondary,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                // Balances the close button so the label sits in the centre.
                Spacer(Modifier.width(52.dp))
            }

            // ---- Record ----
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                ShadowBox(modifier = Modifier.aspectRatio(1f)) {
                    SpinningRecord(
                        song = song,
                        playing = uiState.isPlaying,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )
                }
            }

            // ---- Title ----
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = song?.let { SongDisplay.title(it) } ?: uiState.currentTitle.orEmpty(),
                    color = text,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                song?.let { SongDisplay.artist(it) }?.let {
                    Text(
                        text = it,
                        color = textSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            SeekBar(
                positionMs = uiState.positionMs,
                durationMs = uiState.durationMs,
                onSeek = onSeek
            )

            Spacer(Modifier.height(20.dp))

            // ---- Transport ----
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ToggleButton(
                    on = uiState.shuffleMode,
                    onClick = onShuffleToggle,
                    description = if (uiState.shuffleMode) "Shuffle, on" else "Shuffle, off"
                ) { tint ->
                    Icon(Icons.Outlined.Shuffle, contentDescription = null, tint = tint)
                }

                CustomCircularButton(onClick = onPrevious) {
                    Icon(
                        imageVector = Icons.Outlined.ArrowBackIosNew,
                        contentDescription = "Previous song",
                        tint = cardBorder,
                        modifier = Modifier
                            .padding(6.dp)
                            .size(26.dp)
                    )
                }

                // The main action: bigger and filled.
                CustomCircularButton(onClick = onPlayPause, selected = true) {
                    Icon(
                        imageVector = if (uiState.isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                        contentDescription = if (uiState.isPlaying) "Pause" else "Play",
                        tint = itemsBg,
                        modifier = Modifier
                            .padding(12.dp)
                            .size(40.dp)
                    )
                }

                CustomCircularButton(onClick = onNext) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
                        contentDescription = "Next song",
                        tint = cardBorder,
                        modifier = Modifier
                            .padding(6.dp)
                            .size(26.dp)
                    )
                }

                val repeatOn = uiState.repeatMode != 0
                ToggleButton(
                    on = repeatOn,
                    onClick = onRepeatToggle,
                    description = when (uiState.repeatMode) {
                        1 -> "Repeat one, on"
                        0 -> "Repeat, off"
                        else -> "Repeat all, on"
                    }
                ) { tint ->
                    Icon(
                        imageVector = if (uiState.repeatMode == 1) Icons.Outlined.RepeatOne else Icons.Outlined.Repeat,
                        contentDescription = null,
                        tint = tint
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // ---- Sleep timer: shows time left instead of just a darker icon ----
            CustomButton(
                onClick = onSleepTimer,
                bgColor = if (sleepTimerState.isActive) cardBorder else itemsBg
            ) {
                val tint = if (sleepTimerState.isActive) itemsBg else cardBorder
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Outlined.Bedtime, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
                    Text(
                        text = when {
                            sleepTimerState.isActive && sleepTimerState.endOfSong -> "SLEEP · END OF SONG"
                            sleepTimerState.isActive -> "SLEEP · ${sleepMinutesLeft(sleepTimerState.remainingMs)} LEFT"
                            else -> "SLEEP TIMER"
                        },
                        color = tint,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

/** "42 MIN", rounded up so the last minute never reads as 0. */
private fun sleepMinutesLeft(remainingMs: Long): String =
    "${ceil(remainingMs / 60000.0).toLong().coerceAtLeast(1)} MIN"

@Composable
private fun ToggleButton(
    on: Boolean,
    onClick: () -> Unit,
    description: String,
    icon: @Composable (tint: Color) -> Unit,
) {
    CustomCircularButton(
        onClick = onClick,
        selected = on,
        modifier = Modifier.semantics { contentDescription = description }
    ) {
        icon(toggleTint(on))
    }
}

/** Spins at a steady speed while playing and coasts to a stop on pause. */
@Composable
private fun SpinningRecord(song: Song?, playing: Boolean, modifier: Modifier = Modifier) {
    val rotation = remember { Animatable(0f) }

    LaunchedEffect(playing) {
        if (playing) {
            while (true) {
                rotation.snapTo(rotation.value % 360f)
                rotation.animateTo(
                    targetValue = rotation.value + 360f,
                    animationSpec = tween(durationMillis = 8000, easing = LinearEasing)
                )
            }
        } else {
            rotation.animateTo(
                targetValue = rotation.value + 24f,
                animationSpec = tween(durationMillis = 700, easing = LinearOutSlowInEasing)
            )
        }
    }

    MusicRecord(
        artPath = song?.artPath,
        modifier = modifier,
        rotationDegrees = rotation.value,
        labelFraction = 0.52f
    )
}

/**
 * Draggable progress bar in the app's outlined style. Click to jump, or drag
 * the handle; the time label follows the drag before the seek is applied.
 */
@Composable
private fun SeekBar(positionMs: Long, durationMs: Long, onSeek: (Long) -> Unit) {
    var dragFraction by remember { mutableStateOf<Float?>(null) }
    val playedFraction = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    val fraction = dragFraction ?: playedFraction
    val enabled = durationMs > 0

    Column(modifier = Modifier.fillMaxWidth()) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .then(if (enabled) Modifier.handCursor() else Modifier)
                .semantics {
                    contentDescription = "Seek"
                    progressBarRangeInfo = ProgressBarRangeInfo(fraction, 0f..1f)
                    if (enabled) {
                        setProgress { target ->
                            onSeek((target.coerceIn(0f, 1f) * durationMs).toLong())
                            true
                        }
                    }
                }
                .pointerInput(durationMs) {
                    if (!enabled) return@pointerInput
                    detectTapGestures { offset ->
                        onSeek(((offset.x / size.width).coerceIn(0f, 1f) * durationMs).toLong())
                    }
                }
                .pointerInput(durationMs) {
                    if (!enabled) return@pointerInput
                    detectHorizontalDragGestures(
                        onDragStart = { dragFraction = (it.x / size.width).coerceIn(0f, 1f) },
                        onDragEnd = {
                            dragFraction?.let { onSeek((it * durationMs).toLong()) }
                            dragFraction = null
                        },
                        onDragCancel = { dragFraction = null },
                        onHorizontalDrag = { change, _ ->
                            change.consume()
                            dragFraction = (change.position.x / size.width).coerceIn(0f, 1f)
                        }
                    )
                },
            contentAlignment = Alignment.CenterStart
        ) {
            val thumb = 22.dp
            val trackShape = RoundedCornerShape(3.dp)

            // Track with the app's hard offset shadow.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .offset(2.dp, 3.dp)
                    .background(cardBorder, trackShape)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .background(secondaryBg, trackShape)
                    .border(1.5.dp, cardBorder, trackShape)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .fillMaxHeight()
                        .background(accentColor1, trackShape)
                )
            }
            // Handle.
            Box(
                modifier = Modifier
                    .offset(x = (maxWidth - thumb) * fraction)
                    .size(thumb)
                    .background(itemsBg, RoundedCornerShape(4.dp))
                    .border(2.dp, cardBorder, RoundedCornerShape(4.dp))
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val shownMs = dragFraction?.let { (it * durationMs).toLong() } ?: positionMs
            Text(
                text = Formatter.formatTime(shownMs),
                color = textSecondary,
                style = MaterialTheme.typography.labelSmall
            )
            Text(
                text = if (durationMs > 0) Formatter.formatTime(durationMs) else "--:--",
                color = textSecondary,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
