package com.yatharth.musitify.ui.elements

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.yatharth.musitify.data.entity.Song
import com.yatharth.musitify.ui.theme.cardBorder
import com.yatharth.musitify.ui.theme.text
import com.yatharth.musitify.ui.theme.textSecondary
import com.yatharth.musitify.util.Formatter
import com.yatharth.musitify.util.SongDisplay

private val RowSpacing = 10.dp

/**
 * Playlist songs you can reorder by dragging the handle (mouse or touch). The order
 * changes under the pointer and is saved once when you let go. Screen readers get
 * "Move up" / "Move down" actions instead.
 *
 * A plain Column (not a nested lazy list with a fixed height), so it scrolls
 * with the rest of the screen.
 */
@Composable
fun ReorderableSongList(
    songs: List<Song>,
    currentPlayingIndex: Int,
    onTrackSelected: (Int) -> Unit,
    onReorder: (fromIndex: Int, toIndex: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // Local order while dragging; replaced when the saved order comes back.
    var order by remember(songs) { mutableStateOf(songs) }
    var draggingId by remember { mutableStateOf<Int?>(null) }
    var dragStartIndex by remember { mutableIntStateOf(-1) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var rowHeightPx by remember { mutableIntStateOf(0) }
    val spacingPx = with(LocalDensity.current) { RowSpacing.toPx() }
    val playingId = songs.getOrNull(currentPlayingIndex)?.id

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(RowSpacing)
    ) {
        order.forEachIndexed { index, song ->
            key(song.id) {
                val dragging = song.id == draggingId
                ReorderableSongRow(
                    song = song,
                    position = index + 1,
                    isPlaying = song.id == playingId,
                    dragging = dragging,
                    onClick = { onTrackSelected(songs.indexOfFirst { it.id == song.id }) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onSizeChanged { rowHeightPx = it.height }
                        .zIndex(if (dragging) 1f else 0f)
                        .graphicsLayer {
                            translationY = if (dragging) dragOffset else 0f
                            val s = if (dragging) 1.02f else 1f
                            scaleX = s
                            scaleY = s
                        }
                        .semantics {
                            customActions = buildList {
                                if (index > 0) add(CustomAccessibilityAction("Move up") {
                                    onReorder(index, index - 1); true
                                })
                                if (index < order.lastIndex) add(CustomAccessibilityAction("Move down") {
                                    onReorder(index, index + 1); true
                                })
                            }
                        },
                    handleModifier = Modifier
                        .pointerHoverIcon(PointerIcon.Hand)
                        .pointerInput(song.id) {
                            detectDragGestures(
                                onDragStart = {
                                    draggingId = song.id
                                    dragStartIndex = order.indexOfFirst { it.id == song.id }
                                    dragOffset = 0f
                                },
                                onDrag = { change, amount ->
                                    change.consume()
                                    dragOffset += amount.y
                                    val step = rowHeightPx + spacingPx
                                    val current = order.indexOfFirst { it.id == song.id }
                                    // Past half a row: swap with the neighbour, and shift
                                    // the offset so the row stays under the pointer.
                                    if (dragOffset > step / 2 && current < order.lastIndex) {
                                        order = order.toMutableList().apply { add(current + 1, removeAt(current)) }
                                        dragOffset -= step
                                    } else if (dragOffset < -step / 2 && current > 0) {
                                        order = order.toMutableList().apply { add(current - 1, removeAt(current)) }
                                        dragOffset += step
                                    }
                                },
                                onDragEnd = {
                                    val to = order.indexOfFirst { it.id == song.id }
                                    if (dragStartIndex >= 0 && to != dragStartIndex) onReorder(dragStartIndex, to)
                                    draggingId = null
                                    dragOffset = 0f
                                },
                                onDragCancel = {
                                    order = songs
                                    draggingId = null
                                    dragOffset = 0f
                                }
                            )
                        }
                )
            }
        }
    }
}

@Composable
private fun ReorderableSongRow(
    song: Song,
    position: Int,
    isPlaying: Boolean,
    dragging: Boolean,
    onClick: () -> Unit,
    handleModifier: Modifier,
    modifier: Modifier = Modifier
) {
    ShadowBox(
        modifier = modifier,
        shadowOffsetX = if (dragging) 6.dp else 3.dp,
        shadowOffsetY = if (dragging) 6.dp else 3.dp,
        borderThickness = 2.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 12.dp, top = 8.dp, bottom = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.DragHandle,
                contentDescription = null,
                tint = cardBorder,
                modifier = handleModifier
                    .padding(8.dp)
                    .size(24.dp)
            )

            Row(
                modifier = Modifier
                    .weight(1f)
                    .handCursor()
                    .clickable(onClick = onClick),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ArtImage(
                    path = song.artPath,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(3.dp))
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = SongDisplay.title(song),
                        color = text,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isPlaying) FontWeight.ExtraBold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isPlaying) "Now playing"
                        else listOfNotNull(SongDisplay.artist(song), Formatter.formatTime(song.length))
                            .joinToString(" · "),
                        color = textSecondary,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            Text(
                text = "$position",
                color = textSecondary,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}
