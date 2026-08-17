package com.yatharth.musitify.network.model

import androidx.media3.extractor.mp4.Track
import kotlinx.serialization.Serializable

@Serializable
data class NPlaylist(
    val id : String,
    val name: String,
    val created_on: Long
)
