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

    private val baseDashboard = DashboardEntity(
        id = "dash-1",
        title = "Trip",
        coverImageUrl = null,
        coverImageOffsetY = 0,
        coverImageZoom = 1f,
        currencySymbol = "$",
        themeColor = "#000000",
        dashboardType = "personal",
        createdAt = 1000L,
        order = 0
    )

    private val zeroDebtSummary = DebtSummaryModel(
        transactions = emptyList(),
        balances = emptyMap(),
        totalOwedToYou = 0.0,
        totalYouOwe = 0.0,
        memberShares = emptyMap(),
        owedToYou = emptyList(),
        youOwe = emptyList()
    )

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        calculateDashboardStatsUseCase = CalculateDashboardStatsUseCase(calculateDebtsUseCase)
    }

    @Test
    fun `execute calculates correct total spent and net balance`() = runTest {
        val expenses = listOf(
            ExpenseEntity("e1", "dash-1", "Food", 100.0, "u1", "Food", 1000),
            ExpenseEntity("e2", "dash-1", "Transport", 50.0, "u2", "Transport", 2000)
        )

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

        val result = calculateDashboardStatsUseCase.execute(
            dashboard = baseDashboard,
            expenses = expenses,
            splits = emptyList(),
            settlements = emptyList(),
            members = emptyList(),
            currentUserId = "u1"
        )

        assertEquals("dash-1", result.id)
        assertEquals("Trip", result.title)
        assertEquals(2, result.expenseCount)
        assertEquals(150.0, result.totalSpent, 0.0)
        assertEquals(50.0, result.netBalance, 0.0)
    }

    @Test
    fun `execute with empty expenses returns zero stats`() = runTest {
        `when`(calculateDebtsUseCase.execute(any(), any(), any(), any(), any(), any())).thenReturn(zeroDebtSummary)

        val result = calculateDashboardStatsUseCase.execute(
            dashboard = baseDashboard,
            expenses = emptyList(),
            splits = emptyList(),
            settlements = emptyList(),
            members = emptyList(),
            currentUserId = "u1"
        )

        assertEquals(0, result.expenseCount)
        assertEquals(0.0, result.totalSpent, 0.0)
        assertEquals(0.0, result.netBalance, 0.0)
    }

    @Test
    fun `execute maps all dashboard fields correctly`() = runTest {
        val dashboard = DashboardEntity(
            id = "dash-42",
            title = "Vacation",
            coverImageUrl = "https://example.com/img.png",
            coverImageOffsetY = 25,
            coverImageZoom = 1.5f,
            currencySymbol = "€",
            themeColor = "#FF5733",
            dashboardType = "travel",
            createdAt = 5000L,
            order = 3
        )

        `when`(calculateDebtsUseCase.execute(any(), any(), any(), any(), any(), any())).thenReturn(zeroDebtSummary)

        val result = calculateDashboardStatsUseCase.execute(
            dashboard = dashboard,
            expenses = emptyList(),
            splits = emptyList(),
            settlements = emptyList(),
            members = emptyList(),
            currentUserId = "u1"
        )

        assertEquals("dash-42", result.id)
        assertEquals("Vacation", result.title)
        assertEquals("https://example.com/img.png", result.coverImageUrl)
        assertEquals(25, result.coverImageOffsetY)
        assertEquals(1.5f, result.coverImageZoom)
        assertEquals("€", result.currencySymbol)
        assertEquals("#FF5733", result.themeColor)
        assertEquals("travel", result.dashboardType)
        assertEquals(5000L, result.createdAt)
        assertEquals(3, result.order)
    }

    @Test
    fun `execute with negative net balance when you owe more`() = runTest {
        val expenses = listOf(
            ExpenseEntity("e1", "dash-1", "Food", 100.0, "u2", "Food", 1000)
        )

        val debtSummary = DebtSummaryModel(
            transactions = emptyList(),
            balances = emptyMap(),
            totalOwedToYou = 10.0,
            totalYouOwe = 90.0,
            memberShares = emptyMap(),
            owedToYou = emptyList(),
            youOwe = emptyList()
        )

        `when`(calculateDebtsUseCase.execute(any(), any(), any(), any(), any(), any())).thenReturn(debtSummary)

        val result = calculateDashboardStatsUseCase.execute(
            dashboard = baseDashboard,
            expenses = expenses,
            splits = emptyList(),
            settlements = emptyList(),
            members = emptyList(),
            currentUserId = "u1"
        )

        assertEquals(-80.0, result.netBalance, 0.0) // 10 - 90
    }

    @Test
    fun `execute with single expense calculates correct stats`() = runTest {
        val expenses = listOf(
            ExpenseEntity("e1", "dash-1", "Taxi", 42.5, "u1", "Transport", 1000)
        )

        `when`(calculateDebtsUseCase.execute(any(), any(), any(), any(), any(), any())).thenReturn(zeroDebtSummary)

        val result = calculateDashboardStatsUseCase.execute(
            dashboard = baseDashboard,
            expenses = expenses,
            splits = emptyList(),
            settlements = emptyList(),
            members = emptyList(),
            currentUserId = "u1"
        )

        assertEquals(1, result.expenseCount)
        assertEquals(42.5, result.totalSpent, 0.0)
    }
}
