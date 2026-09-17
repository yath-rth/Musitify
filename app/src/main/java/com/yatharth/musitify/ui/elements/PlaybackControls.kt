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
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.RepeatOne
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yatharth.musitify.data.repository.PlayerUiState
import com.yatharth.musitify.ui.theme.cardBorder
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
    onStats: () -> Unit = {}
) {

    val repeatIcon = when (uiState.repeatMode) {
        0, 2 -> Icons.Outlined.Repeat
        1 -> Icons.Outlined.RepeatOne
        else -> Icons.Outlined.Repeat
    }

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
                        contentDescription = null,
                        tint = cardBorder
                    )
                }

                CustomCircularButton(
                    onClick = onPause,
                ) {
                    Icon(
                        imageVector = if (uiState.isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                        contentDescription = null,
                        tint = cardBorder
                    )
                }

                CustomCircularButton(
                    onClick = onNext,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
                        contentDescription = null,
                        tint = cardBorder
                    )
                }

                CustomCircularButton(
                    onClick = onRepeatToggle,
                ) {
                    Icon(
                        imageVector = repeatIcon,
                        contentDescription = null,
                        tint = if (uiState.repeatMode == 0) cardBorder.copy(alpha = 0.5f) else cardBorder
                    )
                }

                CustomCircularButton(
                    onClick = onStats,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Insights,
                        contentDescription = null,
                        tint = cardBorder
                    )
                }

                CustomCircularButton(
                    onClick = addSong,
                    onLongClick = createPlaylist
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = null,
                        tint = cardBorder
                    )
                }
            }
        }
    }
}