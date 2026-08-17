package com.yatharth.musitify

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.Modifier
import com.yatharth.musitify.service.IdManager
import com.yatharth.musitify.ui.theme.MusitifyTheme
import com.yatharth.musitify.data.repository.PlaylistRepository
import com.yatharth.musitify.data.repository.SongRepository
import com.yatharth.musitify.ui.screens.HomeScreen
import dagger.hilt.android.AndroidEntryPoint
import jakarta.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val TAG = "MainActivity"
    @Inject lateinit var songRepository: SongRepository
    @Inject lateinit var playlistRepository: PlaylistRepository
    @Inject lateinit var idGenerator: IdManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

//        lifecycleScope.launch {
//            playlistRepository.downloadPlaylist(
//                playlistUrl = "https://youtube.com/playlist?list=PLE6nDyTGs1nw&si=K6fr7dNUDKDeWwkt",
//                outputDir = applicationContext.getExternalFilesDir(null)?.absolutePath
//            )
//        }

        setContent {
            MusitifyTheme {
                HomeScreen(
                    modifier = Modifier
                )
            }
        }
    }
}