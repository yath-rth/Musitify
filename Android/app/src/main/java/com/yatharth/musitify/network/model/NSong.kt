package com.yatharth.musitify.network.model

import kotlinx.serialization.Serializable

@Serializable
data class NSong(
    val id: String,
    val title: String,
    val artist: String,
    val url: String,
    val downloaded_at: Long
)
