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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yatharth.musitify.service.SleepTimerDuration
import com.yatharth.musitify.service.SleepTimerState
import com.yatharth.musitify.ui.theme.accentColor1
import com.yatharth.musitify.ui.theme.adaptiveTypography
import com.yatharth.musitify.ui.theme.cardBorder
import com.yatharth.musitify.ui.theme.itemsBg
import com.yatharth.musitify.ui.theme.secondaryBg
import com.yatharth.musitify.ui.theme.text

@Composable
fun SleepTimerDialog(
    sleepTimerState: SleepTimerState,
    onSetTimer: (SleepTimerDuration) -> Unit,
    onCancelTimer: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "SLEEP TIMER",
                style = adaptiveTypography().labelLarge,
                color = text,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (sleepTimerState.isActive) {
                    // Show active timer info
                    Text(
                        text = "Timer Active",
                        style = adaptiveTypography().bodyMedium,
                        color = text,
                        fontWeight = FontWeight.Bold
                    )

                    val remainingMinutes = (sleepTimerState.remainingMs / 60000).toInt()
                    val remainingSeconds = ((sleepTimerState.remainingMs % 60000) / 1000).toInt()

                    Text(
                        text = String.format("%02d:%02d", remainingMinutes, remainingSeconds),
                        style = adaptiveTypography().headlineLarge,
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
                            style = adaptiveTypography().labelMedium,
                            color = text,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.2.sp,
                            modifier = Modifier
                                .background(
                                    shape = RoundedCornerShape(10.dp),
                                    color = itemsBg.copy(alpha = 0.8f)
                                )
                                .padding(10.dp)
                        )
                    }
                } else {
                    // Show timer duration options
                    Text(
                        text = "Set sleep timer duration",
                        style = adaptiveTypography().bodySmall,
                        color = text,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // First row of durations
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        TimerDurationButton(
                            duration = SleepTimerDuration.FIVE_MINUTES,
                            onClick = {
                                onSetTimer(SleepTimerDuration.FIVE_MINUTES)
                                onDismiss()
                            }
                        )
                        TimerDurationButton(
                            duration = SleepTimerDuration.TEN_MINUTES,
                            onClick = {
                                onSetTimer(SleepTimerDuration.TEN_MINUTES)
                                onDismiss()
                            }
                        )
                        TimerDurationButton(
                            duration = SleepTimerDuration.FIFTEEN_MINUTES,
                            onClick = {
                                onSetTimer(SleepTimerDuration.FIFTEEN_MINUTES)
                                onDismiss()
                            }
                        )
                    }

                    // Second row of durations
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        TimerDurationButton(
                            duration = SleepTimerDuration.THIRTY_MINUTES,
                            onClick = {
                                onSetTimer(SleepTimerDuration.THIRTY_MINUTES)
                                onDismiss()
                            }
                        )
                        TimerDurationButton(
                            duration = SleepTimerDuration.FORTY_FIVE_MINUTES,
                            onClick = {
                                onSetTimer(SleepTimerDuration.FORTY_FIVE_MINUTES)
                                onDismiss()
                            }
                        )
                        TimerDurationButton(
                            duration = SleepTimerDuration.SIXTY_MINUTES,
                            onClick = {
                                onSetTimer(SleepTimerDuration.SIXTY_MINUTES)
                                onDismiss()
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            CustomButton(
                onClick = onDismiss,
            ) {
                Text(
                    text = "CLOSE",
                    style = adaptiveTypography().labelMedium,
                    color = text,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier
                        .background(
                            shape = RoundedCornerShape(10.dp),
                            color = itemsBg.copy(alpha = 0.8f)
                        )
                        .padding(10.dp)
                )
            }
        },
        containerColor = secondaryBg
    )
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
            style = adaptiveTypography().labelMedium,
            color = text,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .background(
                    shape = RoundedCornerShape(10.dp),
                    color = itemsBg.copy(alpha = 0.8f)
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}
