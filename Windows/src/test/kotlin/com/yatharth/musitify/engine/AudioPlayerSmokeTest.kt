package com.yatharth.musitify.engine

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Plays a real file through JavaFX. Opt-in (needs an audio device):
 *   MUSITIFY_PLAYER_SMOKE=<path to .mp3> ./gradlew test --tests "*AudioPlayerSmokeTest*"
 */
class AudioPlayerSmokeTest {
    @Test
    fun playsSeeksAndEnds() {
        val path = System.getenv("MUSITIFY_PLAYER_SMOKE") ?: return
        val player = AudioPlayer()
        val track = Track(1, path, "smoke", "smoke", null, 0)
        runBlocking {
            player.setVolume(0.05)
            player.playQueue(listOf(track, track.copy(id = 2)), 0)
            withTimeout(10_000) { player.state.first { it.isPlaying && it.positionMs > 800 } }
            val s = player.state.value
            println("PLAYER SMOKE: playing pos=${s.positionMs} dur=${s.durationMs}")
            assertTrue(s.durationMs > 0)

            player.next()
            withTimeout(5_000) { player.state.first { it.currentIndex == 1 } }
            player.previous() // <=3s in -> back to item 0
            withTimeout(5_000) { player.state.first { it.currentIndex == 0 } }

            player.setRepeatMode(RepeatMode.OFF)
            player.skipTo(1)
            withTimeout(5_000) { player.state.first { it.currentIndex == 1 && it.isPlaying } }
            player.seekTo(player.state.value.durationMs - 700)
            val end = withTimeout(10_000) { player.state.first { it.ended } }
            println("PLAYER SMOKE: ended=${end.ended} isPlaying=${end.isPlaying} idx=${end.currentIndex}")
            assertEquals(1, end.currentIndex)
            assertTrue(!end.isPlaying)
        }
        player.shutdown()
    }
}
