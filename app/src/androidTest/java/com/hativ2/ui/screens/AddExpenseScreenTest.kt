package com.hativ2.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hativ2.ui.MainViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AddExpenseScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var viewModel: MainViewModel
    private lateinit var dashboardId: String

    @Before
    fun setup() {
        // Use the real application context to create the ViewModel
        val application = ApplicationProvider.getApplicationContext<android.app.Application>()
        
        // Note: In a real architecture we would use Dependency Injection to swap this with a TestViewModel.
        // Since we are coupled to MainViewModel, we use the real one and set up a test environment (Dashboard).
        viewModel = MainViewModel(application)

        // Create a temporary dashboard for testing
        runBlocking {
            viewModel.createDashboard("Test Dashboard", "personal", "#FF0000")
            // Wait for it to be created and get ID - naive way involves querying
            // For now, let's assume we can query list
            kotlinx.coroutines.delay(500) // Basic wait for DB
            val dashboards = viewModel.dashboards.first()
            dashboardId = dashboards.last().id
        }
    }

    @Test
    fun verifyAddExpenseUI_InitialState() {
        composeTestRule.setContent {
            AddExpenseScreen(
                dashboardId = dashboardId,
                viewModel = viewModel,
                onBackClick = {}
            )
        }

        // Verify Title
        composeTestRule.onNodeWithText("Add Expense").assertIsDisplayed()

        // Verify Sections
        composeTestRule.onNodeWithText("Description").assertIsDisplayed()
        composeTestRule.onNodeWithText("Amount (₱)").assertIsDisplayed()
        composeTestRule.onNodeWithText("Split With").assertIsDisplayed()

        // Verify Save Button is initially disabled or enabled? 
        // Logic: Checks desc isNotBlank, amount > 0, splitWith not empty.
        // Initially empty desc/amount -> logic inside clickable checks validation but button is always clickable visually (just shows snackbar).
        // Wait, the button implementation in AddExpenseScreen:
        // .clickable { ... validation ... }
        // It doesn't disable the button. It shows a Snackbar.
        
        composeTestRule.onNodeWithText("SAVE EXPENSE").assertIsDisplayed()
    }

    @Test
    fun verifyAddExpense_InputInteraction() {
        composeTestRule.setContent {
            AddExpenseScreen(
                dashboardId = dashboardId,
                viewModel = viewModel,
                onBackClick = {}
            )
        }

        // Input Description
        composeTestRule.onNodeWithText("What was this for?") // Placeholder
            .performTextInput("Test Lunch")
        
        // Input Amount
        composeTestRule.onNodeWithText("0.00") // Placeholder
            .performTextInput("150.00")

        // Verify values are set (by checking text existence)
        composeTestRule.onNodeWithText("Test Lunch").assertIsDisplayed()
        composeTestRule.onNodeWithText("150.00").assertIsDisplayed()
    }

    @Test
    fun verifySplitType_Selection() {
         composeTestRule.setContent {
            AddExpenseScreen(
                dashboardId = dashboardId,
                viewModel = viewModel,
                onBackClick = {}
            )
        }

        // Default is Equal
        // Click Percentage
        composeTestRule.onNodeWithText("📊 Percentage").performClick()
        
        // Verify Percentage input appears for "You" (Default member)
        // "You" should be in the list
        composeTestRule.onNodeWithText("You").assertIsDisplayed()
        
        // Percentage input has "%" suffix
        composeTestRule.onNodeWithText("%").assertIsDisplayed()

        // Click Exact
        composeTestRule.onNodeWithText("💵 Exact").performClick()
        composeTestRule.onNodeWithText("₱").assertIsDisplayed()
    }
}
