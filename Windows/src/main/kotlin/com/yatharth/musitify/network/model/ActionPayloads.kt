package com.yatharth.musitify.network.model

import com.yatharth.musitify.data.entity.SyncAction
import kotlinx.serialization.Serializable

@Serializable
data class PushActionsPayload(
    val deviceId: String,
    val actions: List<SyncAction>
)

@Serializable
data class FetchActionsPayload(
    val actions: List<NAction>
)
