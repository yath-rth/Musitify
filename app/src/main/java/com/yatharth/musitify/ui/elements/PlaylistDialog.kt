package com.yatharth.musitify.ui.elements

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.yatharth.musitify.data.entity.PlaylistInfo
import com.yatharth.musitify.data.entity.Song
import com.yatharth.musitify.data.viewmodel.PlaylistDialogUiState
import com.yatharth.musitify.ui.theme.accentColor1
import com.yatharth.musitify.ui.theme.accentColor2
import com.yatharth.musitify.ui.theme.adaptiveTypography
import com.yatharth.musitify.ui.theme.cardBorder
import com.yatharth.musitify.ui.theme.checkBoxColors
import com.yatharth.musitify.ui.theme.itemsBg
import com.yatharth.musitify.ui.theme.secondaryBg
import com.yatharth.musitify.ui.theme.text

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
                text = "PLAYLISTS",
                style = adaptiveTypography().labelLarge,
                color = text,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isAddToExistingMode) "ADD TO EXISTING" else "CREATE NEW",
                        style = adaptiveTypography().labelSmall,
                        color = text.copy(alpha = 0.7f),
                        fontWeight = FontWeight.ExtraLight,
                        letterSpacing = 1.2.sp
                    )
                    CustomSwitch(
                        checked = isAddToExistingMode,
                        onCheckedChange = onModeChange
                    )
                }

                if (isAddToExistingMode) {
                    if (playlists.isEmpty()) {
                        Text(
                            text = "NO PLAYLISTS YET - SWITCH TO CREATE NEW",
                            style = adaptiveTypography().labelSmall,
                            color = text.copy(alpha = 0.5f),
                            letterSpacing = 1.sp
                        )
                    } else {
                        Text(
                            text = "SELECT PLAYLIST",
                            style = adaptiveTypography().labelSmall,
                            color = text.copy(alpha = 0.6f),
                            fontWeight = FontWeight.ExtraLight,
                            letterSpacing = 1.2.sp
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
                    // Tracknix input field pattern
                    Box(modifier = Modifier.fillMaxWidth().padding(end = 4.dp, bottom = 4.dp)) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .offset(x = 4.dp, y = 4.dp)
                                .background(cardBorder, RoundedCornerShape(5.dp))
                        )
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = itemsBg),
                            shape = RoundedCornerShape(5.dp),
                            border = BorderStroke(1.5.dp, cardBorder)
                        ) {
                            OutlinedTextField(
                                modifier = Modifier.fillMaxWidth(),
                                value = newPlaylistName,
                                onValueChange = onNewPlaylistNameChange,
                                placeholder = {
                                    Text(
                                        "Playlist name",
                                        style = adaptiveTypography().bodyMedium,
                                        color = text.copy(alpha = 0.4f)
                                    )
                                },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = text,
                                    unfocusedTextColor = text,
                                    focusedContainerColor = itemsBg,
                                    unfocusedContainerColor = itemsBg,
                                    cursorColor = accentColor2,
                                )
                            )
                        }
                    }
                }

                Text(
                    text = "SELECT SONGS (${selectedSongIds.size} SELECTED)",
                    style = adaptiveTypography().labelSmall,
                    color = text.copy(alpha = 0.6f),
                    fontWeight = FontWeight.ExtraLight,
                    letterSpacing = 1.2.sp
                )

                // Search field with Tracknix card pattern
                Box(modifier = Modifier.fillMaxWidth().padding(end = 4.dp, bottom = 4.dp)) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .offset(x = 4.dp, y = 4.dp)
                            .background(cardBorder, RoundedCornerShape(5.dp))
                    )
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = itemsBg),
                        shape = RoundedCornerShape(5.dp),
                        border = BorderStroke(1.5.dp, cardBorder)
                    ) {
                        OutlinedTextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = songQuery,
                            onValueChange = onSongFilter,
                            placeholder = {
                                Text(
                                    "FIND SONG...",
                                    style = adaptiveTypography().bodyMedium,
                                    color = text.copy(alpha = 0.4f)
                                )
                            },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = text,
                                unfocusedTextColor = text,
                                focusedContainerColor = itemsBg,
                                unfocusedContainerColor = itemsBg,
                                cursorColor = accentColor2,
                            )
                        )
                    }
                }

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

                when (state) {
                    is PlaylistDialogUiState.Loading -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.height(20.dp), strokeWidth = 2.dp, color = accentColor2)
                            Text(
                                "SAVING...",
                                style = adaptiveTypography().labelSmall,
                                color = text.copy(alpha = 0.7f),
                                letterSpacing = 1.2.sp
                            )
                        }
                    }

                    is PlaylistDialogUiState.Success -> {
                        Text(
                            text = "SAVED",
                            style = adaptiveTypography().labelSmall,
                            color = accentColor2,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.5.sp
                        )
                    }

                    is PlaylistDialogUiState.Error -> {
                        Text(
                            text = "FAILED: ${state.message}",
                            style = adaptiveTypography().labelSmall,
                            color = accentColor1,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.2.sp
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
                    text = if (state is PlaylistDialogUiState.Loading) "SAVING..." else "SAVE",
                    style = adaptiveTypography().labelMedium,
                    color = accentColor1,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.padding(10.dp)
                )
            }
        },
        dismissButton = {
            CustomButton(onClick = onDismiss) {
                Text(
                    text = "CANCEL",
                    style = adaptiveTypography().labelMedium,
                    color = text,
                    fontWeight = FontWeight.ExtraLight,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.padding(10.dp)
                )
            }
        },
        containerColor = secondaryBg
    )
}