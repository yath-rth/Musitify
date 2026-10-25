package com.yatharth.musitify.ui.elements

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.yatharth.musitify.data.entity.Song
import com.yatharth.musitify.ui.theme.adaptiveTypography
import com.yatharth.musitify.ui.theme.text
import java.io.File

@Composable
fun SongItemUI(
    song: Song,
    onPlay: () -> Unit,
    onLongPress: () -> Unit
) {
    val artwork = remember(song.artPath) {
        File(song.artPath)
    }

    CustomButton(
        onClick = onPlay,
        onLongClick = onLongPress
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (artwork.exists()) {
                AsyncImage(
                    model = artwork,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp)
                )
            } else {
                Icon(
                    Icons.Default.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Text(
                modifier = Modifier.weight(2f),
                color = text,
                text = song.name,
                style = adaptiveTypography().bodyMedium,
                fontWeight = FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}