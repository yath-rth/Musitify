package com.yatharth.musitify.ui.elements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.outlined.ArrowBackIosNew
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.RepeatOne
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yatharth.musitify.data.repository.PlayerUiState
import com.yatharth.musitify.data.viewmodel.PlaybackControllerViewModel
import com.yatharth.musitify.ui.theme.cardBorder
import com.yatharth.musitify.util.Formatter.formatTime
import org.koin.compose.viewmodel.koinViewModel

/**
 * Bottom playback bar: labelled progress [FillBar] + 7 circular buttons
 * (previous, play/pause, next, repeat, shuffle, insights, sleep timer).
 * Same signature as Android (the `addSong`/`createPlaylist` button is commented
 * out there too).
 *
 * Desktop addition: the progress bar is seekable (click / drag). [onSeek] gets the
 * target position in ms; by default it calls [PlaybackControllerViewModel.seekTo]
 * on the Koin-provided ViewModel, so Android-style call sites get seeking for free.
 */
@Composable
fun PlaybackControls(
    modifier: Modifier,
    uiState: PlayerUiState,
    onBack: () -> Unit,
    onPause: () -> Unit,
    onNext: () -> Unit,
    addSong: () -> Unit,
    createPlaylist: () -> Unit,
    onRepeatToggle: () -> Unit,
    onShuffleToggle: () -> Unit,
    onStats: () -> Unit = {},
    onSleepTimer: () -> Unit = {},
    sleepTimerActive: Boolean = false,
    onSeek: (Long) -> Unit = defaultSeek(),
) {
    val repeatIcon = when (uiState.repeatMode) {
        1 -> Icons.Outlined.RepeatOne
        else -> Icons.Outlined.Repeat
    }
    val canSeek = uiState.durationMs > 0L

    ShadowBox(
        modifier = modifier.padding(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            FillBar(
                modifier = Modifier.padding(horizontal = 8.dp),
                label = uiState.currentTitle,
                value = uiState.positionMs.toDouble(),
                max = uiState.durationMs.toDouble(),
                secondaryLabel = if (uiState.durationMs != 0L)
                    "${formatTime(uiState.positionMs)} / ${formatTime(uiState.durationMs)}"
                else null,
                onSeek = if (canSeek) { fraction ->
                    onSeek((fraction * uiState.durationMs).toLong())
                } else null
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CustomCircularButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Outlined.ArrowBackIosNew,
                        contentDescription = null,
                        tint = cardBorder
                    )
                }

                CustomCircularButton(onClick = onPause) {
                    Icon(
                        imageVector = if (uiState.isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                        contentDescription = null,
                        tint = cardBorder
                    )
                }

                CustomCircularButton(onClick = onNext) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
                        contentDescription = null,
                        tint = cardBorder
                    )
                }

                CustomCircularButton(onClick = onRepeatToggle) {
                    Icon(
                        imageVector = repeatIcon,
                        contentDescription = null,
                        tint = if (uiState.repeatMode == 0) cardBorder.copy(alpha = 0.5f) else cardBorder
                    )
                }

                CustomCircularButton(onClick = onShuffleToggle) {
                    Icon(
                        imageVector = Icons.Outlined.Shuffle,
                        contentDescription = null,
                        tint = if (uiState.shuffleMode) cardBorder else cardBorder.copy(alpha = 0.5f)
                    )
                }

                CustomCircularButton(onClick = onStats) {
                    Icon(
                        imageVector = Icons.Outlined.Insights,
                        contentDescription = null,
                        tint = cardBorder
                    )
                }

                CustomCircularButton(onClick = onSleepTimer) {
                    Icon(
                        imageVector = Icons.Outlined.Bedtime,
                        contentDescription = null,
                        tint = if (sleepTimerActive) cardBorder else cardBorder.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

/** Seeks through the shared [PlaybackControllerViewModel] (state lives in singleton repositories). */
@Composable
private fun defaultSeek(): (Long) -> Unit {
    val viewModel = koinViewModel<PlaybackControllerViewModel>()
    return remember(viewModel) { { positionMs: Long -> viewModel.seekTo(positionMs) } }
}
