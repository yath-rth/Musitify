package com.yatharth.musitify.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.Companion.IGNORE
import androidx.room.Query
import com.yatharth.musitify.data.entity.SyncAction

@Dao
interface SyncActionDao {

    @Query("SELECT * FROM Actions WHERE isSynced = 0 ORDER BY localId ASC")
    suspend fun getUnsynced(): List<SyncAction>

    @Query("SELECT * FROM Actions ORDER BY localId ASC")
    suspend fun getAll(): List<SyncAction>

    @Query("SELECT * FROM Actions WHERE actionUuid = :actionUuid")
    suspend fun getByActionUuid(actionUuid: String): SyncAction?

    @Query("SELECT * FROM Actions WHERE actionUuid in (:actionUuids)")
    suspend fun getByActionUuids(actionUuids: List<String>): List<SyncAction?>

    @Insert(onConflict = IGNORE)
    suspend fun insert(action: SyncAction): Long

    @Insert(onConflict = IGNORE)
    suspend fun insertAll(actions: List<SyncAction>)

    @Query("UPDATE Actions SET isSynced = 1 WHERE actionUuid IN (:actionUuids)")
    suspend fun markSynced(actionUuids: List<String>)

    @Query("DELETE FROM Actions WHERE isSynced = 1")
    suspend fun pruneSynced()
}
