package com.yatharth.musitify.ui.elements

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.yatharth.musitify.service.SleepTimerDuration
import com.yatharth.musitify.service.SleepTimerState

// STUB — placeholder until the SleepTimerDialog port lands (drop on merge).
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
        confirmButton = { DialogConfirmButton("CLOSE", onDismiss, pill = true) },
    ) { Text("stub") }
}
