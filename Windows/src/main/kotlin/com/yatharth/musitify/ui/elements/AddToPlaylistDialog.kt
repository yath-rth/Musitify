package com.yatharth.musitify.ui.elements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yatharth.musitify.data.entity.PlaylistInfo
import com.yatharth.musitify.data.entity.Song
import com.yatharth.musitify.data.viewmodel.PlaylistDialogUiState
import com.yatharth.musitify.ui.theme.accentColor2
import com.yatharth.musitify.ui.theme.text

/** "ADD TO PLAYLIST" for a single song: pick an existing playlist or create a new one. */
@Composable
fun AddToPlaylistDialog(
    song: Song,
    onDismiss: () -> Unit,

    // Mode toggle: false = create new playlist, true = add to existing
    isAddToExistingMode: Boolean,
    onModeChange: (Boolean) -> Unit,

    // Create-mode fields
    newPlaylistName: String,
    onNewPlaylistNameChange: (String) -> Unit,

    // Add-to-existing fields
    playlists: List<PlaylistInfo>,
    selectedPlaylist: PlaylistInfo?,
    onSelectPlaylist: (PlaylistInfo) -> Unit,

    state: PlaylistDialogUiState,
    onSubmit: () -> Unit
) {
    MusitifyDialog(
        onDismissRequest = onDismiss,
        title = "ADD TO PLAYLIST",
        confirmButton = {
            DialogConfirmButton(
                label = if (state is PlaylistDialogUiState.Loading) "ADDING..." else "ADD",
                onClick = onSubmit,
                color = accentColor2
            )
        },
        dismissButton = { DialogCancelButton(onClick = onDismiss) },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Song: ${song.name}",
                style = MaterialTheme.typography.bodySmall,
                color = text.copy(alpha = 0.7f),
                fontWeight = FontWeight.Medium
            )

            ModeSwitchRow(
                label = if (isAddToExistingMode) "ADD TO EXISTING" else "CREATE NEW",
                checked = isAddToExistingMode,
                onCheckedChange = onModeChange
            )

            if (isAddToExistingMode) {
                PlaylistPicker(
                    playlists = playlists,
                    selectedPlaylist = selectedPlaylist,
                    onSelectPlaylist = onSelectPlaylist,
                    maxHeight = 200.dp
                )
            } else {
                TracknixTextField(
                    value = newPlaylistName,
                    onValueChange = onNewPlaylistNameChange,
                    placeholder = "Playlist name",
                    showOutline = false,
                    onSubmit = onSubmit,
                )
            }

            PlaylistStateLine(state, loadingLabel = "ADDING...", successLabel = "ADDED")
        }
    }
}
