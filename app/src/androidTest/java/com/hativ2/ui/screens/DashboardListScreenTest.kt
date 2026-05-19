package com.hativ2.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hativ2.domain.model.DashboardWithStats
import com.hativ2.ui.MainViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class DashboardListScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val viewModel = mock<MainViewModel>()

    // -------- Empty state --------

    @Test
    fun emptyState_showsHeadlineAndNewVolumePrompt() {
        whenever(viewModel.dashboardsWithStats).thenReturn(MutableStateFlow(emptyList()))

        composeTestRule.setContent {
            DashboardListScreen(
                viewModel = viewModel,
                onDashboardClick = {},
                onOpenSettings = {}
            )
        }

        // Hub headline always visible
        composeTestRule.onNodeWithText("YOUR STORY VOLUMES").assertIsDisplayed()
        // Empty state copy
        composeTestRule.onNodeWithText("No volumes yet").assertIsDisplayed()
        composeTestRule.onNodeWithText("Tap + NEW VOLUME to start your story.").assertIsDisplayed()
    }

    // -------- List state --------

    @Test
    fun withDashboards_eachDashboardTitleIsDisplayed() {
        val dashboards = listOf(
            makeDashboard("d1", "Family Trip"),
            makeDashboard("d2", "Household Bills")
        )
        whenever(viewModel.dashboardsWithStats).thenReturn(MutableStateFlow(dashboards))

        composeTestRule.setContent {
            DashboardListScreen(
                viewModel = viewModel,
                onDashboardClick = {},
                onOpenSettings = {}
            )
        }

        composeTestRule.onNodeWithText("Family Trip").assertIsDisplayed()
        composeTestRule.onNodeWithText("Household Bills").assertIsDisplayed()
    }

    @Test
    fun dashboardCard_showsExpenseCountAndTotal() {
        val dashboards = listOf(
            makeDashboard("d1", "Vacation", expenseCount = 5, totalSpent = 250.0)
        )
        whenever(viewModel.dashboardsWithStats).thenReturn(MutableStateFlow(dashboards))

        composeTestRule.setContent {
            DashboardListScreen(
                viewModel = viewModel,
                onDashboardClick = {},
                onOpenSettings = {}
            )
        }

        // expense count chip label
        composeTestRule.onNodeWithText("5 ch.").assertIsDisplayed()
        // total spend label
        composeTestRule.onNodeWithText("Total").assertIsDisplayed()
    }

    // -------- Navigation --------

    @Test
    fun clickingDashboardCard_invokesOnDashboardClickCallback() {
        val dashboards = listOf(makeDashboard("d1", "Trip to Bali"))
        whenever(viewModel.dashboardsWithStats).thenReturn(MutableStateFlow(dashboards))

        var clickedId: String? = null
        composeTestRule.setContent {
            DashboardListScreen(
                viewModel = viewModel,
                onDashboardClick = { clickedId = it },
                onOpenSettings = {}
            )
        }

        composeTestRule.onNodeWithText("Trip to Bali").performClick()

        assert(clickedId == "d1") { "Expected d1 to be clicked, got $clickedId" }
    }

    // -------- Add dialog --------

    @Test
    fun clickingNewVolumeButton_opensAddDashboardDialog() {
        whenever(viewModel.dashboardsWithStats).thenReturn(MutableStateFlow(emptyList()))

        composeTestRule.setContent {
            DashboardListScreen(
                viewModel = viewModel,
                onDashboardClick = {},
                onOpenSettings = {}
            )
        }

        composeTestRule.onNodeWithText("NEW VOLUME", substring = true).performClick()

        // Dialog should appear with its title
        composeTestRule.onNodeWithText("New Volume", ignoreCase = true).assertIsDisplayed()
    }

    // -------- Helpers --------

    private fun makeDashboard(
        id: String,
        title: String,
        expenseCount: Int = 0,
        totalSpent: Double = 0.0
    ) = DashboardWithStats(
        id = id,
        title = title,
        coverImageUrl = null,
        coverImageOffsetY = 50,
        coverImageZoom = 1.0f,
        currencySymbol = "$",
        themeColor = "#FF9900",
        dashboardType = "other",
        createdAt = 0L,
        order = 0,
        expenseCount = expenseCount,
        totalSpent = totalSpent,
        netBalance = 0.0
    )
}
