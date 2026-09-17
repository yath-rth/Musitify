package com.yatharth.musitify.network.model

import com.yatharth.musitify.data.entity.ActionType
import com.yatharth.musitify.data.entity.SyncAction
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NAction(
    @SerialName("localId") val localId: Int = 0,
    @SerialName("actionUuid") val uuid: String = "",
    @SerialName("type") val type: ActionType = ActionType.SONG_LIKED,
    @SerialName("songUuid") val songUuid: String? = null,
    @SerialName("playlistUuid") val playlistUuid: String? = null,
    @SerialName("payload") val payload: String? = null,
    @SerialName("createdAt") val createdAt: Long = System.currentTimeMillis()
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
