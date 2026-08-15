package com.yatharth.musitify.network.model

import kotlinx.serialization.Serializable

@Serializable
data class MUser_Song(
    val song_id: String,
    val user_id: String,
    val liked_on: Long
)
