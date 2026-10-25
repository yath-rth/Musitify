package com.yatharth.musitify.ui.elements

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import com.yatharth.musitify.data.viewmodel.HomeScreenViewModel
import com.yatharth.musitify.data.viewmodel.SettingsViewModel
import com.yatharth.musitify.ui.theme.accentColor1
import com.yatharth.musitify.ui.theme.accentColor2
import com.yatharth.musitify.ui.theme.cardBorder
import com.yatharth.musitify.ui.theme.itemsBg
import com.yatharth.musitify.ui.theme.text
import org.koin.compose.viewmodel.koinViewModel
import java.io.File
import javax.swing.JFileChooser
import javax.swing.UIManager

/**
 * Desktop-only settings dialog in the shared Tracknix dialog style:
 * sync server URL, music folder (native folder chooser), "Sync now", and
 * read-only app version + device id. "SAVE" persists the server URL; the music
 * folder is applied as soon as it is chosen.
 */
@Composable
fun SettingsDialog(onDismiss: () -> Unit) {
    val settingsViewModel = koinViewModel<SettingsViewModel>()
    val homeScreenViewModel = koinViewModel<HomeScreenViewModel>()

    val serverUrl by settingsViewModel.serverUrl.collectAsState()
    val musicDir by settingsViewModel.musicDir.collectAsState()
    val deviceId by settingsViewModel.deviceId.collectAsState()
    val syncMessage by homeScreenViewModel.message.collectAsState()

    var urlInput by remember { mutableStateOf(serverUrl) }
    var urlEdited by remember { mutableStateOf(false) }
    var syncRequested by remember { mutableStateOf(false) }
    var musicDirChanged by remember { mutableStateOf(false) }

    // The stored URL loads asynchronously; follow it until the user starts typing.
    LaunchedEffect(serverUrl) { if (!urlEdited) urlInput = serverUrl }

    MusitifyDialog(
        onDismissRequest = onDismiss,
        title = "SETTINGS",
        confirmButton = {
            DialogConfirmButton(
                label = "SAVE",
                onClick = {
                    settingsViewModel.setServerUrl(urlInput)
                    onDismiss()
                },
                pill = true,
            )
        },
        dismissButton = { DialogCancelButton(onClick = onDismiss, pill = true) },
    ) {
        Column(
            modifier = Modifier.widthIn(min = 320.dp, max = 480.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Server ──────────────────────────────────────────────────────
            SectionLabel("SYNC SERVER URL")
            InputCard {
                OutlinedTextField(
                    value = urlInput,
                    onValueChange = {
                        urlInput = it
                        urlEdited = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium,
                    placeholder = {
                        Text(
                            text = SettingsViewModel.DEFAULT_SERVER_URL,
                            style = MaterialTheme.typography.bodyMedium,
                            color = text.copy(alpha = 0.4f)
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = text,
                        unfocusedTextColor = text,
                        focusedContainerColor = itemsBg,
                        unfocusedContainerColor = itemsBg,
                        cursorColor = accentColor2,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                    )
                )
            }

            // ── Music folder ────────────────────────────────────────────────
            SectionLabel("MUSIC FOLDER")
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = musicDir,
                    style = MaterialTheme.typography.bodySmall,
                    color = text,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                ActionButton(
                    label = "CHANGE…",
                    icon = { Icon(Icons.Outlined.Folder, contentDescription = null, tint = text) },
                    bgColor = cardBorder.copy(alpha = 0.1f),
                    color = text,
                    onClick = {
                        chooseFolder(musicDir)?.let {
                            settingsViewModel.setMusicDir(it)
                            musicDirChanged = true
                        }
                    }
                )
            }
            Hint(
                if (musicDirChanged) "New downloads will be saved here. Existing songs stay where they are."
                else "New downloads are saved to this folder. Existing songs stay where they are."
            )

            // ── Sync ────────────────────────────────────────────────────────
            SectionLabel("LIBRARY SYNC")
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ActionButton(
                    label = "SYNC NOW",
                    icon = { Icon(Icons.Outlined.Sync, contentDescription = null, tint = accentColor2) },
                    bgColor = accentColor2.copy(alpha = 0.2f),
                    color = accentColor2,
                    onClick = {
                        homeScreenViewModel.clearMessage()
                        homeScreenViewModel.syncLibrary()
                        syncRequested = true
                    }
                )
                val status = syncMessage?.let { "FAILED: $it" }
                    ?: if (syncRequested) "SYNC STARTED" else null
                status?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (syncMessage != null) accentColor1 else accentColor2,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.2.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ── About ───────────────────────────────────────────────────────
            SectionLabel("ABOUT")
            SelectionContainer {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    InfoLine("Version", appVersion())
                    InfoLine("Device ID", deviceId ?: "—")
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = text.copy(alpha = 0.7f),
        fontWeight = FontWeight.ExtraLight,
        letterSpacing = 1.2.sp
    )
}

@Composable
private fun Hint(textValue: String) {
    Text(
        text = textValue,
        style = MaterialTheme.typography.labelSmall,
        color = text.copy(alpha = 0.5f),
        letterSpacing = 0.5.sp
    )
}

@Composable
private fun InfoLine(label: String, value: String) {
    Text(
        text = "$label: $value",
        style = MaterialTheme.typography.bodySmall,
        color = text.copy(alpha = 0.7f),
        fontWeight = FontWeight.Medium
    )
}

/** "Tracknix input field": hard 4/4 shadow behind a 5 dp bordered card. */
@Composable
private fun InputCard(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(end = 4.dp, bottom = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(4.dp, 4.dp)
                .background(cardBorder, RoundedCornerShape(5.dp))
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = itemsBg),
            shape = RoundedCornerShape(5.dp),
            border = BorderStroke(1.5.dp, cardBorder)
        ) { content() }
    }
}

/** Context-menu style button (icon + label), as in SongContextMenuDialog. */
@Composable
private fun ActionButton(
    label: String,
    icon: @Composable () -> Unit,
    bgColor: Color,
    color: Color,
    onClick: () -> Unit,
) {
    CustomButton(onClick = onClick, bgColor = bgColor) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            icon()
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = color,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.2.sp
            )
        }
    }
}

/** jpackage sets `jpackage.app-version` in packaged builds; dev runs fall back to the build version. */
private fun appVersion(): String =
    System.getProperty("jpackage.app-version")?.takeIf { it.isNotBlank() } ?: APP_VERSION_FALLBACK

private const val APP_VERSION_FALLBACK = "1.0.0"

private var systemLookAndFeelApplied = false

/**
 * Native-looking (Windows LAF) modal folder chooser. Runs on the AWT event thread,
 * which is Compose Desktop's main thread, so it can be called from a click handler.
 */
private fun chooseFolder(current: String): File? {
    if (!systemLookAndFeelApplied) {
        runCatching { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()) }
        systemLookAndFeelApplied = true
    }
    val chooser = JFileChooser(current.takeIf { it.isNotBlank() }?.let(::File)).apply {
        fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
        isAcceptAllFileFilterUsed = false
        dialogTitle = "Choose music folder"
        approveButtonText = "Select folder"
    }
    return if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) chooser.selectedFile else null
}
