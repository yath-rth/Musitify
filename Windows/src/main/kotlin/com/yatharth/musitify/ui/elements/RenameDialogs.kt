package com.yatharth.musitify.ui.elements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.yatharth.musitify.data.entity.Playlist
import com.yatharth.musitify.data.entity.Song

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
        newName = newName,
        onNameChange = onNameChange,
        onDismiss = onDismiss,
        onConfirm = onConfirm
    )
}

@Composable
private fun RenameDialog(
    title: String,
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
            TracknixTextField(
                value = newName,
                onValueChange = onNameChange,
                placeholder = "Enter new name",
                onSubmit = onConfirm,
            )
        }
    }
}
