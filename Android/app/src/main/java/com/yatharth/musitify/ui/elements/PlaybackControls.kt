package com.yatharth.musitify.ui.elements

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.outlined.ArrowBackIosNew
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.RepeatOne
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.yatharth.musitify.data.repository.PlayerUiState
import com.yatharth.musitify.ui.theme.cardBorder
import com.yatharth.musitify.ui.theme.itemsBg
import com.yatharth.musitify.util.Formatter.formatTime

/**
 * Mini player pinned above the system bar. Playback only: previous, play and
 * next, with play larger and filled as the main action. Shuffle and repeat sit
 * at the edges, smaller. Everything else (sleep timer, seeking) is in the
 * full-screen player, opened by tapping the title row.
 */
@Composable
fun PlaybackControls(
    modifier: Modifier,
    uiState: PlayerUiState,
    onBack: () -> Unit,
    onPause: () -> Unit,
    onNext: () -> Unit,
    onRepeatToggle: () -> Unit,
    onShuffleToggle: () -> Unit,
    // Opens the full-screen player. Null while nothing is loaded.
    onOpenNowPlaying: (() -> Unit)? = null
) {
    val repeatOn = uiState.repeatMode != 0

    ShadowBox(
        modifier = modifier.padding(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (onOpenNowPlaying != null)
                            Modifier.clickable(onClickLabel = "Open player", onClick = onOpenNowPlaying)
                        else Modifier
                    )
            ) {
                FillBar(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    label = uiState.currentTitle ?: "Nothing playing",
                    value = uiState.positionMs.toDouble(),
                    max = uiState.durationMs.toDouble(),
                    secondaryLabel = if (uiState.durationMs != 0L)
                        "${formatTime(uiState.positionMs)} / ${formatTime(uiState.durationMs)}"
                    else null
                )
                if (onOpenNowPlaying != null) {
                    Icon(
                        imageVector = Icons.Outlined.KeyboardArrowUp,
                        contentDescription = null,
                        tint = cardBorder,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CustomCircularButton(
                    onClick = onShuffleToggle,
                    selected = uiState.shuffleMode,
                    modifier = Modifier.semantics {
                        contentDescription = if (uiState.shuffleMode) "Shuffle, on" else "Shuffle, off"
                    }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Shuffle,
                        contentDescription = null,
                        tint = toggleTint(uiState.shuffleMode),
                        modifier = Modifier.size(18.dp)
                    )
                }

                CustomCircularButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Outlined.ArrowBackIosNew,
                        contentDescription = "Previous song",
                        tint = cardBorder
                    )
                }

                CustomCircularButton(onClick = onPause, selected = true) {
                    Icon(
                        imageVector = if (uiState.isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                        contentDescription = if (uiState.isPlaying) "Pause" else "Play",
                        tint = itemsBg,
                        modifier = Modifier
                            .padding(4.dp)
                            .size(32.dp)
                    )
                }

                CustomCircularButton(onClick = onNext) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
                        contentDescription = "Next song",
                        tint = cardBorder
                    )
                }

                CustomCircularButton(
                    onClick = onRepeatToggle,
                    selected = repeatOn,
                    modifier = Modifier.semantics {
                        contentDescription = when (uiState.repeatMode) {
                            1 -> "Repeat one, on"
                            0 -> "Repeat, off"
                            else -> "Repeat all, on"
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (uiState.repeatMode == 1) Icons.Outlined.RepeatOne else Icons.Outlined.Repeat,
                        contentDescription = null,
                        tint = toggleTint(repeatOn),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/** Icon colour for a toggle: white on the filled ink background when on. */
private fun toggleTint(on: Boolean) = if (on) itemsBg else cardBorder
