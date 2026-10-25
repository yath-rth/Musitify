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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.yatharth.musitify.ui.theme.textSecondary

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
        title = {
            Text(
                text = "ADD TO PLAYLIST",
                style = adaptiveTypography().labelLarge,
                color = text,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Song: ${song.name}",
                    style = adaptiveTypography().bodySmall,
                    color = text.copy(alpha = 0.7f),
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
                            style = adaptiveTypography().labelSmall,
                            color = textSecondary,
                            letterSpacing = 1.sp
                        )
                    } else {
                        Text(
                            text = "CHOOSE ONE",
                            style = adaptiveTypography().labelSmall,
                            color = textSecondary,
                            fontWeight = FontWeight.Normal,
                            letterSpacing = 1.2.sp
                        )
                        LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                            items(playlists) { playlist ->
                                // The whole row picks the playlist, not just the circle.
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .selectable(
                                            selected = selectedPlaylist?.playlist?.id == playlist.playlist.id,
                                            role = Role.RadioButton,
                                            onClick = { onSelectPlaylist(playlist) }
                                        ),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    RadioButton(
                                        selected = selectedPlaylist?.playlist?.id == playlist.playlist.id,
                                        onClick = null, // the row handles selection
                                        colors = RadioButtonDefaults.colors(selectedColor = cardBorder, unselectedColor = cardBorder)
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
                                    focusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                                    unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent
                                )
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
                                "ADDING...",
                                style = adaptiveTypography().labelSmall,
                                color = text.copy(alpha = 0.7f),
                                letterSpacing = 1.2.sp
                            )
                        }
                    }

                    is PlaylistDialogUiState.Success -> {
                        Text(
                            text = "ADDED",
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

                    PlaylistDialogUiState.Idle -> { /* nothing */ }
                }
            }
        },
        confirmButton = {
            CustomButton(
                onClick = onSubmit,
                bgColor = accentColor2.copy(alpha = 0.2f)
            ) {
                Text(
                    text = if (state is PlaylistDialogUiState.Loading) "ADDING..." else "ADD",
                    style = adaptiveTypography().labelMedium,
                    color = accentColor2,
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
                    fontWeight = FontWeight.Normal,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }
    )
}
