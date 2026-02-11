package com.hativ2.domain.model

data class DashboardWithStats(
    val id: String,
    val title: String,
    val coverImageUrl: String?,
    val coverImageOffsetY: Int,
    val coverImageZoom: Float,
    val currencySymbol: String,
    val themeColor: String,
    val dashboardType: String,
    val createdAt: Long,
    val order: Int,
    // Computed stats
    val expenseCount: Int,
    val totalSpent: Double,
    val netBalance: Double // positive = owed to you, negative = you owe
)
