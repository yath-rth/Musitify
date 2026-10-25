package com.yatharth.musitify

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.yatharth.musitify.service.IdManager
import com.yatharth.musitify.ui.theme.MusitifyTheme
import com.yatharth.musitify.data.repository.PlaylistRepository
import com.yatharth.musitify.data.repository.SettingsRepository
import com.yatharth.musitify.data.repository.SongRepository
import com.yatharth.musitify.ui.screens.HomeScreen
import com.yatharth.musitify.util.LegacyFixer
import dagger.hilt.android.AndroidEntryPoint
import jakarta.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val TAG = "MainActivity"
    @Inject lateinit var songRepository: SongRepository
    @Inject lateinit var playlistRepository: PlaylistRepository
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var idGenerator: IdManager
    @Inject lateinit var legacyFixer: LegacyFixer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The app background is always light, so force dark status/nav bar icons
        // (the default follows the system theme and made them pale grey).
        val lightBars = SystemBarStyle.light(
            android.graphics.Color.TRANSPARENT,
            android.graphics.Color.TRANSPARENT
        )
        enableEdgeToEdge(statusBarStyle = lightBars, navigationBarStyle = lightBars)

        lifecycleScope.launch {
            val hasOpenedApp = settingsRepository.hasUserOpenedApp()
            if(!hasOpenedApp){
                val id = idGenerator.generateDeviceId()

                settingsRepository.openedApp()
            }
        }

        // One-time artist repair; no-op once it has completed.
        lifecycleScope.launch {
            try {
                legacyFixer.fixArtists()
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Artist fix failed", e)
            }
        }

//        lifecycleScope.launch {
//            playlistRepository.downloadPlaylist(
//                playlistUrl = "https://youtube.com/playlist?list=PLE6nDyTGs1nw&si=K6fr7dNUDKDeWwkt",
//                outputDir = applicationContext.getExternalFilesDir(null)?.absolutePath
//            )
//        }

        setContent {
            MusitifyTheme {
                HomeScreen(modifier = Modifier)
            }
        }
    }
}