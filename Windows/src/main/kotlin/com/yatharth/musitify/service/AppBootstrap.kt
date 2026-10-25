package com.yatharth.musitify.service

import com.yatharth.musitify.data.repository.MediaControllerRepository
import com.yatharth.musitify.data.repository.SettingsRepository
import com.yatharth.musitify.engine.YtDlpUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.koin.core.Koin

/**
 * Desktop startup (Android `MusitifyApp.onCreate` + `MainActivity.onCreate`):
 *  1. First run (`hasOpened` not "true"): create `device_id`, then set `hasOpened`.
 *  2. Connect the player ([MediaControllerRepository.connect]).
 *  3. Kick off `YtDlpUpdater.updateIfDue()` in [backgroundScope] (returned job; null if skipped).
 */
suspend fun bootstrapApp(
    koin: Koin,
    backgroundScope: CoroutineScope,
    connectPlayer: Boolean = true,
    updateYtDlp: Boolean = true,
): Job? {
    val settingsRepository = koin.get<SettingsRepository>()
    if (!settingsRepository.hasUserOpenedApp()) {
        koin.get<IdManager>().generateDeviceId()
        settingsRepository.openedApp()
    }

    if (connectPlayer) koin.get<MediaControllerRepository>().connect()

    if (!updateYtDlp) return null
    val updater = koin.get<YtDlpUpdater>()
    return backgroundScope.launch(Dispatchers.IO) {
        runCatching { updater.updateIfDue() }
            .onFailure { if (it is kotlinx.coroutines.CancellationException) throw it }
    }
}
