package com.yatharth.musitify.service

import com.yatharth.musitify.data.dao.SettingsDao
import com.yatharth.musitify.data.entity.Settings
import java.util.UUID

/**
 * Port of Android `IdManager`. Android also injected Song/Playlist/SongToPlaylist DAOs but never
 * used them; only [SettingsDao] is kept.
 */
class IdManager(
    private val settingsDao: SettingsDao
) {

    /** Creates a random device id and upserts it as setting `device_id` (overwrites, as Android). */
    suspend fun generateDeviceId(): String {
        val id = UUID.randomUUID().toString()
        settingsDao.updateSetting(
            Settings(
                name = "device_id",
                value = id,
                updated_on = System.currentTimeMillis()
            )
        )
        return id
    }
}
