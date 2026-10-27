package com.yatharth.musitify.ui.elements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yatharth.musitify.data.entity.PlaylistInfo
import com.yatharth.musitify.data.entity.Song
import com.yatharth.musitify.data.viewmodel.PlaylistDialogUiState
import com.yatharth.musitify.ui.theme.cardBorder
import com.yatharth.musitify.ui.theme.text
import com.yatharth.musitify.ui.theme.textSecondary
import com.yatharth.musitify.util.SongDisplay

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
                onClick = onSubmit
            )
        },
        dismissButton = { DialogCancelButton(onClick = onDismiss) },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Song: ${SongDisplay.title(song)}",
                style = MaterialTheme.typography.bodySmall,
                color = textSecondary,
                fontWeight = FontWeight.Medium
            )

            // Both choices visible and labelled, instead of a switch whose
            // label flipped to describe its current state.
            SegmentedControl(
                options = listOf("EXISTING", "NEW"),
                selectedIndex = if (isAddToExistingMode) 0 else 1,
                onSelect = { onModeChange(it == 0) },
                modifier = Modifier.fillMaxWidth()
            )

            if (isAddToExistingMode) {
                if (playlists.isEmpty()) {
                    Text(
                        text = "No playlists yet. Choose NEW to make one.",
                        style = MaterialTheme.typography.labelSmall,
                        color = textSecondary,
                        letterSpacing = 1.sp
                    )
                } else {
                    Text(
                        text = "CHOOSE ONE",
                        style = MaterialTheme.typography.labelSmall,
                        color = textSecondary,
                        letterSpacing = 1.2.sp
                    )
                    LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                        items(playlists, key = { it.playlist.id }) { playlist ->
                            val selected = selectedPlaylist?.playlist?.id == playlist.playlist.id
                            // The whole row picks the playlist, not just the circle.
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .handCursor()
                                    .selectable(
                                        selected = selected,
                                        role = Role.RadioButton,
                                        onClick = { onSelectPlaylist(playlist) }
                                    ),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                RadioButton(
                                    selected = selected,
                                    onClick = null, // the row handles selection
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = cardBorder,
                                        unselectedColor = cardBorder
                                    )
                                )
                                Text(
                                    text = playlist.playlist.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = text
                                )
                            }
                        }
                    }
                }
            } else {
                TracknixTextField(
                    value = newPlaylistName,
                    onValueChange = onNewPlaylistNameChange,
                    placeholder = "Playlist name",
                    onSubmit = onSubmit,
                )
            }

            if (state is PlaylistDialogUiState.Error) DialogErrorText(state.message)
        }
    }
}
