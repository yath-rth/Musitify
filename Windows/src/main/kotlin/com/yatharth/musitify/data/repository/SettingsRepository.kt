package com.yatharth.musitify.data.repository

import com.yatharth.musitify.data.dao.SettingsDao
import com.yatharth.musitify.data.entity.Settings

/** 1:1 port of Android `SettingsRepository` (the unused Context dependency is dropped). */
class SettingsRepository(
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

    suspend fun openedApp() {
        settingsDao.updateSetting(
            Settings(
                name = "hasOpened",
                value = "true",
                updated_on = System.currentTimeMillis()
            )
        )
    }
}
