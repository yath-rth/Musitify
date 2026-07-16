package com.yatharth.musitify.ui.elements

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.yatharth.musitify.data.repository.PlayerUiState
import com.yatharth.musitify.ui.theme.designSystemButton
import com.yatharth.musitify.ui.theme.designSystemShadowBox

@Composable
fun PlaybackControls(
    modifier: Modifier,
    uiState: PlayerUiState,
    onBack: () -> Unit,
    onPause: () -> Unit,
    onNext: () -> Unit
){
    designSystemShadowBox(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        Column(

        ){
            FillBar(
                value = uiState.positionMs.toDouble(),
                max = uiState.durationMs.toDouble()
            )

            Row(

            ) {
                designSystemButton(
                    modifier = Modifier.weight(1f),
                    onClick = onBack,
                    text = "Back"
                )

                designSystemButton(
                    modifier = Modifier.weight(1f),
                    onClick = onPause,
                    text = if (uiState.isPlaying) "Pause" else "Resume"
                )

                designSystemButton(
                    modifier = Modifier.weight(1f),
                    onClick = onNext,
                    text = "Next"
                )
            }
        }
    }
}