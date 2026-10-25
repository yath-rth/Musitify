package com.yatharth.musitify.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.yatharth.musitify.data.viewmodel.PlaybackControllerViewModel
import com.yatharth.musitify.data.viewmodel.StatsViewModel
import com.yatharth.musitify.ui.theme.background
import org.koin.compose.viewmodel.koinViewModel

// STUB — placeholder until the StatsScreen port lands (drop on merge).
@Composable
fun StatsScreen(
    modifier: Modifier = Modifier,
    onClose: () -> Unit = {},
    playbackControllerViewModel: PlaybackControllerViewModel = koinViewModel(),
    statsViewModel: StatsViewModel = koinViewModel()
) {
    Box(modifier.fillMaxSize().background(background), contentAlignment = Alignment.Center) {
        Text("YOUR STATS (stub) - press Esc")
    }
}
