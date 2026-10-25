package com.yatharth.musitify.util

import androidx.room.TypeConverter
import java.util.UUID

class UUIDConverter {

    @TypeConverter
    fun fromUUID(uuid: UUID): String {
        return uuid.toString()
    }

    @TypeConverter
    fun uuidFromString(string: String): UUID? {
        return if (string.isNotEmpty()) UUID.fromString(string) else null
    }

}