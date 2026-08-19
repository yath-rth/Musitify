package com.yatharth.musitify.ui.elements

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yatharth.musitify.data.viewmodel.AddSongsUiState
import com.yatharth.musitify.ui.theme.accentColor1
import com.yatharth.musitify.ui.theme.accentColor2
import com.yatharth.musitify.ui.theme.adaptiveTypography
import com.yatharth.musitify.ui.theme.cardBorder
import com.yatharth.musitify.ui.theme.itemsBg
import com.yatharth.musitify.ui.theme.secondaryBg
import com.yatharth.musitify.ui.theme.text

@Composable
fun AddSongDialog(
    onDismiss: () -> Unit,
    url: String,
    onUrlChange: (String) -> Unit,
    isPlaylistMode: Boolean,
    onModeChange: (Boolean) -> Unit,
    state: AddSongsUiState,
    onSubmit: () -> Unit,
    onSuccess: () -> Unit,
) {
    AlertDialog(
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isPlaylistMode) "PLAYLIST URL" else "SINGLE VIDEO URL",
                        style = adaptiveTypography().labelSmall,
                        color = text.copy(alpha = 0.7f),
                        fontWeight = FontWeight.ExtraLight,
                        letterSpacing = 1.2.sp
                    )
                    CustomSwitch(
                        checked = isPlaylistMode,
                        onCheckedChange = onModeChange
                    )
                }

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
                                    if (isPlaylistMode) "Paste playlist URL" else "Paste video URL",
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

                when (state) {
                    is AddSongsUiState.Loading -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.height(10.dp), strokeWidth = 2.dp, color = accentColor2)
                            Text(
                                text = if (isPlaylistMode)
                                    "DOWNLOADING (${state.completed}/${state.total})..."
                                else "DOWNLOADING...",
                                style = adaptiveTypography().labelSmall,
                                color = text.copy(alpha = 0.7f),
                                letterSpacing = 1.2.sp
                            )
                        }
                    }
                    is AddSongsUiState.Success -> {
                        Text(
                            text = "ADDED ${state.count} SONG${if (state.count != 1) "S" else ""}",
                            style = adaptiveTypography().labelSmall,
                            color = accentColor2,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.5.sp
                        )
                        onSuccess()
                    }
                    is AddSongsUiState.Error -> {
                        Text(
                            text = "FAILED: ${state.message}",
                            style = adaptiveTypography().labelSmall,
                            color = accentColor1,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.2.sp
                        )
                    }
                    AddSongsUiState.Idle -> { /* nothing */ }
                }
            }
        },
        confirmButton = {
            CustomButton(
                onClick = onSubmit,
                bgColor = accentColor1.copy(alpha = 0.2f)
            ) {
                Text(
                    text = if (state is AddSongsUiState.Loading) "ADDING..." else "ADD",
                    style = adaptiveTypography().labelMedium,
                    color = accentColor1,
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
                    fontWeight = FontWeight.ExtraLight,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.padding(10.dp)
                )
            }
        },
        containerColor = secondaryBg
    )
}