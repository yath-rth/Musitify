package com.yatharth.musitify.ui.elements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yatharth.musitify.data.viewmodel.AddSongsUiState
import com.yatharth.musitify.ui.theme.accentColor1
import com.yatharth.musitify.ui.theme.text

/** "ADD SONGS": paste a single video or a playlist URL and queue it. Desktop: Enter submits. */
@Composable
fun AddSongDialog(
    onDismiss: () -> Unit,
    url: String,
    onUrlChange: (String) -> Unit,
    isPlaylistMode: Boolean,
    onModeChange: (Boolean) -> Unit,
    state: AddSongsUiState,
    onSubmit: () -> Unit,
) {
    MusitifyDialog(
        onDismissRequest = onDismiss,
        title = "ADD SONGS",
        confirmButton = { DialogConfirmButton(label = "ADD TO QUEUE", onClick = onSubmit) },
        dismissButton = { DialogCancelButton(onClick = onDismiss) },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ModeSwitchRow(
                label = if (isPlaylistMode) "PLAYLIST URL" else "SINGLE VIDEO URL",
                checked = isPlaylistMode,
                onCheckedChange = onModeChange
            )

            TracknixTextField(
                value = url,
                onValueChange = onUrlChange,
                placeholder = if (isPlaylistMode) "Paste playlist URL" else "Paste video URL",
                onSubmit = onSubmit,
            )

            if (state is AddSongsUiState.Error) {
                DialogErrorText(state.message)
            }
        }
    }
}

/** Mode label (labelSmall, 70 % text, ExtraLight, 1.2 sp) + [CustomSwitch], spaced apart. */
@Composable
internal fun ModeSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = text.copy(alpha = 0.7f),
            fontWeight = FontWeight.ExtraLight,
            letterSpacing = 1.2.sp
        )
        CustomSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/** "FAILED: <message>" in coral, labelSmall ExtraBold. */
@Composable
internal fun DialogErrorText(message: String) {
    Text(
        text = "FAILED: $message",
        style = MaterialTheme.typography.labelSmall,
        color = accentColor1,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 1.2.sp
    )
}
