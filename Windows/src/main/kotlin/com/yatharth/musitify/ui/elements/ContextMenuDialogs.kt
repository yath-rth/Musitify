package com.yatharth.musitify.ui.elements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yatharth.musitify.data.entity.Playlist
import com.yatharth.musitify.data.entity.Song
import com.yatharth.musitify.ui.theme.accentColor1
import com.yatharth.musitify.ui.theme.accentColor2
import com.yatharth.musitify.ui.theme.cardBorder
import com.yatharth.musitify.ui.theme.text
import com.yatharth.musitify.util.SongDisplay

/** Long-press / right-click menu for a song: ADD TO PLAYLIST, RENAME, DELETE. */
@Composable
fun SongContextMenuDialog(
    song: Song,
    onDismiss: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    ContextMenuDialog(
        artPath = song.artPath,
        name = SongDisplay.title(song),
        subtitle = song.artist,
        addLabel = "ADD TO PLAYLIST",
        onDismiss = onDismiss,
        onAdd = onAddToPlaylist,
        onRename = onRename,
        onDelete = onDelete
    )
}

/** Long-press / right-click menu for a playlist: ADD SONGS, RENAME, DELETE. */
@Composable
fun PlaylistContextMenuDialog(
    playlist: Playlist,
    onDismiss: () -> Unit,
    onAddSongs: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    ContextMenuDialog(
        artPath = playlist.artPath,
        name = playlist.name,
        subtitle = "Playlist",
        addLabel = "ADD SONGS",
        onDismiss = onDismiss,
        onAdd = onAddSongs,
        onRename = onRename,
        onDelete = onDelete
    )
}

/** [artPath] null ⇒ no thumbnail (Android playlist menu); songs always show one. */
@Composable
private fun ContextMenuDialog(
    artPath: String?,
    name: String,
    subtitle: String,
    addLabel: String,
    onDismiss: () -> Unit,
    onAdd: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    MusitifyDialog(
        onDismissRequest = onDismiss,
        titleContent = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (artPath != null) {
                    ArtImage(
                        path = artPath,
                        contentDescription = name,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = text,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = text.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = { DialogCancelButton(onClick = onDismiss) },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ContextMenuAction(Icons.Outlined.Add, addLabel, accentColor2, accentColor2.copy(alpha = 0.2f), onAdd)
            ContextMenuAction(Icons.Outlined.Edit, "RENAME", text, cardBorder.copy(alpha = 0.1f), onRename)
            ContextMenuAction(Icons.Outlined.Delete, "DELETE", accentColor1, accentColor1.copy(alpha = 0.2f), onDelete)
        }
    }
}

@Composable
private fun ContextMenuAction(
    icon: ImageVector,
    label: String,
    color: Color,
    bgColor: Color,
    onClick: () -> Unit,
) {
    CustomButton(onClick = onClick, bgColor = bgColor) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color)
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = color,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.2.sp
            )
        }
    }
}
