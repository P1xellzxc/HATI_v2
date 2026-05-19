package com.hativ2.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
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

    private val viewModel = mock<MainViewModel>()

    @Test
    fun showsInitialUI() {
        val dashboardId = "dash-1"
        whenever(viewModel.getPeople(dashboardId)).thenReturn(MutableStateFlow(emptyList()))
        whenever(viewModel.dashboards).thenReturn(MutableStateFlow(emptyList()))

        composeTestRule.setContent {
            AddExpenseScreen(
                dashboardId = dashboardId,
                viewModel = viewModel,
                onBackClick = {}
            )
        }

        composeTestRule.onNodeWithText("NEW CHAPTER").assertIsDisplayed()
        composeTestRule.onNodeWithText("Description").assertIsDisplayed()
        composeTestRule.onNodeWithText("Amount").assertIsDisplayed()
        composeTestRule.onNodeWithText("ADD EXPENSE").assertIsDisplayed()
    }
}
