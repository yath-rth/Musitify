package com.yatharth.musitify.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import java.util.UUID

enum class ActionType {
    SONG_LIKED,
    SONG_UNLIKED,
    PLAYLIST_CREATED,
    PLAYLIST_DELETED,
    SONG_ADDED_TO_PLAYLIST,
    SONG_REMOVED_FROM_PLAYLIST
}

@Entity(
    tableName = "Actions",
    indices = [
        Index(value = ["actionUuid"], unique = true),
        Index(value = ["isSynced"])
    ]
)
@Serializable
data class SyncAction(
    @PrimaryKey(autoGenerate = true) val localId: Int = 0,
    val actionUuid: String = UUID.randomUUID().toString(),
    val type: ActionType,
    val songUuid: String? = null,
    val playlistUuid: String? = null,
    val payload: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)
