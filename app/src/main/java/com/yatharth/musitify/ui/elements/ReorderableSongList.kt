package com.yatharth.musitify.ui.elements

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
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
    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var targetIndex by remember { mutableStateOf<Int?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(
            items = songs,
            key = { _, song -> song.id }
        ) { index, song ->
            val isBeingDragged = draggedIndex == index
            val isPlaying = index == currentPlayingIndex

            ReorderableSongRow(
                song = song,
                isPlaying = isPlaying,
                isBeingDragged = isBeingDragged,
                onClick = { onTrackSelected(index) },
                onDragStart = { draggedIndex = index },
                onDragEnd = {
                    draggedIndex?.let { from ->
                        targetIndex?.let { to ->
                            if (from != to) {
                                onReorder(from, to)
                            }
                        }
                    }
                    draggedIndex = null
                    targetIndex = null
                },
                onDraggedOver = { targetIndex = index },
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(if (isBeingDragged) 1f else 0f)
            )
        }
    }
}

@Composable
private fun ReorderableSongRow(
    song: Song,
    isPlaying: Boolean,
    isBeingDragged: Boolean,
    onClick: () -> Unit,
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit,
    onDraggedOver: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .graphicsLayer {
                alpha = if (isBeingDragged) 0.7f else 1f
                scaleX = if (isBeingDragged) 1.05f else 1f
                scaleY = if (isBeingDragged) 1.05f else 1f
            }
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        isDragging = true
                        onDragStart()
                    },
                    onDragEnd = {
                        isDragging = false
                        onDragEnd()
                    },
                    onDragCancel = {
                        isDragging = false
                        onDragEnd()
                    },
                    onDrag = { _, _ ->
                        onDraggedOver()
                    }
                )
            },
        colors = CardDefaults.cardColors(
            containerColor = if (isPlaying) itemsBg.copy(alpha = 0.8f) else itemsBg
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.DragHandle,
                contentDescription = "Drag to reorder",
                tint = cardBorder.copy(alpha = 0.6f),
                modifier = Modifier.size(24.dp)
            )

            AsyncImage(
                model = File(song.artPath),
                contentDescription = song.name,
                modifier = Modifier
                    .size(48.dp)
                    .aspectRatio(1f),
                contentScale = ContentScale.Crop
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = song.name,
                    color = if (isPlaying) accentColor2 else text,
                    style = adaptiveTypography().bodyMedium,
                    fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${Formatter.formatTime(song.length)} · ${playCountOf(song)} plays",
                    color = text.copy(alpha = 0.6f),
                    style = adaptiveTypography().labelSmall,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}
