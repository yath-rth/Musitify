package com.yatharth.musitify.ui.elements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yatharth.musitify.data.entity.Song
import com.yatharth.musitify.data.viewmodel.PlaylistDialogUiState
import com.yatharth.musitify.ui.theme.cardBorder
import com.yatharth.musitify.ui.theme.itemsBg
import com.yatharth.musitify.ui.theme.text
import com.yatharth.musitify.ui.theme.textSecondary
import com.yatharth.musitify.util.Formatter
import com.yatharth.musitify.util.SongDisplay

/** New playlist: a name and the songs to start it with. */
@Composable
fun CreatePlaylistDialog(
    name: String,
    onNameChange: (String) -> Unit,
    query: String,
    onQueryChange: (String) -> Unit,
    songs: List<Song>,
    selected: Set<Song>,
    onToggleSong: (Song) -> Unit,
    state: PlaylistDialogUiState,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
) {
    MusitifyDialog(
        onDismissRequest = onDismiss,
        title = "NEW PLAYLIST",
        confirmButton = {
            DialogConfirmButton(
                label = when {
                    state is PlaylistDialogUiState.Loading -> "CREATING…"
                    selected.isEmpty() -> "CREATE"
                    else -> "CREATE · ${selected.size}"
                },
                onClick = onSubmit,
                textColor = text
            )
        },
        dismissButton = { DialogCancelButton(onClick = onDismiss) },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            FieldLabel("NAME")
            TracknixTextField(value = name, onValueChange = onNameChange, placeholder = "e.g. Thursday")
            FieldLabel("SONGS")
            TracknixTextField(value = query, onValueChange = onQueryChange, placeholder = "Find a song")
            SongPicker(songs = songs, selected = selected, alreadyIn = emptySet(), onToggle = onToggleSong)
            StatusLine(state)
        }
    }
}

/**
 * Add songs to one known playlist. Songs already in it are shown ticked and
 * locked, so it's clear what's there and nothing gets added twice.
 */
@Composable
fun AddSongsToPlaylistDialog(
    playlistName: String,
    query: String,
    onQueryChange: (String) -> Unit,
    songs: List<Song>,
    alreadyIn: Set<Int>,
    selected: Set<Song>,
    onToggleSong: (Song) -> Unit,
    state: PlaylistDialogUiState,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
) {
    MusitifyDialog(
        onDismissRequest = onDismiss,
        title = "ADD TO ${playlistName.uppercase()}",
        confirmButton = {
            DialogConfirmButton(
                label = when {
                    state is PlaylistDialogUiState.Loading -> "ADDING…"
                    selected.isEmpty() -> "ADD"
                    else -> "ADD ${Formatter.count(selected.size, "SONG")}"
                },
                onClick = onSubmit,
                textColor = text
            )
        },
        dismissButton = { DialogCancelButton(onClick = onDismiss) },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            TracknixTextField(value = query, onValueChange = onQueryChange, placeholder = "Find a song")
            SongPicker(songs = songs, selected = selected, alreadyIn = alreadyIn, onToggle = onToggleSong)
            StatusLine(state)
        }
    }
}

@Composable
private fun SongPicker(
    songs: List<Song>,
    selected: Set<Song>,
    alreadyIn: Set<Int>,
    onToggle: (Song) -> Unit,
) {
    if (songs.isEmpty()) {
        Text("No songs match.", style = MaterialTheme.typography.bodySmall, color = textSecondary)
        return
    }
    // Songs you can still add come first; ones already in the playlist sink.
    val ordered = remember(songs, alreadyIn) { songs.sortedBy { it.id in alreadyIn } }
    LazyColumn(modifier = Modifier.heightIn(max = 260.dp)) {
        items(ordered, key = { it.id }) { song ->
            val inPlaylist = song.id in alreadyIn
            val checked = inPlaylist || song in selected
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (inPlaylist) Modifier else Modifier.handCursor())
                    .toggleable(
                        value = checked,
                        enabled = !inPlaylist,
                        role = Role.Checkbox,
                        onValueChange = { onToggle(song) }
                    )
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Checkbox(
                    checked = checked,
                    onCheckedChange = null, // the row handles it
                    enabled = !inPlaylist,
                    colors = CheckboxDefaults.colors(
                        checkedColor = cardBorder,
                        uncheckedColor = cardBorder,
                        checkmarkColor = itemsBg,
                        disabledCheckedColor = textSecondary
                    )
                )
                ArtImage(
                    path = song.artPath,
                    modifier = Modifier.size(36.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = SongDisplay.title(song),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (inPlaylist) textSecondary else text,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (inPlaylist) {
                        Text(
                            text = "Already in playlist",
                            style = MaterialTheme.typography.labelSmall,
                            color = textSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusLine(state: PlaylistDialogUiState) {
    if (state is PlaylistDialogUiState.Error) DialogErrorText(state.message)
}
