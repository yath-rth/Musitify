package com.yatharth.musitify.ui.elements

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

// STUB — placeholder until the SettingsDialog lands (drop on merge).
@Composable
fun SettingsDialog(onDismiss: () -> Unit) {
    MusitifyDialog(
        onDismissRequest = onDismiss,
        title = "SETTINGS",
        confirmButton = { DialogConfirmButton("CLOSE", onDismiss, pill = true) },
    ) { Text("stub") }
}
