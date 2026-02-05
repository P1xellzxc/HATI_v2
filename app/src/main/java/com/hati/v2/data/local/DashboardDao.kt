package com.hati.v2.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface DashboardDao {
    @Query("SELECT * FROM dashboards WHERE isDeleted = 0 ORDER BY createdAt DESC")
    fun getAllDashboards(): Flow<List<DashboardEntity>>

    @Query("SELECT * FROM dashboards WHERE id = :id")
    suspend fun getDashboardById(id: String): DashboardEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(dashboard: DashboardEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(dashboards: List<DashboardEntity>)

    @Query("DELETE FROM dashboards")
    suspend fun clearAll()

    @Query("SELECT * FROM dashboards WHERE isSynced = 0")
    suspend fun getUnsyncedDashboards(): List<DashboardEntity>

    @Query("UPDATE dashboards SET isSynced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: String)
}
