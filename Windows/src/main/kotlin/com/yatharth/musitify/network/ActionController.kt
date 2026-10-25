package com.yatharth.musitify.network

import com.yatharth.musitify.data.entity.SyncAction
import com.yatharth.musitify.data.repository.ActionRepository
import com.yatharth.musitify.data.repository.SettingsRepository
import com.yatharth.musitify.data.repository.SyncGateway
import com.yatharth.musitify.network.model.FetchActionsPayload
import com.yatharth.musitify.network.model.NAction
import com.yatharth.musitify.network.model.PushActionsPayload
import com.yatharth.musitify.service.RemoteChangesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

/**
 * Port of Android `network.ActionController` (action-log sync). Wire format is identical:
 *  - `POST {base}/actions` with `{"deviceId", "actions":[SyncAction...]}` (default Json, so
 *    defaults such as `isSynced=false` and null uuids are omitted, `payload` is a JSON string);
 *  - `GET {base}/actions?lastFetchedActionId=N` → `{"actions":[NAction...]}`.
 *
 * Desktop fixes:
 *  - base URL comes from setting `server_url` (default `http://localhost:8080`);
 *  - pushed actions are marked synced, only after a 2xx;
 *  - the pull cursor (max server `localId` received) is persisted in `sync_cursor` and sent;
 *  - echoes are skipped: actions whose `actionUuid` already exists locally, and actions that carry
 *    this device's id; new remote actions are stored (isSynced = true) so they are applied once;
 *  - responses are decoded with `ignoreUnknownKeys = true`;
 *  - a literal `"null"` song/playlist uuid is never sent (the field is omitted instead);
 *  - failures are thrown as [IOException] instead of only being logged.
 */
class ActionController(
    private val settingsRepository: SettingsRepository,
    private val actionRepository: ActionRepository,
    private val remoteChangesManager: RemoteChangesManager,
    private val client: OkHttpClient = OkHttpClient(),
) : SyncGateway {

    private val tag = "ActionController"

    override suspend fun pushActions(): Int {
        val deviceId = SyncConfig.deviceId(settingsRepository)
        val unsyncedActions = actionRepository.getUnsyncedActions()

        val actionPayload = PushActionsPayload(
            deviceId = deviceId,
            actions = unsyncedActions.map { it.withoutNullUuids() }
        )

        val request = Request.Builder()
            .url(SyncConfig.endpoint(settingsRepository, ACTIONS_PATH))
            .post(SyncConfig.encodeJson.encodeToString(actionPayload).toRequestBody(SyncConfig.JSON))
            .build()

        withContext(Dispatchers.IO) {
            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IOException("Sync failed: ${response.code} ${response.body.string()}")
                    }
                    println("$tag: Server response: ${response.body.string()}")
                }
            } catch (e: IOException) {
                throw if (e.message?.startsWith("Sync failed") == true) e
                else IOException("Sync failed: ${e.message ?: e.javaClass.simpleName}", e)
            }
        }

        actionRepository.markSynced(unsyncedActions.map { it.actionUuid })
        return unsyncedActions.size
    }

    override suspend fun pullActions(): Int {
        val lastFetchedId = actionRepository.getLastCursor()
        val url = SyncConfig.endpoint(settingsRepository, ACTIONS_PATH).newBuilder()
            .addQueryParameter("lastFetchedActionId", lastFetchedId.toString())
            .build()

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        val body = withContext(Dispatchers.IO) {
            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IOException("Fetch failed: ${response.code} ${response.body.string()}")
                    }
                    response.body.string()
                }
            } catch (e: IOException) {
                throw if (e.message?.startsWith("Fetch failed") == true) e
                else IOException("Fetch failed: ${e.message ?: e.javaClass.simpleName}", e)
            }
        }

        val payload = try {
            SyncConfig.decodeJson.decodeFromString<FetchActionsPayload>(body)
        } catch (e: SerializationException) {
            throw IOException("Fetch failed: bad response (${e.message})", e)
        } catch (e: IllegalArgumentException) {
            throw IOException("Fetch failed: bad response (${e.message})", e)
        }

        val nActions = payload.actions
        val deviceId = settingsRepository.getSettingByName(SyncConfig.DEVICE_ID_KEY)?.value

        val newActions = mutableListOf<SyncAction>()
        val seen = HashSet<String>()
        for (nAction in nActions) {
            if (deviceId != null && nAction.originDeviceId() == deviceId) continue
            val uuid = nAction.uuid
            if (uuid.isNotBlank()) {
                if (!seen.add(uuid) || actionRepository.hasAction(uuid)) continue
            }
            newActions += nAction.toSyncAction()
        }

        // Stored as synced so later pulls (or a lost cursor) never apply them twice.
        actionRepository.insertRemoteActions(newActions.filter { it.actionUuid.isNotBlank() })
        if (newActions.isNotEmpty()) {
            remoteChangesManager.apply(newActions)
        }
        remoteChangesManager.generateMissingCovers()

        val maxId = nActions.maxOfOrNull { it.localId }
        if (maxId != null && maxId.toLong() > lastFetchedId) {
            actionRepository.setCursor(maxId.toLong())
        }
        return newActions.size
    }

    private fun SyncAction.withoutNullUuids(): SyncAction = copy(
        songUuid = songUuid.takeUnless { it == "null" },
        playlistUuid = playlistUuid.takeUnless { it == "null" }
    )

    /** The device the action came from: the action field, else a `deviceId`/`device_id` payload key. */
    private fun NAction.originDeviceId(): String? = deviceId ?: payload?.let { raw ->
        runCatching {
            val obj = Json.parseToJsonElement(raw).jsonObject
            (obj["deviceId"] ?: obj["device_id"])?.jsonPrimitive?.contentOrNull
        }.getOrNull()
    }

    private companion object {
        const val ACTIONS_PATH = "actions"
    }
}
