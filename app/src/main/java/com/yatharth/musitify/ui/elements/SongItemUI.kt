package com.yatharth.musitify.ui.elements

import android.util.Log
import android.util.Log.w
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yatharth.musitify.data.entity.Song
import com.yatharth.musitify.ui.theme.designSystemButton
import com.yatharth.musitify.ui.theme.designSystemShadowBox

@Composable
fun SongItemUI(
    song: Song,
    onPlay: () -> Unit
) {
    val TAG = "SongItemUI"

    designSystemShadowBox(
        modifier = Modifier.padding(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier.weight(2f),
                text = song.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            designSystemButton(
                modifier = Modifier.weight(1f),
                text = "Play",
                onClick = onPlay
            )
        }
    }
}