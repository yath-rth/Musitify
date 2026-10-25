package com.yatharth.musitify.ui.elements

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
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.yatharth.musitify.data.entity.Song
import com.yatharth.musitify.ui.theme.accentColor1
import com.yatharth.musitify.ui.theme.accentColor2
import com.yatharth.musitify.ui.theme.adaptiveTypography
import com.yatharth.musitify.ui.theme.cardBorder
import com.yatharth.musitify.ui.theme.secondaryBg
import com.yatharth.musitify.ui.theme.text

@Composable
fun SongContextMenuDialog(
    song: Song,
    onDismiss: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    MusitifyDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = song.artPath,
                    contentDescription = song.name,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    contentScale = ContentScale.Crop
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.name,
                        style = adaptiveTypography().bodyMedium,
                        color = text,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = song.artist,
                        style = adaptiveTypography().labelSmall,
                        color = text.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CustomButton(
                    onClick = onAddToPlaylist,
                    bgColor = accentColor2.copy(alpha = 0.2f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = null,
                            tint = accentColor2
                        )
                        Text(
                            text = "ADD TO PLAYLIST",
                            style = adaptiveTypography().labelMedium,
                            color = accentColor2,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.2.sp
                        )
                    }
                }

                CustomButton(
                    onClick = onRename,
                    bgColor = cardBorder.copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = null,
                            tint = text
                        )
                        Text(
                            text = "RENAME",
                            style = adaptiveTypography().labelMedium,
                            color = text,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.2.sp
                        )
                    }
                }

                CustomButton(
                    onClick = onDelete,
                    bgColor = accentColor1.copy(alpha = 0.2f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = null,
                            tint = accentColor1
                        )
                        Text(
                            text = "DELETE",
                            style = adaptiveTypography().labelMedium,
                            color = accentColor1,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.2.sp
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            CustomButton(onClick = onDismiss) {
                Text(
                    text = "CANCEL",
                    style = adaptiveTypography().labelMedium,
                    color = text,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }
    )
}
