package com.yatharth.musitify.ui.elements

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yatharth.musitify.engine.SleepTimerDuration
import com.yatharth.musitify.engine.SleepTimerState
import com.yatharth.musitify.ui.theme.cardBorder
import com.yatharth.musitify.ui.theme.itemsBg
import com.yatharth.musitify.ui.theme.text

private val firstRow = listOf(
    SleepTimerDuration.FIVE_MINUTES,
    SleepTimerDuration.TEN_MINUTES,
    SleepTimerDuration.FIFTEEN_MINUTES,
)
private val secondRow = listOf(
    SleepTimerDuration.THIRTY_MINUTES,
    SleepTimerDuration.FORTY_FIVE_MINUTES,
    SleepTimerDuration.SIXTY_MINUTES,
)

/**
 * Sleep timer dialog (1:1 with Android): while active, the time left (or "stops when this
 * song ends") + "CANCEL TIMER"; otherwise "THIS SONG" and a 3×2 grid of durations.
 * Any choice also dismisses.
 */
@Composable
fun SleepTimerDialog(
    sleepTimerState: SleepTimerState,
    onSetTimer: (SleepTimerDuration) -> Unit,
    onEndOfSong: () -> Unit,
    onCancelTimer: () -> Unit,
    onDismiss: () -> Unit
) {
    MusitifyDialog(
        onDismissRequest = onDismiss,
        title = "SLEEP TIMER",
        confirmButton = {
            CustomButton(onClick = onDismiss) {
                Text(
                    text = "CLOSE",
                    style = MaterialTheme.typography.labelMedium,
                    color = text,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier
                        .background(shape = RoundedCornerShape(10.dp), color = itemsBg.copy(alpha = 0.8f))
                        .padding(10.dp)
                )
            }
        },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (sleepTimerState.isActive && sleepTimerState.endOfSong) {
                Text(
                    text = "Music stops when this song ends.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = text,
                    textAlign = TextAlign.Center
                )
            } else if (sleepTimerState.isActive) {
                Text(
                    text = "Music stops in",
                    style = MaterialTheme.typography.bodyMedium,
                    color = text
                )

                val remainingMinutes = (sleepTimerState.remainingMs / 60000).toInt()
                val remainingSeconds = ((sleepTimerState.remainingMs % 60000) / 1000).toInt()

                Text(
                    text = String.format("%02d:%02d", remainingMinutes, remainingSeconds),
                    style = MaterialTheme.typography.titleMedium,
                    color = text
                )
            }

            if (sleepTimerState.isActive) {
                Spacer(modifier = Modifier.height(8.dp))

                CustomButton(
                    onClick = {
                        onCancelTimer()
                        onDismiss()
                    },
                    bgColor = cardBorder.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "CANCEL TIMER",
                        style = MaterialTheme.typography.labelMedium,
                        color = text,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.2.sp,
                        modifier = Modifier
                            .background(shape = RoundedCornerShape(10.dp), color = itemsBg.copy(alpha = 0.8f))
                            .padding(10.dp)
                    )
                }
            } else {
                Text(
                    text = "Stop the music after",
                    style = MaterialTheme.typography.bodySmall,
                    color = text,
                    textAlign = TextAlign.Center
                )

                // Most common choice, so it comes first and full width.
                CustomButton(
                    onClick = {
                        onEndOfSong()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "THIS SONG",
                        style = MaterialTheme.typography.labelMedium,
                        color = text,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp)
                    )
                }

                listOf(firstRow, secondRow).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        row.forEach { duration ->
                            TimerDurationButton(
                                duration = duration,
                                onClick = {
                                    onSetTimer(duration)
                                    onDismiss()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TimerDurationButton(
    duration: SleepTimerDuration,
    onClick: () -> Unit
) {
    CustomButton(
        onClick = onClick,
        bgColor = cardBorder.copy(alpha = 0.2f)
    ) {
        Text(
            text = "${duration.minutes}m",
            style = MaterialTheme.typography.labelMedium,
            color = text,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .background(shape = RoundedCornerShape(10.dp), color = itemsBg.copy(alpha = 0.8f))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}
