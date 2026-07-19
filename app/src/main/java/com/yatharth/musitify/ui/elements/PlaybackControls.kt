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
import androidx.compose.material.icons.outlined.ArrowBackIosNew
import androidx.compose.material.icons.outlined.ArrowForwardIos
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.RepeatOne
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
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
    onRepeatToggle: () -> Unit
) {

    val repeatIcon = when (uiState.repeatMode) {
        0, 1 -> Icons.Outlined.Repeat
        2 -> Icons.Outlined.RepeatOne
        else -> Icons.Outlined.Repeat
    }

    Column(
        modifier = modifier.padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        ShadowBox(
//            modifier = Modifier.padding(8.dp)
        ) {
            FillBar(
                modifier = Modifier.padding(8.dp),
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
        }

        Log.d(
            "PlaybackControls",
            "value = ${uiState.positionMs.toDouble()} max = ${uiState.durationMs.toDouble()}"
        )

        ShadowBox(
//            modifier = Modifier.padding(8.dp)
        ) {
            Row(

            ) {
                CustomCircularButton(
                    modifier = Modifier.weight(1f),
                    onClick = onBack,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ArrowBackIosNew,
                        contentDescription = null,
                        tint = cardBorder
                    )
                }

                CustomCircularButton(
                    modifier = Modifier.weight(1f),
                    onClick = onPause,
                ) {
                    Icon(
                        imageVector = if (uiState.isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                        contentDescription = null,
                        tint = cardBorder
                    )
                }

                CustomCircularButton(
                    modifier = Modifier.weight(1f),
                    onClick = onNext,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
                        contentDescription = null,
                        tint = cardBorder
                    )
                }

                CustomCircularButton(
                    modifier = Modifier.weight(1f),
                    onClick = onRepeatToggle,
                ) {
                    Icon(
                        imageVector = repeatIcon,
                        contentDescription = null,
                        tint = if (uiState.repeatMode == 0) cardBorder.copy(alpha = 0.5f) else cardBorder
                    )
                }
            }
        }
    }
}