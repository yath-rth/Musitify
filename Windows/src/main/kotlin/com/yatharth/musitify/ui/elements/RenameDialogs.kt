package com.yatharth.musitify.ui.elements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yatharth.musitify.data.entity.Playlist
import com.yatharth.musitify.data.entity.Song
import com.yatharth.musitify.ui.theme.text

/** "RENAME SONG". Desktop: Enter confirms. */
@Composable
fun RenameSongDialog(
    song: Song,
    newName: String,
    onNameChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    RenameDialog(
        title = "RENAME SONG",
        currentName = song.name,
        newName = newName,
        onNameChange = onNameChange,
        onDismiss = onDismiss,
        onConfirm = onConfirm
    )
}

/** "RENAME PLAYLIST". Desktop: Enter confirms. */
@Composable
fun RenamePlaylistDialog(
    playlist: Playlist,
    newName: String,
    onNameChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    RenameDialog(
        title = "RENAME PLAYLIST",
        currentName = playlist.name,
        newName = newName,
        onNameChange = onNameChange,
        onDismiss = onDismiss,
        onConfirm = onConfirm
    )
}

@Composable
private fun RenameDialog(
    title: String,
    currentName: String,
    newName: String,
    onNameChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    MusitifyDialog(
        onDismissRequest = onDismiss,
        title = title,
        confirmButton = { DialogConfirmButton(label = "RENAME", onClick = onConfirm) },
        dismissButton = { DialogCancelButton(onClick = onDismiss) },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Current name: $currentName",
                style = MaterialTheme.typography.bodySmall,
                color = text.copy(alpha = 0.7f),
                fontWeight = FontWeight.Medium
            )
            TracknixTextField(
                value = newName,
                onValueChange = onNameChange,
                placeholder = "Enter new name",
                showOutline = false,
                onSubmit = onConfirm,
            )
        }
    }
}
