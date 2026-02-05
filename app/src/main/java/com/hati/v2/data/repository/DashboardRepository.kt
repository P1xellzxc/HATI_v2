package com.hati.v2.data.repository

import com.hati.v2.data.local.DashboardDao
import com.hati.v2.data.local.DashboardEntity
import com.hati.v2.data.local.MemberDao
import com.hati.v2.data.local.MemberEntity
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DashboardRepository @Inject constructor(
    private val dashboardDao: DashboardDao,
    private val memberDao: MemberDao,
    private val supabaseClient: SupabaseClient
) {

    fun getAllDashboards(): Flow<List<DashboardEntity>> = dashboardDao.getAllDashboards()

    suspend fun getDashboardById(id: String): DashboardEntity? {
        return dashboardDao.getDashboardById(id)
    }

    suspend fun createDashboard(name: String, ownerId: String) {
        val id = java.util.UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        
        val dashboard = DashboardEntity(
            id = id,
            name = name,
            ownerId = ownerId,
            createdAt = now,
            updatedAt = now,
            isSynced = false
        )
        
        // Add owner as a member
        val member = MemberEntity(
            id = java.util.UUID.randomUUID().toString(),
            dashboardId = id,
            userId = ownerId,
            name = "Me", // TODO: Get actual user name
            role = "owner",
            joinedAt = now,
            isSynced = false
        )

        dashboardDao.insert(dashboard)
        memberDao.insert(member)
        
        // TODO: Try to sync immediately
    }

    fun getMembers(dashboardId: String): Flow<List<MemberEntity>> {
        return memberDao.getMembersByDashboard(dashboardId)
    }
}
