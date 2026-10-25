package com.yatharth.musitify.ui.elements

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.yatharth.musitify.data.entity.Song
import com.yatharth.musitify.data.viewmodel.PlaylistDialogUiState
import com.yatharth.musitify.ui.theme.adaptiveTypography
import com.yatharth.musitify.ui.theme.cardBorder
import com.yatharth.musitify.ui.theme.danger
import com.yatharth.musitify.ui.theme.itemsBg
import com.yatharth.musitify.ui.theme.primaryAction
import com.yatharth.musitify.ui.theme.text
import com.yatharth.musitify.ui.theme.textSecondary
import com.yatharth.musitify.util.Formatter

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
        title = { DialogTitle("NEW PLAYLIST") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FieldLabel("NAME")
                OutlinedInput(value = name, onValueChange = onNameChange, placeholder = "e.g. Thursday")
                FieldLabel("SONGS")
                OutlinedInput(value = query, onValueChange = onQueryChange, placeholder = "Find a song")
                SongPicker(songs = songs, selected = selected, alreadyIn = emptySet(), onToggle = onToggleSong)
                StatusLine(state)
            }
        },
        confirmButton = {
            PrimaryDialogButton(
                label = when {
                    state is PlaylistDialogUiState.Loading -> "CREATING…"
                    selected.isEmpty() -> "CREATE"
                    else -> "CREATE · ${selected.size}"
                },
                onClick = onSubmit
            )
        },
        dismissButton = { SecondaryDialogButton("CANCEL", onDismiss) }
    )
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
        title = { DialogTitle("ADD TO ${playlistName.uppercase()}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedInput(value = query, onValueChange = onQueryChange, placeholder = "Find a song")
                SongPicker(songs = songs, selected = selected, alreadyIn = alreadyIn, onToggle = onToggleSong)
                StatusLine(state)
            }
        },
        confirmButton = {
            PrimaryDialogButton(
                label = when {
                    state is PlaylistDialogUiState.Loading -> "ADDING…"
                    selected.isEmpty() -> "ADD"
                    else -> "ADD ${Formatter.count(selected.size, "SONG")}"
                },
                onClick = onSubmit
            )
        },
        dismissButton = { SecondaryDialogButton("CANCEL", onDismiss) }
    )
}

@Composable
private fun SongPicker(
    songs: List<Song>,
    selected: Set<Song>,
    alreadyIn: Set<Int>,
    onToggle: (Song) -> Unit,
) {
    if (songs.isEmpty()) {
        Text("No songs match.", style = adaptiveTypography().bodySmall, color = textSecondary)
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
                AsyncImage(
                    model = song.artPath,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.name,
                        style = adaptiveTypography().bodySmall,
                        color = if (inPlaylist) textSecondary else text,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (inPlaylist) {
                        Text(
                            text = "Already in playlist",
                            style = adaptiveTypography().labelSmall,
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
    if (state is PlaylistDialogUiState.Error) {
        Text(
            text = state.message,
            style = adaptiveTypography().bodySmall,
            color = danger
        )
    }
}

@Composable
internal fun DialogTitle(title: String) {
    Text(
        text = title,
        style = adaptiveTypography().labelLarge,
        color = text,
        fontWeight = FontWeight.ExtraBold,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun FieldLabel(label: String) {
    Text(text = label, style = adaptiveTypography().labelSmall, color = textSecondary)
}

/** Text field in the outlined-card style used across the app's forms. */
@Composable
internal fun OutlinedInput(value: String, onValueChange: (String) -> Unit, placeholder: String) {
    val shape = RoundedCornerShape(5.dp)
    Box(modifier = Modifier
        .fillMaxWidth()
        .padding(end = 4.dp, bottom = 4.dp)) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(4.dp, 4.dp)
                .background(cardBorder, shape)
        )
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .background(itemsBg, shape)
                .border(1.5.dp, cardBorder, shape),
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(placeholder, style = adaptiveTypography().bodyMedium, color = textSecondary)
            },
            singleLine = true,
            textStyle = adaptiveTypography().bodyMedium,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = text,
                unfocusedTextColor = text,
                focusedContainerColor = itemsBg,
                unfocusedContainerColor = itemsBg,
                cursorColor = text,
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent
            )
        )
    }
}

@Composable
internal fun PrimaryDialogButton(label: String, onClick: () -> Unit) {
    CustomButton(onClick = onClick, bgColor = primaryAction.copy(alpha = 0.2f)) {
        Text(
            text = label,
            style = adaptiveTypography().labelMedium,
            color = text,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(10.dp)
        )
    }
}

@Composable
internal fun SecondaryDialogButton(label: String, onClick: () -> Unit) {
    CustomButton(onClick = onClick) {
        Text(
            text = label,
            style = adaptiveTypography().labelMedium,
            color = text,
            modifier = Modifier.padding(10.dp)
        )
    }
}
