package com.yatharth.musitify.ui.elements

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.yatharth.musitify.data.entity.Song
import com.yatharth.musitify.ui.theme.accentColor2
import com.yatharth.musitify.ui.theme.adaptiveTypography
import com.yatharth.musitify.ui.theme.cardBorder
import com.yatharth.musitify.ui.theme.itemsBg
import com.yatharth.musitify.ui.theme.text
import com.yatharth.musitify.util.Formatter
import com.yatharth.musitify.util.SongMedia.playCountOf
import java.io.File

@Composable
fun ReorderableSongList(
    songs: List<Song>,
    currentPlayingIndex: Int,
    onTrackSelected: (Int) -> Unit,
    onReorder: (fromIndex: Int, toIndex: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth().height(500.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(
            items = songs,
            key = { _, song -> song.id }
        ) { index, song ->
            val isPlaying = index == currentPlayingIndex

            ReorderableSongRow(
                song = song,
                index = index,
                totalSongs = songs.size,
                isPlaying = isPlaying,
                onClick = { onTrackSelected(index) },
                onMoveUp = {
                    if (index > 0) {
                        onReorder(index, index - 1)
                    }
                },
                onMoveDown = {
                    if (index < songs.size - 1) {
                        onReorder(index, index + 1)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ReorderableSongRow(
    song: Song,
    index: Int,
    totalSongs: Int,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (isPlaying) itemsBg.copy(alpha = 0.8f) else itemsBg
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // Reorder controls
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.width(40.dp)
            ) {
                IconButton(
                    onClick = onMoveUp,
                    enabled = index > 0,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Move up",
                        tint = if (index > 0) cardBorder else cardBorder.copy(alpha = 0.3f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onMoveDown,
                    enabled = index < totalSongs - 1,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Move down",
                        tint = if (index < totalSongs - 1) cardBorder else cardBorder.copy(alpha = 0.3f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Album art
            AsyncImage(
                model = File(song.artPath),
                contentDescription = song.name,
                modifier = Modifier
                    .size(48.dp)
                    .aspectRatio(1f)
                    .clickable(onClick = onClick),
                contentScale = ContentScale.Crop
            )

            // Song info
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onClick)
            ) {
                Text(
                    text = song.name,
                    color = if (isPlaying) accentColor2 else text,
                    style = adaptiveTypography().bodyMedium,
                    fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${Formatter.formatTime(song.length)} · ${playCountOf(song)} plays",
                    color = text.copy(alpha = 0.6f),
                    style = adaptiveTypography().labelSmall,
                    letterSpacing = 0.5.sp
                )
            }

            // Position indicator
            Text(
                text = "${index + 1}",
                color = cardBorder.copy(alpha = 0.6f),
                style = adaptiveTypography().labelMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(end = 8.dp)
            )
        }
    }
}
