package com.yatharth.musitify.network.model

import kotlinx.serialization.Serializable

@Serializable
data class SyncLibraryPayload(
    val device_id: String,
    val playlists: List<NPlaylist>,
    val songs: List<NSong>,
    val playlist_songs: List<MPlaylist_Song>,
)
