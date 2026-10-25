package com.yatharth.musitify.network

import com.yatharth.musitify.data.repository.SettingsRepository
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import java.io.IOException
import java.util.UUID

/** Shared sync settings. Android hard-codes `http://localhost:8080`; desktop reads `server_url`. */
object SyncConfig {
    const val SERVER_URL_KEY = "server_url"
    const val DEFAULT_SERVER_URL = "http://localhost:8080"
    const val DEVICE_ID_KEY = "device_id"

    val JSON: MediaType = "application/json; charset=utf-8".toMediaType()

    /** Decoding tolerates extra keys (Android's strict default Json breaks on them). */
    val decodeJson: Json = Json { ignoreUnknownKeys = true }

    /** Encoding uses the default Json exactly like Android (`encodeDefaults = false`). */
    val encodeJson: Json = Json

    suspend fun baseUrl(settings: SettingsRepository): String =
        settings.getSettingByName(SERVER_URL_KEY)?.value?.trim()?.takeIf { it.isNotEmpty() }
            ?: DEFAULT_SERVER_URL

    /** `{base}/{path}`; throws [IOException] when the configured URL is not a valid http(s) URL. */
    suspend fun endpoint(settings: SettingsRepository, path: String): HttpUrl {
        val base = baseUrl(settings)
        return "${base.trimEnd('/')}/$path".toHttpUrlOrNull()
            ?: throw IOException("Invalid server URL: $base")
    }

    /** Same as Android: read `device_id`, or create and store one if missing. */
    suspend fun deviceId(settings: SettingsRepository): String =
        settings.getSettingByName(DEVICE_ID_KEY)?.value ?: run {
            val id = UUID.randomUUID().toString()
            settings.insertSetting(DEVICE_ID_KEY, id)
            id
        }
}
