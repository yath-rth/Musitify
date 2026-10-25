package com.yatharth.musitify

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.yatharth.musitify.ui.elements.ArtImage
import com.yatharth.musitify.ui.elements.CustomAlertBox
import com.yatharth.musitify.ui.elements.CustomButton
import com.yatharth.musitify.ui.elements.CustomCircularButton
import com.yatharth.musitify.ui.elements.CustomSwitch
import com.yatharth.musitify.ui.elements.FillBar
import com.yatharth.musitify.ui.elements.MusicRecord
import com.yatharth.musitify.ui.elements.ShadowBox
import com.yatharth.musitify.ui.elements.SortIcon
import com.yatharth.musitify.ui.elements.SortMode
import com.yatharth.musitify.ui.theme.MusitifyTheme
import com.yatharth.musitify.ui.theme.accentColor1
import com.yatharth.musitify.ui.theme.background
import com.yatharth.musitify.ui.theme.cardBorder

// Temporary component gallery — will be replaced by the real app shell.
fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Musitify",
        state = rememberWindowState(width = 1100.dp, height = 800.dp),
    ) {
        MusitifyTheme {
            var playing by remember { mutableStateOf(true) }
            var checked by remember { mutableStateOf(false) }
            var sort by remember { mutableStateOf(SortMode.DOWNLOADED_DESC) }
            var position by remember { mutableDoubleStateOf(70_000.0) }
            var dontShow by remember { mutableStateOf(false) }
            val showAlert = remember { mutableStateOf(false) }
            var log by remember { mutableStateOf("-") }

            Row(
                Modifier.fillMaxSize().background(background).padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                Column(
                    Modifier.width(520.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("COMPONENTS", style = MaterialTheme.typography.labelLarge)
                    Text("last event: $log", style = MaterialTheme.typography.bodySmall)
                    ShadowBox(Modifier.padding(end = 4.dp, bottom = 4.dp)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            FillBar(
                                label = "Blinding Lights",
                                secondaryLabel = "${(position / 60000).toInt()}:%02d / 3:20".format((position / 1000).toInt() % 60),
                                value = position, max = 200_000.0,
                                onSeek = { position = it * 200_000.0; log = "seek $it" }
                            )
                            FillBar(value = 0.0, max = 0.0, secondaryLabel = null)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CustomCircularButton(onClick = { playing = !playing }, onLongClick = { log = "circular long/right" }) {
                            Icon(if (playing) Icons.Outlined.Pause else Icons.Outlined.PlayArrow, null, tint = cardBorder)
                        }
                        SortIcon(sort) { sort = sort.next(); log = "sort $sort" }
                        CustomSwitch(checked) { checked = it }
                        CustomButton(onClick = { showAlert.value = true }, onLongClick = { log = "button long/right" },
                            bgColor = accentColor1.copy(alpha = 0.2f)) {
                            Text("DELETE", Modifier.padding(10.dp), color = accentColor1, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ArtImage(null, Modifier.size(64.dp))
                        ArtImage("C:/does/not/exist.webp", Modifier.size(64.dp))
                    }
                }
                ShadowBox(Modifier.width(420.dp).padding(16.dp)) {
                    MusicRecord(artPath = null, playing = playing, imageSize = 100, modifier = Modifier.padding(16.dp))
                }
            }

            if (showAlert.value) {
                CustomAlertBox(
                    showClearConfirm = showAlert,
                    onConfirm = { log = "confirmed" },
                    title = "Delete Song",
                    body = "Are you sure you want to delete \"Blinding Lights\"?",
                    yes = "Delete", no = "Cancel",
                    dontShowAgain = dontShow,
                    onDontShowAgainChange = { dontShow = it },
                )
            }
        }
    }
}
