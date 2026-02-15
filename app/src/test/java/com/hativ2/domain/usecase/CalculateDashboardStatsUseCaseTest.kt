package com.hativ2.domain.usecase

import com.hativ2.data.entity.DashboardEntity
import com.hativ2.data.entity.ExpenseEntity
import com.hativ2.domain.model.DebtSummaryModel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any

class CalculateDashboardStatsUseCaseTest {

    @Mock
    private lateinit var calculateDebtsUseCase: CalculateDebtsUseCase

    private lateinit var calculateDashboardStatsUseCase: CalculateDashboardStatsUseCase

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        calculateDashboardStatsUseCase = CalculateDashboardStatsUseCase(calculateDebtsUseCase)
    }

    @Test
    fun `execute calculates correct total spent and net balance`() = runTest {
        // Given
        val dashboard = DashboardEntity(
            id = "dash-1",
            title = "Trip",
            coverImageUrl = null,
            coverImageOffsetY = 0,
            coverImageZoom = 1f,
            currencySymbol = "$",
            themeColor = "#000000",
            dashboardType = "personal",
            createdAt = System.currentTimeMillis(),
            order = 0
        )

        val expenses = listOf(
            ExpenseEntity("e1", "dash-1", "Food", 100.0, "u1", "Food", 1000),
            ExpenseEntity("e2", "dash-1", "Transport", 50.0, "u2", "Transport", 2000)
        )

        // Mocking the result from CalculateDebtsUseCase
        val debtSummary = DebtSummaryModel(
            transactions = emptyList(),
            balances = emptyMap(),
            totalOwedToYou = 75.0,
            totalYouOwe = 25.0,
            memberShares = emptyMap(),
            owedToYou = emptyList(),
            youOwe = emptyList()
        )

        `when`(calculateDebtsUseCase.execute(any(), any(), any(), any(), any(), any())).thenReturn(debtSummary)

        // When
        val result = calculateDashboardStatsUseCase.execute(
            dashboard = dashboard,
            expenses = expenses,
            splits = emptyList(),
            settlements = emptyList(),
            members = emptyList(),
            currentUserId = "u1"
        )

        // Then
        assertEquals("dash-1", result.id)
        assertEquals("Trip", result.title)
        assertEquals(2, result.expenseCount)
        assertEquals(150.0, result.totalSpent, 0.0) // 100 + 50
        assertEquals(50.0, result.netBalance, 0.0) // 75 - 25
    }
}
