package com.hativ2.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.hativ2.data.entity.DashboardEntity
import com.hativ2.data.entity.DashboardMemberEntity
import com.hativ2.data.entity.PersonEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DashboardDao {
    @Query("SELECT * FROM dashboards ORDER BY `order` ASC")
    fun getAllDashboards(): Flow<List<DashboardEntity>>

    @Query("SELECT * FROM dashboards WHERE id = :id")
    suspend fun getDashboardById(id: String): DashboardEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDashboard(dashboard: DashboardEntity)

    @Update
    suspend fun updateDashboard(dashboard: DashboardEntity)

    @Query("DELETE FROM dashboards WHERE id = :id")
    suspend fun deleteDashboard(id: String)

    // Member relations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addMember(member: DashboardMemberEntity)

    @Query("DELETE FROM dashboard_members WHERE dashboardId = :dashboardId AND personId = :personId")
    suspend fun removeMember(dashboardId: String, personId: String)

    @Query("""
        SELECT P.* FROM people P
        INNER JOIN dashboard_members DM ON P.id = DM.personId
        WHERE DM.dashboardId = :dashboardId
    """)
    fun getDashboardMembers(dashboardId: String): Flow<List<PersonEntity>>
}
