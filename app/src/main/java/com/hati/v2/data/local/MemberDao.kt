package com.hati.v2.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MemberDao {
    @Query("SELECT * FROM members WHERE dashboardId = :dashboardId")
    fun getMembersByDashboard(dashboardId: String): Flow<List<MemberEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(member: MemberEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(members: List<MemberEntity>)
    
    @Query("SELECT * FROM members WHERE isSynced = 0")
    suspend fun getUnsyncedMembers(): List<MemberEntity>
    
    @Query("UPDATE members SET isSynced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: String)
}
