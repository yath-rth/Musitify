package com.yatharth.musitify.ui.elements.display

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yatharth.musitify.data.entity.Playlist
import com.yatharth.musitify.data.entity.PlaylistInfo
import com.yatharth.musitify.ui.elements.ArtImage
import com.yatharth.musitify.ui.elements.ShadowBox
import com.yatharth.musitify.ui.theme.accentColor2
import com.yatharth.musitify.ui.theme.text
import com.yatharth.musitify.util.Formatter

private const val ADD_PLAYLIST_KEY = "add_playlist"

/**
 * Cover-flow of playlists plus a trailing "CREATE PLAYLIST" card.
 *
 * [currentPlayingIndex] is a list **index** (Android passed the selected playlist id here
 * and also scrolled to `playlist.id` on side taps — both fixed on desktop).
 * Tap centred ⇒ [onPlaylistSelected] with the playlist **id**; tap side ⇒ scroll to it;
 * long press / right click ⇒ [onLongClick]. The create card reacts to any tap.
 */
@Composable
fun PlaylistCarousel(
    playlists: List<PlaylistInfo>,
    currentPlayingIndex: Int,
    onPlaylistSelected: (Int) -> Unit,
    onLongClick: (Playlist) -> Unit,
    onCreatePlaylist: () -> Unit,
    modifier: Modifier = Modifier
) {
    val itemWidth = 200.dp
    CoverFlowCarousel(
        itemCount = playlists.size + 1,
        itemWidth = itemWidth,
        itemHeight = itemWidth + 64.dp,
        spacing = (-20).dp,
        focusIndex = currentPlayingIndex,
        key = { index -> if (index < playlists.size) playlists[index].playlist.id else ADD_PLAYLIST_KEY },
        onSelect = { index ->
            if (index < playlists.size) onPlaylistSelected(playlists[index].playlist.id)
            else onCreatePlaylist()
        },
        onContext = { index -> if (index < playlists.size) onLongClick(playlists[index].playlist) },
        hasContext = { index -> index < playlists.size },
        selectsOffCentre = { index -> index >= playlists.size },
        modifier = modifier,
    ) { index ->
        if (index < playlists.size) {
            PlaylistArt(playlists[index], modifier = Modifier.fillMaxSize())
        } else {
            AddPlaylistCard(modifier = Modifier.fillMaxSize())
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
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "${playlist.numOfSongs} songs • ${
                    Formatter.formatTimeToMinutes(playlist.playlistLength)
                }",
                color = text.copy(alpha = 0.6f),
                style = MaterialTheme.typography.labelSmall,
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
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.5.sp
            )
        }
    }
}
