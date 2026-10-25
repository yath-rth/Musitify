package com.yatharth.musitify.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.yatharth.musitify.ui.elements.BackDispatcher
import com.yatharth.musitify.ui.elements.BackHandler
import com.yatharth.musitify.ui.elements.LocalBackDispatcher
import com.yatharth.musitify.ui.elements.LocalTextFieldFocusTracker
import com.yatharth.musitify.ui.elements.TextFieldFocusTracker
import com.yatharth.musitify.ui.screens.HomeScreen
import com.yatharth.musitify.ui.screens.StatsScreen
import com.yatharth.musitify.ui.theme.MusitifyTheme

/**
 * App shell (Android `MainActivity.setContent`): MusitifyTheme and the Home ⇄ Stats
 * switch driven by one `showStats` flag, no transition.
 *
 * [backDispatcher] receives Escape from the window (Android back): Stats → Home, inside a
 * playlist → playlist carousel. [focusTracker] tells the window shortcuts when a text field
 * is focused.
 */
@Composable
fun App(
    backDispatcher: BackDispatcher,
    focusTracker: TextFieldFocusTracker,
) {
    CompositionLocalProvider(
        LocalBackDispatcher provides backDispatcher,
        LocalTextFieldFocusTracker provides focusTracker,
    ) {
        MusitifyTheme {
            var showStats by rememberSaveable { mutableStateOf(false) }

            // StatsScreen: BackHandler(onBack = onClose).
            BackHandler(enabled = showStats) { showStats = false }

            if (showStats) {
                StatsScreen(
                    modifier = Modifier,
                    onClose = { showStats = false }
                )
            } else {
                HomeScreen(
                    modifier = Modifier,
                    onOpenStats = { showStats = true }
                )
            }
        }
    }
}
