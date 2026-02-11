package com.hativ2.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "dashboards")
data class DashboardEntity(
    @PrimaryKey val id: String,
    val title: String,
    val coverImageUrl: String?,
    // Storing coverImageSettings as JSON or separate fields if needed. For now, simple fields.
    val coverImageOffsetY: Int = 50,
    val coverImageZoom: Float = 1.0f,
    val currencySymbol: String,
    val themeColor: String,
    val dashboardType: String, // "travel", "household", "event", "other"
    val createdAt: Long,
    val order: Int
)
