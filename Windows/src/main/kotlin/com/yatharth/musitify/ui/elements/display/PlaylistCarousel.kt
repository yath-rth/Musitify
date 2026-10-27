package com.yatharth.musitify.ui.elements.display

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yatharth.musitify.data.entity.Playlist
import com.yatharth.musitify.data.entity.PlaylistInfo
import com.yatharth.musitify.ui.elements.ArtImage
import com.yatharth.musitify.ui.elements.ShadowBox
import com.yatharth.musitify.ui.theme.text
import com.yatharth.musitify.ui.theme.textSecondary
import com.yatharth.musitify.util.Formatter

/**
 * Cover-flow of playlists. Creating a playlist is a button in the section header now,
 * not a card in the row (as on Android).
 *
 * [currentPlayingIndex] is a list **index**. Tap centred ⇒ [onPlaylistSelected] with the
 * playlist **id**; tap side ⇒ scroll to it; long press / right click ⇒ [onLongClick].
 */
@Composable
fun PlaylistCarousel(
    playlists: List<PlaylistInfo>,
    currentPlayingIndex: Int,
    onPlaylistSelected: (Int) -> Unit,
    onLongClick: (Playlist) -> Unit,
    modifier: Modifier = Modifier
) {
    CoverFlowCarousel(
        itemCount = playlists.size,
        itemWidth = 200.dp,
        spacing = (-20).dp,
        focusIndex = currentPlayingIndex,
        key = { index -> playlists[index].playlist.id },
        onSelect = { index -> onPlaylistSelected(playlists[index].playlist.id) },
        onContext = { index -> onLongClick(playlists[index].playlist) },
        modifier = modifier,
    ) { index ->
        PlaylistArt(
            playlists[index],
            modifier = Modifier
                .fillMaxWidth()
                // Room for the 4dp hard shadow, which the row would clip.
                .padding(end = 4.dp, bottom = 6.dp)
        )
    }
}

@Composable
fun PlaylistArt(
    playlist: PlaylistInfo,
    modifier: Modifier
) {
    ShadowBox(
        modifier = modifier,
        shadowOffsetX = 4.dp,
        shadowOffsetY = 4.dp,
        borderThickness = 2.dp,
        rounding = 5
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            playlist.playlist.artPath?.let {
                ArtImage(
                    path = it,
                    contentDescription = playlist.playlist.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                )
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = playlist.playlist.name,
                color = text,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "${Formatter.count(playlist.numOfSongs, "song")} • ${
                    Formatter.formatTimeToMinutes(playlist.playlistLength)
                }",
                color = textSecondary,
                style = MaterialTheme.typography.labelSmall,
                letterSpacing = 0.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
