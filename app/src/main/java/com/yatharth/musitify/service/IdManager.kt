package com.yatharth.musitify.service

import com.yatharth.musitify.data.dao.PlaylistDao
import com.yatharth.musitify.data.dao.SettingsDao
import com.yatharth.musitify.data.dao.SongDao
import com.yatharth.musitify.data.dao.SongToPlaylistDao
import com.yatharth.musitify.data.entity.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IdManager @Inject constructor(
    private val songDao: SongDao,
    private val playlistDao: PlaylistDao,
    private val songToPlaylistDao: SongToPlaylistDao,
    private val settingsDao: SettingsDao
) {

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