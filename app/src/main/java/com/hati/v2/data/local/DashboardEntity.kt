package com.hati.v2.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dashboards")
data class DashboardEntity(
    @PrimaryKey val id: String,
    val name: String,
    val ownerId: String, // User ID who owns this dashboard
    val currency: String = "PHP",
    val theme: String = "default", // For future custom themes
    val createdAt: Long,
    val updatedAt: Long,
    val isSynced: Boolean = false,
    val isDeleted: Boolean = false
)
