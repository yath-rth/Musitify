package com.yatharth.musitify.ui.elements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yatharth.musitify.data.viewmodel.AddSongsUiState
import com.yatharth.musitify.ui.theme.accentColor1
import com.yatharth.musitify.ui.theme.adaptiveTypography
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
                text = "Add Songs",
                style = adaptiveTypography().bodyLarge,
                color = text
            )
        },
        text = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isPlaylistMode) "Playlist URL" else "Single Video URL",
                        style = adaptiveTypography().bodySmall,
                        color = text
                    )
                    CustomSwitch(
                        checked = isPlaylistMode,
                        onCheckedChange = onModeChange
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = url,
                    onValueChange = onUrlChange,
                    placeholder = {
                        Text(if (isPlaylistMode) "Paste playlist URL" else "Paste video URL")
                    },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                when (state) {
                    is AddSongsUiState.Loading -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.height(20.dp))
                            Text(
                                text = if (isPlaylistMode)
                                    "Downloading (${state.completed}/${state.total})..."
                                else "Downloading...",
                                style = adaptiveTypography().labelSmall,
                                color = text
                            )
                        }
                    }
                    is AddSongsUiState.Success -> {
                        Text(
                            text = "Added ${state.count} song${if (state.count != 1) "s" else ""}",
                            style = adaptiveTypography().labelSmall,
                            color = accentColor1,
                            fontWeight = FontWeight.Bold
                        )
                        onSuccess()
                    }
                    is AddSongsUiState.Error -> {
                        Text(
                            text = "Failed: ${state.message}",
                            style = adaptiveTypography().labelSmall,
                            color = Color.Red
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
                    text = if (state is AddSongsUiState.Loading) "Adding..." else "Add",
                    style = adaptiveTypography().bodyLarge,
                    color = accentColor1,
                    modifier = Modifier.padding(10.dp)
                )
            }
        },
        dismissButton = {
            CustomButton(onClick = onDismiss) {
                Text(
                    text = "Cancel",
                    style = adaptiveTypography().bodyLarge,
                    color = text,
                    modifier = Modifier.padding(10.dp)
                )
            }
        },
        containerColor = secondaryBg
    )
}