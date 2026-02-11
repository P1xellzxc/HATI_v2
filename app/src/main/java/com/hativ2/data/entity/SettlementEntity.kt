package com.hativ2.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "settlements",
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
            childColumns = ["fromId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = PersonEntity::class,
            parentColumns = ["id"],
            childColumns = ["toId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("dashboardId"),
        Index("fromId"),
        Index("toId")
    ]
)
data class SettlementEntity(
    @PrimaryKey val id: String,
    val dashboardId: String,
    val fromId: String,      // Person paying (settling the debt)
    val toId: String,        // Person receiving payment
    val amount: Double,
    val createdAt: Long
)
