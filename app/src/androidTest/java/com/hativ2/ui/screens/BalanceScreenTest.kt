package com.hativ2.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hativ2.data.entity.DashboardEntity
import com.hativ2.data.entity.ExpenseEntity
import com.hativ2.ui.MainViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class BalanceScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val viewModel = mock<MainViewModel>()
    private val dashId = "dash-1"

    // -------- Screen headline --------

    @Test
    fun balanceScreen_showsArcStatisticsTitle() {
        setupEmptyState()

        composeTestRule.setContent {
            BalanceScreen(
                dashboardId = dashId,
                viewModel = viewModel,
                onBackClick = {}
            )
        }

        composeTestRule.onNodeWithText("ARC STATISTICS").assertIsDisplayed()
    }

    @Test
    fun balanceScreen_showsTotalArcSpending() {
        setupEmptyState()

        composeTestRule.setContent {
            BalanceScreen(
                dashboardId = dashId,
                viewModel = viewModel,
                onBackClick = {}
            )
        }

        composeTestRule.onNodeWithText("Total Arc Spending").assertIsDisplayed()
    }

    // -------- Empty state --------

    @Test
    fun withNoExpenses_showsNoChaptersEmptyState() {
        setupEmptyState()

        composeTestRule.setContent {
            BalanceScreen(
                dashboardId = dashId,
                viewModel = viewModel,
                onBackClick = {}
            )
        }

        composeTestRule.onNodeWithText("No chapters to display").assertIsDisplayed()
    }

    // -------- With expenses --------

    @Test
    fun withExpenses_dashboardTitleAppearsInScreen() {
        setupWithExpenses()

        composeTestRule.setContent {
            BalanceScreen(
                dashboardId = dashId,
                viewModel = viewModel,
                onBackClick = {}
            )
        }

        // Dashboard title is uppercased in the subtitle area
        composeTestRule.onNodeWithText("FAMILY TRIP", ignoreCase = true).assertIsDisplayed()
    }

    @Test
    fun withExpenses_lastSixMonthsLabelIsDisplayed() {
        setupWithExpenses()

        composeTestRule.setContent {
            BalanceScreen(
                dashboardId = dashId,
                viewModel = viewModel,
                onBackClick = {}
            )
        }

        composeTestRule.onNodeWithText("Last 6 Months").assertIsDisplayed()
    }

    // -------- Helpers --------

    private fun setupEmptyState() {
        whenever(viewModel.getExpenses(dashId)).thenReturn(MutableStateFlow(emptyList()))
        whenever(viewModel.dashboards).thenReturn(MutableStateFlow(emptyList()))
    }

    private fun setupWithExpenses() {
        val dashboard = DashboardEntity(
            id = dashId, title = "Family Trip", coverImageUrl = null,
            currencySymbol = "$", themeColor = "#000", dashboardType = "other",
            createdAt = 0L, order = 0
        )
        val expenses = listOf(
            ExpenseEntity(
                id = "e1", dashboardId = dashId, description = "Dinner",
                amount = 120.0, paidBy = "p1", category = "Food",
                createdAt = System.currentTimeMillis()
            )
        )
        whenever(viewModel.getExpenses(dashId)).thenReturn(MutableStateFlow(expenses))
        whenever(viewModel.dashboards).thenReturn(MutableStateFlow(listOf(dashboard)))
    }
}
