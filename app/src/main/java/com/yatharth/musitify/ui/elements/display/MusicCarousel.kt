package com.yatharth.musitify.ui.elements.display

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.common.math.Quantiles.scale
import com.yatharth.musitify.data.entity.Song
import com.yatharth.musitify.ui.elements.ShadowBox
import com.yatharth.musitify.ui.theme.adaptiveTypography
import com.yatharth.musitify.util.Formatter
import com.yatharth.musitify.util.SongMedia.playCountOf
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.abs

@Composable
fun MusicCarousel(
    modifier: Modifier = Modifier,
    songs: List<Song>,
    size: Int = 180,
    spacing: Int = -20,
    threshold: Float = 0.5f,
    scroll: Boolean = true,
    currentPlayingIndex: Int,
    onTrackSelected: (Int) -> Unit,
    onLongSelect: (Song) -> Unit
) {
    val listState = rememberLazyListState()
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val coroutineScope = rememberCoroutineScope()
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val itemWidth = (size).dp
    val sidePadding = (screenWidth - itemWidth) / 2

    if (scroll) {
        LaunchedEffect(currentPlayingIndex) {
            if (songs.isEmpty()) return@LaunchedEffect
            listState.animateScrollToItem(currentPlayingIndex)
        }
    }

    // Track which item is centered, purely for visual state (e.g. "tap to play" affordance)
    val centeredIndex by remember {
        derivedStateOf { findCenterItemIndex(listState) ?: currentPlayingIndex }
    }

    LazyRow(
        state = listState,
        flingBehavior = flingBehavior,
        contentPadding = PaddingValues(horizontal = sidePadding),
        horizontalArrangement = Arrangement.spacedBy(spacing.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        itemsIndexed(songs, key = { _, s -> s.id }) { index, song ->
            val layoutInfo = listState.layoutInfo
            val itemInfo = layoutInfo.visibleItemsInfo.find { it.index == index }

            val rawDistance = if (itemInfo == null) 1f else {
                val center = layoutInfo.viewportEndOffset / 2f
                val itemCenter = itemInfo.offset + itemInfo.size / 2f
                ((itemCenter - center) / (layoutInfo.viewportEndOffset / 2f)).coerceIn(-1f, 1f)
            }

            val distanceFromCenter = if (abs(rawDistance) < threshold) 0f else rawDistance

            val scaleTarget = lerp(1f, 0.75f, abs(distanceFromCenter))
            val rotationTarget = lerp(0f, 45f, distanceFromCenter)
            val alphaTarget = lerp(1f, 0.5f, abs(distanceFromCenter))
            val isNearCenter = abs(distanceFromCenter) < 0.15f

//            val scale by animateFloatAsState(
//                targetValue = scaleTarget,
//                animationSpec = spring(
//                    dampingRatio = Spring.DampingRatioNoBouncy,
//                    stiffness = Spring.StiffnessMedium
//                )
//            )
//            val rotationY by animateFloatAsState(
//                targetValue = rotationTarget,
//                animationSpec = spring(
//                    dampingRatio = Spring.DampingRatioNoBouncy,
//                    stiffness = Spring.StiffnessMedium
//                )
//            )
//            val alpha by animateFloatAsState(
//                targetValue = alphaTarget,
//                animationSpec = spring(
//                    dampingRatio = Spring.DampingRatioNoBouncy,
//                    stiffness = Spring.StiffnessMedium
//                )
//            )

            Box(
                modifier = Modifier
                    .width(itemWidth)
                    .height(itemWidth + 64.dp)
                    .graphicsLayer {
                        scaleX = scaleTarget
                        scaleY = scaleTarget
                        this.rotationY = rotationTarget
                        this.alpha = alphaTarget
                        cameraDistance = 12f * density
                    }
                    .combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onLongClick = { onLongSelect(song) },
                        onClick = {
                            if (isNearCenter) {
                                onTrackSelected(index)
                            } else {
                                coroutineScope.launch {
                                    listState.animateScrollToItem(index)
                                }
                            }
                        }
                    )
            ) {
                AlbumArt(song, modifier = Modifier.fillMaxSize())
            }
        }
    }
}

fun lerp(start: Float, stop: Float, fraction: Float): Float {
    return start + (stop - start) * fraction
}

fun findCenterItemIndex(listState: LazyListState): Int? {
    val layoutInfo = listState.layoutInfo
    val center = layoutInfo.viewportEndOffset / 2
    return layoutInfo.visibleItemsInfo.minByOrNull { item ->
        abs((item.offset + item.size / 2) - center)
    }?.index
}

@Composable
fun AlbumArt(
    song: Song,
    modifier: Modifier,
) {

    val artwork = remember(song.artPath) {
        File(song.artPath)
    }

    ShadowBox(
        modifier = modifier,
        shadowOffsetX = 0.dp,
        shadowOffsetY = 0.dp,
        borderThickness = 5.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            AsyncImage(
                model = artwork,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                contentScale = ContentScale.Crop
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = song.name,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = adaptiveTypography().bodyMedium
            )

            Text(
                text = Formatter.formatTime(song.length) + " • " + playCountOf(song) + " plays",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = adaptiveTypography().bodySmall
            )
        }
    }
}