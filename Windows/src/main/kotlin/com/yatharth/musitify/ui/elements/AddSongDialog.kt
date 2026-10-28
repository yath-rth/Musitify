package com.yatharth.musitify.ui.elements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yatharth.musitify.data.viewmodel.AddSongsUiState
import com.yatharth.musitify.ui.theme.danger
import com.yatharth.musitify.ui.theme.textSecondary
import com.yatharth.musitify.util.LinkDetector

/**
 * "ADD SONGS": paste a video or playlist link and queue it. The link decides what gets
 * added (no switch); the dialog says what it understood. Desktop: Enter submits.
 */
@Composable
fun AddSongDialog(
    onDismiss: () -> Unit,
    url: String,
    onUrlChange: (String) -> Unit,
    state: AddSongsUiState,
    onSubmit: () -> Unit,
) {
    val isPlaylist = LinkDetector.isPlaylist(url)

    MusitifyDialog(
        onDismissRequest = onDismiss,
        title = "ADD SONGS",
        confirmButton = {
            DialogConfirmButton(label = if (isPlaylist) "ADD PLAYLIST" else "ADD SONG", onClick = onSubmit)
        },
        dismissButton = { DialogCancelButton(onClick = onDismiss) },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            FieldLabel("YOUTUBE LINK")

            TracknixTextField(
                value = url,
                onValueChange = onUrlChange,
                placeholder = "Paste a video or playlist link",
                onSubmit = onSubmit,
            )

            Text(
                text = when {
                    url.isBlank() -> "A playlist link adds every song in it."
                    isPlaylist -> "Playlist link: every song will be added."
                    else -> "Song link: this one song will be added."
                },
                style = MaterialTheme.typography.bodySmall,
                color = textSecondary
            )

            if (state is AddSongsUiState.Error) {
                DialogErrorText(state.message)
            }
        }
    }
}

/** "FAILED: <message>" in red, labelSmall ExtraBold. */
@Composable
internal fun DialogErrorText(message: String) {
    Text(
        text = "FAILED: $message",
        style = MaterialTheme.typography.labelSmall,
        color = danger,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 1.2.sp
    )
}
