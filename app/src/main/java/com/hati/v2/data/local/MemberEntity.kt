package com.hati.v2.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "members",
    foreignKeys = [
        ForeignKey(
            entity = DashboardEntity::class,
            parentColumns = ["id"],
            childColumns = ["dashboardId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("dashboardId")]
)
data class MemberEntity(
    @PrimaryKey val id: String,
    val dashboardId: String,
    val userId: String?, // Nullable if invited via email/link but not yet a registered user
    val name: String, // Display name in this dashboard
    val role: String = "member", // owner, admin, member
    val joinedAt: Long,
    val isSynced: Boolean = false
)
