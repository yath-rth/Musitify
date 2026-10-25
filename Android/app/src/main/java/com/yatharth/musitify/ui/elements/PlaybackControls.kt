package com.yatharth.musitify.ui.elements

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBackIosNew
import androidx.compose.material.icons.outlined.ArrowForwardIos
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.RepeatOne
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yatharth.musitify.data.repository.PlayerUiState
import com.yatharth.musitify.ui.theme.cardBorder
import com.yatharth.musitify.ui.theme.itemsBg
import com.yatharth.musitify.ui.theme.designSystemButton
import com.yatharth.musitify.ui.theme.designSystemShadowBox
import com.yatharth.musitify.util.Formatter.formatTime

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
    sleepTimerActive: Boolean = false
) {

    val repeatIcon = when (uiState.repeatMode) {
        0, 2 -> Icons.Outlined.Repeat
        1 -> Icons.Outlined.RepeatOne
        else -> Icons.Outlined.Repeat
    }
    val repeatOn = uiState.repeatMode != 0

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
                    "${formatTime(uiState.positionMs)} / ${
                        formatTime(
                            uiState.durationMs
                        )
                    }"
                else null
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CustomCircularButton(
                    onClick = onBack,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ArrowBackIosNew,
                        contentDescription = "Previous song",
                        tint = cardBorder
                    )
                }

                CustomCircularButton(
                    onClick = onPause,
                ) {
                    Icon(
                        imageVector = if (uiState.isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                        contentDescription = if (uiState.isPlaying) "Pause" else "Play",
                        tint = cardBorder
                    )
                }

                CustomCircularButton(
                    onClick = onNext,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
                        contentDescription = "Next song",
                        tint = cardBorder
                    )
                }

                CustomCircularButton(
                    onClick = onRepeatToggle,
                    selected = repeatOn,
                ) {
                    Icon(
                        imageVector = repeatIcon,
                        contentDescription = when (uiState.repeatMode) {
                            1 -> "Repeat one, on"
                            0 -> "Repeat, off"
                            else -> "Repeat all, on"
                        },
                        tint = toggleTint(repeatOn)
                    )
                }

                CustomCircularButton(
                    onClick = onShuffleToggle,
                    selected = uiState.shuffleMode,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Shuffle,
                        contentDescription = if (uiState.shuffleMode) "Shuffle, on" else "Shuffle, off",
                        tint = toggleTint(uiState.shuffleMode)
                    )
                }

                CustomCircularButton(
                    onClick = onStats,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Insights,
                        contentDescription = "Listening stats",
                        tint = cardBorder
                    )
                }

                CustomCircularButton(
                    onClick = onSleepTimer,
                    selected = sleepTimerActive,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Bedtime,
                        contentDescription = if (sleepTimerActive) "Sleep timer, on" else "Sleep timer",
                        tint = toggleTint(sleepTimerActive)
                    )
                }

//                CustomCircularButton(
//                    onClick = addSong,
//                    onLongClick = createPlaylist
//                ) {
//                    Icon(
//                        imageVector = Icons.Outlined.Add,
//                        contentDescription = null,
//                        tint = cardBorder
//                    )
//                }
            }
        }
    }
}

/** Icon colour for a toggle: white on the filled ink background when on. */
private fun toggleTint(on: Boolean) = if (on) itemsBg else cardBorder
