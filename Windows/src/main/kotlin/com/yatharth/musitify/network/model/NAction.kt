package com.yatharth.musitify.network.model

import com.yatharth.musitify.data.entity.ActionType
import com.yatharth.musitify.data.entity.SyncAction
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

/**
 * Port of Android `NAction` (a pulled action; [localId] is the **server** sequence id).
 *
 * Desktop addition: optional [deviceId] (decode-only, accepts `deviceId` or `device_id`) so the
 * client can drop actions that originated on this device when the server includes it. Android's
 * strict Json would fail on that key; desktop decodes with `ignoreUnknownKeys = true`.
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class NAction(
    @SerialName("localId") val localId: Int = 0,
    @SerialName("actionUuid") val uuid: String = "",
    @SerialName("type") val type: ActionType = ActionType.SONG_LIKED,
    @SerialName("songUuid") val songUuid: String? = null,
    @SerialName("playlistUuid") val playlistUuid: String? = null,
    @SerialName("payload") val payload: String? = null,
    @SerialName("createdAt") val createdAt: Long = System.currentTimeMillis(),
    @SerialName("deviceId") @JsonNames("device_id") val deviceId: String? = null,
) {
    fun toSyncAction(): SyncAction = SyncAction(
        actionUuid = uuid,
        type = type,
        songUuid = songUuid,
        playlistUuid = playlistUuid,
        payload = payload,
        createdAt = createdAt,
        isSynced = true
    )
}
