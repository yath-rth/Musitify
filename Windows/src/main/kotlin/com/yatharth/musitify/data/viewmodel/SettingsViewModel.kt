package com.yatharth.musitify.data.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yatharth.musitify.data.repository.SettingsRepository
import com.yatharth.musitify.platform.AppPaths
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Desktop-only settings: sync server URL (Settings row `server_url`), music folder
 * ([AppPaths.musicDir], stored in `config.properties`) and the read-only device id
 * (Settings row `device_id`).
 */
class SettingsViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _serverUrl = MutableStateFlow(DEFAULT_SERVER_URL)
    val serverUrl: StateFlow<String> = _serverUrl.asStateFlow()

    private val _musicDir = MutableStateFlow("")
    val musicDir: StateFlow<String> = _musicDir.asStateFlow()

    private val _deviceId = MutableStateFlow<String?>(null)
    val deviceId: StateFlow<String?> = _deviceId.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.getSettingByName(KEY_SERVER_URL)?.value
                ?.takeIf { it.isNotBlank() }
                ?.let { _serverUrl.value = it }
            _deviceId.value = settingsRepository.getSettingByName(KEY_DEVICE_ID)?.value
            _musicDir.value = withContext(Dispatchers.IO) { AppPaths.musicDir.absolutePath }
        }
    }

    /** Persists [url] (trimmed, trailing `/` removed); blank resets to [DEFAULT_SERVER_URL]. */
    fun setServerUrl(url: String) {
        val normalized = url.trim().trimEnd('/').ifBlank { DEFAULT_SERVER_URL }
        _serverUrl.value = normalized
        viewModelScope.launch { settingsRepository.insertSetting(KEY_SERVER_URL, normalized) }
    }

    /** New downloads go to [dir]; existing songs keep their stored paths. `null` resets to the default. */
    fun setMusicDir(dir: File?) {
        viewModelScope.launch {
            _musicDir.value = withContext(Dispatchers.IO) {
                AppPaths.setMusicDir(dir)
                AppPaths.musicDir.absolutePath
            }
        }
    }

    companion object {
        const val KEY_SERVER_URL = "server_url"
        const val KEY_DEVICE_ID = "device_id"
        const val DEFAULT_SERVER_URL = "http://localhost:8080"
    }
}
