package com.yatharth.musitify.ui.elements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.yatharth.musitify.data.entity.PlaylistInfo
import com.yatharth.musitify.data.entity.Song
import com.yatharth.musitify.data.viewmodel.PlaylistDialogUiState
import com.yatharth.musitify.ui.theme.accentColor1
import com.yatharth.musitify.ui.theme.adaptiveTypography
import com.yatharth.musitify.ui.theme.checkBoxColors
import com.yatharth.musitify.ui.theme.secondaryBg
import com.yatharth.musitify.ui.theme.text
import com.yatharth.musitify.ui.theme.textFieldColors

@Composable
fun PlaylistDialog(
    onDismiss: () -> Unit,

    // Mode toggle: false = create new playlist, true = add to existing
    isAddToExistingMode: Boolean,
    onModeChange: (Boolean) -> Unit,

    // Create-mode fields
    newPlaylistName: String,
    onNewPlaylistNameChange: (String) -> Unit,

    songQuery: String,
    onSongFilter: (String) -> Unit,

    // Add-to-existing fields
    playlists: List<PlaylistInfo>,
    selectedPlaylist: PlaylistInfo?,
    onSelectPlaylist: (PlaylistInfo) -> Unit,

    // Song selection (used in both modes)
    allSongs: List<Song>,
    selectedSongIds: Set<Song>,
    onToggleSong: (Song) -> Unit,

    state: PlaylistDialogUiState,
    onSubmit: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Playlists",
                style = adaptiveTypography().bodyLarge,
                color = text
            )
        },
        text = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isAddToExistingMode) "Add to Existing" else "Create New",
                        style = adaptiveTypography().bodySmall,
                        color = text
                    )
                    CustomSwitch(
                        checked = isAddToExistingMode,
                        onCheckedChange = onModeChange
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (isAddToExistingMode) {
                    if (playlists.isEmpty()) {
                        Text(
                            text = "No playlists yet — switch to Create New",
                            style = adaptiveTypography().labelSmall,
                            color = text.copy(alpha = 0.5f)
                        )
                    } else {
                        Text(
                            text = "Select playlist",
                            style = adaptiveTypography().labelSmall,
                            color = text.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        LazyColumn(modifier = Modifier.heightIn(max = 120.dp)) {
                            items(playlists) { playlist ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Checkbox(
                                        checked = selectedPlaylist?.playlist?.id == playlist.playlist.id,
                                        onCheckedChange = { onSelectPlaylist(playlist) },
                                        colors = checkBoxColors()
                                    )
                                    Text(
                                        text = playlist.playlist.name,
                                        style = adaptiveTypography().bodySmall,
                                        color = text
                                    )
                                }
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = newPlaylistName,
                        onValueChange = onNewPlaylistNameChange,
                        placeholder = {
                            Text(
                                "Playlist name",
                                color = text,
                                style = adaptiveTypography().bodyMedium
                            )
                        },
                        singleLine = true,
                        colors = textFieldColors(),
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Select songs | (${selectedSongIds.size} selected)",
                    style = adaptiveTypography().labelSmall,
                    color = text.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = songQuery,
                    onValueChange = onSongFilter,
                    placeholder = {
                        Text(
                            "Find song...",
                            color = text,
                            style = adaptiveTypography().bodyMedium
                        )
                    },
                    singleLine = true,
                    colors = textFieldColors(),
                )
                LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                    items(allSongs, key = { it.id }) { song ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Checkbox(
                                checked = selectedSongIds.contains(song),
                                onCheckedChange = { onToggleSong(song) },
                                colors = checkBoxColors()
                            )
                            AsyncImage(
                                model = song.artPath,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp)
                            )
                            Text(
                                text = song.name,
                                style = adaptiveTypography().bodySmall,
                                color = text,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                when (state) {
                    is PlaylistDialogUiState.Loading -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.height(20.dp))
                            Text("Saving...", style = adaptiveTypography().labelSmall, color = text)
                        }
                    }

                    is PlaylistDialogUiState.Success -> {
                        Text(
                            text = "Saved",
                            style = adaptiveTypography().labelSmall,
                            color = accentColor1,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    is PlaylistDialogUiState.Error -> {
                        Text(
                            text = "Failed: ${state.message}",
                            style = adaptiveTypography().labelSmall,
                            color = Color.Red
                        )
                    }

                    PlaylistDialogUiState.Idle -> { /* nothing */
                    }
                }
            }
        },
        confirmButton = {
            CustomButton(
                onClick = onSubmit,
                bgColor = accentColor1.copy(alpha = 0.2f)
            ) {
                Text(
                    text = if (state is PlaylistDialogUiState.Loading) "Saving..." else "Save",
                    style = adaptiveTypography().bodyLarge,
                    color = accentColor1,
                    modifier = Modifier.padding(10.dp)
                )
            }
        },
        dismissButton = {
            CustomButton(onClick = onDismiss) {
                Text(
                    text = "Cancel",
                    style = adaptiveTypography().bodyLarge,
                    color = text,
                    modifier = Modifier.padding(10.dp)
                )
            }
        },
        containerColor = secondaryBg
    )
}