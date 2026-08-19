package com.yatharth.musitify.data.repository

import android.R.attr.name
import android.content.Context
import com.yatharth.musitify.data.dao.PlaylistDao
import com.yatharth.musitify.data.dao.SettingsDao
import com.yatharth.musitify.data.dao.SongDao
import com.yatharth.musitify.data.entity.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsDao: SettingsDao
) {

    suspend fun insertSetting(name: String, value: String): Long {
        return settingsDao.updateSetting(
            Settings(
                name = name,
                value = value,
                updated_on = System.currentTimeMillis()
            )
        )
    }

    suspend fun getSettingByName(name: String): Settings? {
        return settingsDao.getSettingByName(name)
    }

    suspend fun getSettingById(id: Int): Settings? {
        return settingsDao.getSettingsById(id)
    }

    suspend fun hasUserOpenedApp(): Boolean {
        return settingsDao.getSettingByName("hasOpened")?.value.toBoolean()
    }

    suspend fun openedApp(){
        settingsDao.updateSetting(
            Settings(
                name = "hasOpened",
                value = "true",
                updated_on = System.currentTimeMillis()
            )
        )
    }

}
