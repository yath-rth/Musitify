package com.yatharth.musitify.ui.elements

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yatharth.musitify.data.repository.PlayerUiState

// STUB — placeholder until the PlaybackControls port lands (drop on merge).
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
    ShadowBox(modifier.padding(8.dp)) {
        Column(Modifier.padding(8.dp)) {
            FillBar(label = uiState.currentTitle, value = uiState.positionMs.toDouble(), max = uiState.durationMs.toDouble())
            Row {
                CustomCircularButton(onClick = onBack) { Icon(Icons.Outlined.SkipPrevious, null) }
                CustomCircularButton(onClick = onPause) {
                    Icon(if (uiState.isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow, null)
                }
                CustomCircularButton(onClick = onNext) { Icon(Icons.Outlined.SkipNext, null) }
                CustomCircularButton(onClick = onStats) { Icon(Icons.Outlined.Insights, null) }
                CustomCircularButton(onClick = onSleepTimer) { Icon(Icons.Outlined.Bedtime, null) }
            }
        }
    }
}
