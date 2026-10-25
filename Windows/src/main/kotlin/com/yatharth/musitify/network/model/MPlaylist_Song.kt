package com.yatharth.musitify.network.model

import kotlinx.serialization.Serializable

@Suppress("ClassName")
@Serializable
data class MPlaylist_Song(
    val song_id: String,
    val playlist_id: String,
    val position: Int,
    val added_on: Long
)
