package com.yatharth.musitify.ui.elements

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yatharth.musitify.data.entity.PlaylistInfo
import com.yatharth.musitify.data.entity.Song
import com.yatharth.musitify.data.viewmodel.PlaylistDialogUiState
import com.yatharth.musitify.ui.theme.accentColor2
import com.yatharth.musitify.ui.theme.checkBoxColors
import com.yatharth.musitify.ui.theme.text

/**
 * "PLAYLISTS": create a new playlist, or add songs to an existing one (switch).
 * Does not auto-close on success — shows "SAVED" (Android behaviour).
 * Desktop: whole rows are clickable, not just the checkboxes.
 */
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
    MusitifyDialog(
        onDismissRequest = onDismiss,
        title = "PLAYLISTS",
        confirmButton = {
            DialogConfirmButton(
                label = if (state is PlaylistDialogUiState.Loading) "SAVING..." else "SAVE",
                onClick = onSubmit
            )
        },
        dismissButton = { DialogCancelButton(onClick = onDismiss) },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                    maxHeight = 120.dp
                )
            } else {
                TracknixTextField(
                    value = newPlaylistName,
                    onValueChange = onNewPlaylistNameChange,
                    placeholder = "Playlist name",
                )
            }

            DialogSectionLabel("SELECT SONGS (${selectedSongIds.size} SELECTED)")

            TracknixTextField(
                value = songQuery,
                onValueChange = onSongFilter,
                placeholder = "FIND SONG...",
            )

            LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                items(allSongs, key = { it.id }) { song ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .handCursor()
                            .clickable { onToggleSong(song) },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(
                            checked = selectedSongIds.contains(song),
                            onCheckedChange = { onToggleSong(song) },
                            colors = checkBoxColors()
                        )
                        ArtImage(
                            path = song.artPath,
                            modifier = Modifier.size(32.dp),
                            contentScale = ContentScale.Fit
                        )
                        Text(
                            text = song.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = text,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            PlaylistStateLine(state, loadingLabel = "SAVING...", successLabel = "SAVED")
        }
    }
}

/** Small section caption: labelSmall, 60 % text, ExtraLight, 1.2 sp. */
@Composable
internal fun DialogSectionLabel(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = text.copy(alpha = 0.6f),
        fontWeight = FontWeight.ExtraLight,
        letterSpacing = 1.2.sp
    )
}

/** Single-select playlist list made of checkboxes (Android pattern) or the empty hint. */
@Composable
internal fun PlaylistPicker(
    playlists: List<PlaylistInfo>,
    selectedPlaylist: PlaylistInfo?,
    onSelectPlaylist: (PlaylistInfo) -> Unit,
    maxHeight: Dp,
) {
    if (playlists.isEmpty()) {
        Text(
            text = "NO PLAYLISTS YET - SWITCH TO CREATE NEW",
            style = MaterialTheme.typography.labelSmall,
            color = text.copy(alpha = 0.5f),
            letterSpacing = 1.sp
        )
        return
    }
    Column {
        DialogSectionLabel("SELECT PLAYLIST")
        Spacer(modifier = Modifier.height(4.dp))
        LazyColumn(modifier = Modifier.heightIn(max = maxHeight)) {
            items(playlists, key = { it.playlist.id }) { playlist ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .handCursor()
                        .clickable { onSelectPlaylist(playlist) },
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
                        style = MaterialTheme.typography.bodySmall,
                        color = text
                    )
                }
            }
        }
    }
}

/** Loading spinner + label / success label / "FAILED: msg" / nothing. */
@Composable
internal fun PlaylistStateLine(
    state: PlaylistDialogUiState,
    loadingLabel: String,
    successLabel: String,
) {
    when (state) {
        is PlaylistDialogUiState.Loading -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.height(20.dp),
                    strokeWidth = 2.dp,
                    color = accentColor2
                )
                Text(
                    loadingLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = text.copy(alpha = 0.7f),
                    letterSpacing = 1.2.sp
                )
            }
        }

        is PlaylistDialogUiState.Success -> {
            Text(
                text = successLabel,
                style = MaterialTheme.typography.labelSmall,
                color = accentColor2,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.5.sp
            )
        }

        is PlaylistDialogUiState.Error -> DialogErrorText(state.message)

        PlaylistDialogUiState.Idle -> Unit
    }
}
