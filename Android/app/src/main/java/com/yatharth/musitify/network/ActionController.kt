package com.yatharth.musitify.network

import android.util.Log
import com.yatharth.musitify.data.repository.ActionRepository
import com.yatharth.musitify.data.repository.SettingsRepository
import com.yatharth.musitify.network.model.FetchActionsPayload
import com.yatharth.musitify.network.model.PushActionsPayload
import com.yatharth.musitify.service.RemoteChangesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActionController @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val actionRepository: ActionRepository,
    private val remoteChangesManager: RemoteChangesManager
) {
    private val TAG = "ActionController"
    private val client = OkHttpClient()
    private val URL = "http://localhost:8080/actions"
    val JSON: MediaType? = "application/json; charset=utf-8".toMediaTypeOrNull()

    suspend fun pushActions() {
        val device_id: String = settingsRepository.getSettingByName("device_id")?.value ?: run {
            val id = UUID.randomUUID().toString()
            settingsRepository.insertSetting("device_id", id)
            id
        }

        val unsyncedActions = actionRepository.getUnsyncedActions()

        val actionPayload = PushActionsPayload(
            deviceId = device_id,
            actions = unsyncedActions
        )

        val request = Request.Builder()
            .url(URL)
            .post(Json.encodeToString(actionPayload).toRequestBody(JSON))
            .build()

        try {
            withContext(Dispatchers.IO) {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IOException("Sync failed: ${response.code} ${response.body}")
                    }

                    val responseBody = response.body.string()
                    println("Server response: $responseBody")

                    actionRepository.markSynced(unsyncedActions.map { it.actionUuid })
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, e.message.toString())
        }
    }

    suspend fun pullActions() {
        val lastFetchedId = actionRepository.getLastCursor()
        val url = URL.toHttpUrl().newBuilder()
            .addQueryParameter("lastFetchedActionId", lastFetchedId.toString())
            .build()

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        try {
            withContext(Dispatchers.IO) {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IOException("Fetch failed ${response.body}")
                    }

                    val payload = Json.decodeFromString<FetchActionsPayload>(response.body.string())
                    Log.d(TAG, payload.toString())

                    val nActions = payload.actions
                    val syncActions = nActions.map { it.toSyncAction() }
//                    val newActions = actionRepository.insertRemoteActions(syncActions)
//                    Log.d(TAG, newActions.toString())

                    if (syncActions.isNotEmpty()) {
                        remoteChangesManager.apply(syncActions)
                    }
                    remoteChangesManager.generateMissingCovers()

                    val maxId = nActions.maxOfOrNull { it.localId }
                    if (maxId != null) {
//                        actionRepository.setCursor(maxId.toLong())
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, e.message.toString())
        }
    }

}