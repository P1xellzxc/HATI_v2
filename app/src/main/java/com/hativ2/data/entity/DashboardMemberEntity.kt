package com.hativ2.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "dashboard_members",
    primaryKeys = ["dashboardId", "personId"],
    foreignKeys = [
        ForeignKey(
            entity = DashboardEntity::class,
            parentColumns = ["id"],
            childColumns = ["dashboardId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = PersonEntity::class,
            parentColumns = ["id"],
            childColumns = ["personId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("dashboardId"),
        Index("personId")
    ]
)
data class DashboardMemberEntity(
    val dashboardId: String,
    val personId: String,
    val joinedAt: Long
)
