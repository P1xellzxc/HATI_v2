package com.hati.v2.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupDao {
    
    @Query("SELECT * FROM groups WHERE id = :groupId LIMIT 1")
    suspend fun getGroupById(groupId: String): GroupEntity?
    
    @Query("SELECT * FROM groups WHERE isDeleted = 0 ORDER BY updatedAt DESC")
    fun getAllGroups(): Flow<List<GroupEntity>>
    
    @Query("SELECT * FROM groups WHERE id IN (:groupIds)")
    suspend fun getGroupsByIds(groupIds: List<String>): List<GroupEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(group: GroupEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(groups: List<GroupEntity>)
    
    @Query("UPDATE groups SET isDeleted = 1 WHERE id = :groupId")
    suspend fun markAsDeleted(groupId: String)
    
    @Query("DELETE FROM groups WHERE isDeleted = 1")
    suspend fun deleteSoftDeletedGroups()
}
