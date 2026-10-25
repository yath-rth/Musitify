package com.yatharth.musitify.ui.elements.display

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
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
import com.yatharth.musitify.data.entity.Song
import com.yatharth.musitify.ui.elements.ArtImage
import com.yatharth.musitify.ui.elements.ShadowBox
import com.yatharth.musitify.ui.theme.text
import com.yatharth.musitify.util.Formatter
import com.yatharth.musitify.util.SongMedia.playCountOf

/**
 * 3D cover-flow of songs (see [CoverFlowCarousel] for the desktop input handling).
 * Tap the centred card ⇒ [onTrackSelected]; tap a side card ⇒ scroll to it;
 * long press / right click ⇒ [onLongSelect]. With [scroll] the row follows
 * [currentPlayingIndex] (playlist view); the All Songs carousel passes `scroll = false`.
 */
@Composable
fun MusicCarousel(
    modifier: Modifier = Modifier,
    songs: List<Song>,
    size: Int = 180,
    spacing: Int = -20,
    scroll: Boolean = true,
    currentPlayingIndex: Int,
    onTrackSelected: (Int) -> Unit,
    onLongSelect: (Song) -> Unit
) {
    CoverFlowCarousel(
        itemCount = songs.size,
        itemWidth = size.dp,
        itemHeight = size.dp + 80.dp,
        spacing = spacing.dp,
        focusIndex = if (scroll) currentPlayingIndex else null,
        key = { songs[it].id },
        onSelect = onTrackSelected,
        onContext = { onLongSelect(songs[it]) },
        modifier = modifier,
    ) { index ->
        AlbumArt(songs[index], modifier = Modifier.fillMaxSize(), isPlaying = index == currentPlayingIndex)
    }
}

@Composable
fun AlbumArt(
    song: Song,
    modifier: Modifier,
    @Suppress("UNUSED_PARAMETER") isPlaying: Boolean = false, // unused visually, as on Android
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
            ArtImage(
                path = song.artPath,
                contentDescription = song.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = song.name,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
                color = text,
                fontWeight = FontWeight.Normal
            )

            Text(
                text = Formatter.formatTime(song.length) + " • " + playCountOf(song) + " plays",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelSmall,
                color = text.copy(alpha = 0.6f),
                fontWeight = FontWeight.ExtraLight,
                letterSpacing = 0.5.sp
            )
        }
    }
}
