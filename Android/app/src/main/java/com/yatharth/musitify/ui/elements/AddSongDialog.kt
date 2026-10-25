package com.yatharth.musitify.ui.elements

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yatharth.musitify.data.viewmodel.AddSongsUiState
import com.yatharth.musitify.ui.theme.accentColor1
import com.yatharth.musitify.ui.theme.primaryAction
import com.yatharth.musitify.ui.theme.accentColor2
import com.yatharth.musitify.ui.theme.adaptiveTypography
import com.yatharth.musitify.ui.theme.cardBorder
import com.yatharth.musitify.ui.theme.itemsBg
import com.yatharth.musitify.ui.theme.secondaryBg
import com.yatharth.musitify.ui.theme.text
import com.yatharth.musitify.ui.theme.textSecondary
import com.yatharth.musitify.util.LinkDetector

@Composable
fun AddSongDialog(
    onDismiss: () -> Unit,
    url: String,
    onUrlChange: (String) -> Unit,
    state: AddSongsUiState,
    onSubmit: () -> Unit,
) {
    val isPlaylist = LinkDetector.isPlaylist(url)

    MusitifyDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "ADD SONGS",
                style = adaptiveTypography().labelLarge,
                color = text,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "YOUTUBE LINK",
                    style = adaptiveTypography().labelSmall,
                    color = textSecondary
                )

                // Tracknix input field pattern: card with shadow offset
                Box(modifier = Modifier.fillMaxWidth().padding(end = 4.dp, bottom = 4.dp)) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .offset(x = 4.dp, y = 4.dp)
                            .background(cardBorder, RoundedCornerShape(5.dp))
                    )
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = itemsBg),
                        shape = RoundedCornerShape(5.dp),
                        border = BorderStroke(1.5.dp, cardBorder)
                    ) {
                        OutlinedTextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = url,
                            onValueChange = onUrlChange,
                            placeholder = {
                                Text(
                                    "Paste a video or playlist link",
                                    style = adaptiveTypography().bodyMedium,
                                    color = text.copy(alpha = 0.4f)
                                )
                            },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = text,
                                unfocusedTextColor = text,
                                focusedContainerColor = itemsBg,
                                unfocusedContainerColor = itemsBg,
                                cursorColor = accentColor2,
                            )
                        )
                    }
                }

                // The link decides what gets added; say what we understood.
                Text(
                    text = when {
                        url.isBlank() -> "A playlist link adds every song in it."
                        isPlaylist -> "Playlist link: every song will be added."
                        else -> "Song link: this one song will be added."
                    },
                    style = adaptiveTypography().bodySmall,
                    color = textSecondary
                )

                if (state is AddSongsUiState.Error) {
                    Text(
                        text = "FAILED: ${state.message}",
                        style = adaptiveTypography().labelSmall,
                        color = accentColor1,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.2.sp
                    )
                }
            }
        },
        confirmButton = {
            CustomButton(
                onClick = onSubmit,
                bgColor = primaryAction.copy(alpha = 0.2f)
            ) {
                Text(
                    text = if (isPlaylist) "ADD PLAYLIST" else "ADD SONG",
                    style = adaptiveTypography().labelMedium,
                    color = primaryAction,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.padding(10.dp)
                )
            }
        },
        dismissButton = {
            CustomButton(onClick = onDismiss) {
                Text(
                    text = "CANCEL",
                    style = adaptiveTypography().labelMedium,
                    color = text,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }
    )
}