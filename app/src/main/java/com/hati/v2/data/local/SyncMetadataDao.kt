package com.hati.v2.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SyncMetadataDao {
    
    @Query("SELECT lastSyncTime FROM sync_metadata WHERE groupId = :groupId LIMIT 1")
    suspend fun getLastSyncTime(groupId: String): Long?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(metadata: SyncMetadataEntity)
    
    @Query("UPDATE sync_metadata SET lastSyncTime = :timestamp, lastSyncStatus = 'success' WHERE groupId = :groupId")
    suspend fun updateLastSyncTime(groupId: String, timestamp: Long)
    
    @Query("UPDATE sync_metadata SET lastSyncStatus = :status WHERE groupId = :groupId")
    suspend fun updateSyncStatus(groupId: String, status: String)
    
    @Query("UPDATE sync_metadata SET pendingChanges = :count WHERE groupId = :groupId")
    suspend fun updatePendingChanges(groupId: String, count: Int)
}
