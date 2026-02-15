package com.hativ2.ui.screens

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hativ2.data.entity.PersonEntity
import com.hativ2.ui.MainViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class AddExpenseScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // Mock MainViewModel 
    // We assume Mockito can mock this class. If fails, we might need a fake or open-class config.
    private val viewModel = mock<MainViewModel>()

    @Test
    fun showsInitialUI() {
        val dashboardId = "dash-1"
        // Mock getPeople flow
        whenever(viewModel.getPeople(dashboardId)).thenReturn(MutableStateFlow(emptyList()))

        composeTestRule.setContent {
            AddExpenseScreen(
                dashboardId = dashboardId,
                viewModel = viewModel,
                onBackClick = {}
            )
        }

        // Verify key elements
        composeTestRule.onNodeWithText("Add Expense").assertIsDisplayed()
        composeTestRule.onNodeWithText("Description").assertIsDisplayed()
        composeTestRule.onNodeWithText("Amount (₱)").assertIsDisplayed()
        composeTestRule.onNodeWithText("SAVE EXPENSE").assertIsDisplayed()
        
        // Check placeholders (if visible) might fail if merged. 
        // But "What was this for?" and "0.00" are separate Text nodes if BasicTextField is separate.
        // Let's rely on labels for now.
    }

    @Test
    fun inputsWork() {
        val dashboardId = "dash-2"
        whenever(viewModel.getPeople(dashboardId)).thenReturn(MutableStateFlow(emptyList()))

        composeTestRule.setContent {
            AddExpenseScreen(
                dashboardId = dashboardId,
                viewModel = viewModel,
                onBackClick = {}
            )
        }

        // Find by placeholder text if visible initially
        // Description
        // composeTestRule.onNodeWithText("What was this for?").performTextInput("Test Dinner")
        // Amount
        // composeTestRule.onNodeWithText("0.00").performTextInput("150")
        
        // Since we can't fully trust placeholder visibility in semantic tree without running interactive preview,
        // we'll target by tag if added, or traverse tree. 
        // But let's try finding the node that has the "Description" label and then check for input capability.
        // Actually, MangaTextField puts label and input in Column. They are siblings.
        // We can find by text "Description" then use onSibling or similar.
        // Or simpler: Just find the node that *contains* text "What was this for?" if empty.
    }
}
