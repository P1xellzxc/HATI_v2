package com.hativ2.domain.usecase

import com.hativ2.data.entity.DashboardEntity
import com.hativ2.data.entity.ExpenseEntity
import com.hativ2.data.entity.PersonEntity
import com.hativ2.data.entity.SettlementEntity
import com.hativ2.data.entity.SplitEntity
import com.hativ2.domain.model.DashboardWithStats
import javax.inject.Inject

class CalculateDashboardStatsUseCase @Inject constructor(
    private val calculateDebtsUseCase: CalculateDebtsUseCase
) {
    fun execute(
        dashboard: DashboardEntity,
        expenses: List<ExpenseEntity>,
        splits: List<SplitEntity>,
        settlements: List<SettlementEntity>,
        members: List<PersonEntity>,
        currentUserId: String
    ): DashboardWithStats {
        val debtSummary = calculateDebtsUseCase.execute(
            expenses = expenses,
            splits = splits,
            settlements = settlements,
            userIds = members.map { it.id },
            currentUserId = currentUserId
        )

        return DashboardWithStats(
            id = dashboard.id,
            title = dashboard.title,
            coverImageUrl = dashboard.coverImageUrl,
            coverImageOffsetY = dashboard.coverImageOffsetY,
            coverImageZoom = dashboard.coverImageZoom,
            currencySymbol = dashboard.currencySymbol,
            themeColor = dashboard.themeColor,
            dashboardType = dashboard.dashboardType,
            createdAt = dashboard.createdAt,
            order = dashboard.order,
            expenseCount = expenses.size,
            totalSpent = expenses.sumOf { it.amount },
            netBalance = debtSummary.totalOwedToYou - debtSummary.totalYouOwe
        )
    }
}
