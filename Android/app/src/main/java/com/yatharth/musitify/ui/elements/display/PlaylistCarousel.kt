package com.yatharth.musitify.ui.elements.display

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.yatharth.musitify.data.entity.Playlist
import com.yatharth.musitify.data.entity.PlaylistInfo
import com.yatharth.musitify.ui.elements.ShadowBox
import com.yatharth.musitify.ui.theme.accentColor2
import com.yatharth.musitify.ui.theme.adaptiveTypography
import com.yatharth.musitify.ui.theme.cardBorder
import com.yatharth.musitify.ui.theme.text
import com.yatharth.musitify.util.Formatter
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.abs

@Composable
fun PlaylistCarousel(
    playlists: List<PlaylistInfo>,
    currentPlayingIndex: Int,
    onPlaylistSelected: (Int) -> Unit,
    onLongClick: (Playlist) -> Unit,
    onCreatePlaylist: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val coroutineScope = rememberCoroutineScope()
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val itemWidth = 200.dp
    val sidePadding = (screenWidth - itemWidth) / 2

    LaunchedEffect(currentPlayingIndex) {
        if (playlists.isEmpty()) return@LaunchedEffect
        listState.animateScrollToItem(currentPlayingIndex)
    }

    LazyRow(
        state = listState,
        flingBehavior = flingBehavior,
        contentPadding = PaddingValues(horizontal = sidePadding),
        horizontalArrangement = Arrangement.spacedBy((-20).dp),
        modifier = modifier.fillMaxWidth()
    ) {
        itemsIndexed(playlists, key = { _, s -> s.playlist.id }) { index, playlistInfo ->
            val layoutInfo = listState.layoutInfo
            val itemInfo = layoutInfo.visibleItemsInfo.find { it.index == index }

            val rawDistance = if (itemInfo == null) 1f else {
                val center = layoutInfo.viewportEndOffset / 2f
                val itemCenter = itemInfo.offset + itemInfo.size / 2f
                ((itemCenter - center) / (layoutInfo.viewportEndOffset / 2f)).coerceIn(-1f, 1f)
            }

            val distanceFromCenter = if (abs(rawDistance) < 0.5f) 0f else rawDistance

            val scale = lerp(1f, 0.75f, abs(distanceFromCenter))
            val rotationY = lerp(0f, 45f, distanceFromCenter)
            val alpha = lerp(1f, 0.5f, abs(distanceFromCenter))
            val isNearCenter = abs(distanceFromCenter) < 0.15f

            Box(
                modifier = Modifier
                    .width(itemWidth)
                    .height(itemWidth + 64.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.rotationY = rotationY
                        this.alpha = alpha
                        cameraDistance = 12f * density
                    }
                    .combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onLongClick = { onLongClick(playlistInfo.playlist) },
                        onClick = {
                            if (isNearCenter) {
                                onPlaylistSelected(playlistInfo.playlist.id)
                            } else {
                                coroutineScope.launch {
                                    listState.animateScrollToItem(playlistInfo.playlist.id)
                                }
                            }
                        }
                    )
            ) {
                PlaylistArt(playlistInfo, modifier = Modifier.fillMaxSize())
            }
        }

        // Add playlist card
        item(key = "add_playlist") {
            val index = playlists.size
            val layoutInfo = listState.layoutInfo
            val itemInfo = layoutInfo.visibleItemsInfo.find { it.index == index }

            val rawDistance = if (itemInfo == null) 1f else {
                val center = layoutInfo.viewportEndOffset / 2f
                val itemCenter = itemInfo.offset + itemInfo.size / 2f
                ((itemCenter - center) / (layoutInfo.viewportEndOffset / 2f)).coerceIn(-1f, 1f)
            }

            val distanceFromCenter = if (abs(rawDistance) < 0.5f) 0f else rawDistance

            val scale = lerp(1f, 0.75f, abs(distanceFromCenter))
            val rotationY = lerp(0f, 45f, distanceFromCenter)
            val alpha = lerp(1f, 0.5f, abs(distanceFromCenter))

            Box(
                modifier = Modifier
                    .width(itemWidth)
                    .height(itemWidth + 64.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.rotationY = rotationY
                        this.alpha = alpha
                        cameraDistance = 12f * density
                    }
                    .combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onCreatePlaylist
                    )
            ) {
                AddPlaylistCard(modifier = Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
fun PlaylistArt(
    playlist: PlaylistInfo,
    modifier: Modifier
) {
    ShadowBox(
        modifier = modifier,
        shadowOffsetX = 0.dp,
        shadowOffsetY = 0.dp,
        borderThickness = 6.dp,
        rounding = 5
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            playlist.playlist.artPath?.let {
                AsyncImage(
                    model = File(it),
                    contentDescription = playlist.playlist.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = playlist.playlist.name,
                color = text,
                style = adaptiveTypography().bodyLarge,
                fontWeight = FontWeight.Normal,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "${playlist.numOfSongs} songs \u2022 ${
                    Formatter.formatTimeToMinutes(playlist.playlistLength)
                }",
                color = text.copy(alpha = 0.6f),
                style = adaptiveTypography().labelSmall,
                fontWeight = FontWeight.ExtraLight,
                letterSpacing = 0.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun AddPlaylistCard(
    modifier: Modifier
) {
    ShadowBox(
        modifier = modifier,
        shadowOffsetX = 0.dp,
        shadowOffsetY = 0.dp,
        borderThickness = 6.dp,
        rounding = 5,
        bgColor = accentColor2.copy(alpha = 0.1f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = "Create playlist",
                modifier = Modifier.size(64.dp),
                tint = accentColor2
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "CREATE PLAYLIST",
                color = accentColor2,
                style = adaptiveTypography().labelLarge,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.5.sp
            )
        }
    }
}
