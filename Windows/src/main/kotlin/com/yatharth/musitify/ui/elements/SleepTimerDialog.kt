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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yatharth.musitify.engine.SleepTimerDuration
import com.yatharth.musitify.engine.SleepTimerState
import com.yatharth.musitify.ui.theme.JetBrainsMonoNL
import com.yatharth.musitify.ui.theme.accentColor1
import com.yatharth.musitify.ui.theme.adaptiveSp
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
 * Sleep timer dialog (1:1 with Android): live MM:SS countdown + "CANCEL TIMER"
 * while active, otherwise a 3×2 grid of durations. Any choice also dismisses.
 *
 * Desktop fix: Android rendered the countdown with the undefined `headlineLarge`
 * (system font); here it uses JetBrains Mono at the same 32/40 sp metrics.
 */
@Composable
fun SleepTimerDialog(
    sleepTimerState: SleepTimerState,
    onSetTimer: (SleepTimerDuration) -> Unit,
    onCancelTimer: () -> Unit,
    onDismiss: () -> Unit
) {
    MusitifyDialog(
        onDismissRequest = onDismiss,
        title = "SLEEP TIMER",
        confirmButton = {
            DialogConfirmButton(label = "CLOSE", onClick = onDismiss, pill = true)
        },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (sleepTimerState.isActive) {
                Text(
                    text = "Timer Active",
                    style = MaterialTheme.typography.bodyMedium,
                    color = accentColor1,
                    fontWeight = FontWeight.Bold
                )

                val remainingMinutes = (sleepTimerState.remainingMs / 60000).toInt()
                val remainingSeconds = ((sleepTimerState.remainingMs % 60000) / 1000).toInt()

                Text(
                    text = String.format("%02d:%02d", remainingMinutes, remainingSeconds),
                    style = TextStyle(
                        fontFamily = JetBrainsMonoNL,
                        fontSize = adaptiveSp(32f),
                        lineHeight = adaptiveSp(40f),
                        letterSpacing = 0.sp
                    ),
                    color = text,
                    fontWeight = FontWeight.Bold
                )

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
                    text = "Set sleep timer duration",
                    style = MaterialTheme.typography.bodySmall,
                    color = text,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

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
