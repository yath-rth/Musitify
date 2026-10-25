package com.yatharth.musitify.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.yatharth.musitify.data.entity.ListeningSession
import kotlinx.coroutines.flow.Flow

@Dao
interface ListeningSessionDao {

    @Insert
    suspend fun insert(session: ListeningSession): Long

    @Insert
    suspend fun insertAll(sessions: List<ListeningSession>)

    @Query("SELECT * FROM ListeningSessions ORDER BY startTimeMs ASC")
    fun getAll(): Flow<List<ListeningSession>>
}