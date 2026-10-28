package com.yatharth.musitify.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import com.yatharth.musitify.ui.elements.BackDispatcher
import com.yatharth.musitify.ui.elements.LocalBackDispatcher
import com.yatharth.musitify.ui.elements.LocalTextFieldFocusTracker
import com.yatharth.musitify.ui.elements.TextFieldFocusTracker
import com.yatharth.musitify.ui.screens.HomeScreen
import com.yatharth.musitify.ui.theme.MusitifyTheme

/**
 * App shell (Android `MainActivity.setContent`): MusitifyTheme around [HomeScreen], which
 * holds both places in the app (Library and Stats tabs) and the full-screen player.
 *
 * [backDispatcher] receives Escape from the window (Android back): Now Playing → closed,
 * Stats → Library, inside a playlist → playlist carousel. [focusTracker] tells the window
 * shortcuts when a text field is focused.
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
            HomeScreen(modifier = Modifier)
        }
    }
}
