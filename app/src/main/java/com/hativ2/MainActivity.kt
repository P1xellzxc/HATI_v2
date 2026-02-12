package com.hativ2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.hativ2.ui.screens.DashboardListScreen
import com.hativ2.ui.screens.ExpenseListScreen
import com.hativ2.ui.screens.AddExpenseScreen
import com.hativ2.ui.screens.DashboardDetailScreen
import com.hativ2.ui.screens.BalanceScreen
import com.hativ2.ui.screens.HistoryScreen
import com.hativ2.ui.screens.ChartsScreen
import com.hativ2.ui.theme.HatiV2Theme

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val viewModel: com.hativ2.ui.MainViewModel = viewModel()

    NavHost(navController = navController, startDestination = "dashboard_list") {
                composable("dashboard_list") {
            DashboardListScreen(
                viewModel = viewModel,
                onDashboardClick = { dashboardId ->
                    navController.navigate("dashboard_detail/$dashboardId")
                }
            )
        }
        composable(
            "dashboard_detail/{dashboardId}",
            arguments = listOf(navArgument("dashboardId") { type = NavType.StringType })
        ) { backStackEntry ->
            val dashboardId = backStackEntry.arguments?.getString("dashboardId") ?: return@composable
            DashboardDetailScreen(
                dashboardId = dashboardId,
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() },
                onAddExpenseClick = { _ ->
                    navController.navigate("add_expense/$dashboardId")
                },
                onBalanceClick = { _ ->
                    navController.navigate("charts/$dashboardId")
                },
                onViewExpensesClick = { _ ->
                    navController.navigate("history/$dashboardId")
                },
                onExpenseClick = { expenseId ->
                    navController.navigate("add_expense/$dashboardId?expenseId=$expenseId")
                }
            )
        }
        composable(
            "balance/{dashboardId}",
            arguments = listOf(navArgument("dashboardId") { type = NavType.StringType })
        ) { backStackEntry ->
            val dashboardId = backStackEntry.arguments?.getString("dashboardId") ?: return@composable
            BalanceScreen(
                dashboardId = dashboardId,
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(
            "expense_list/{dashboardId}",
            arguments = listOf(navArgument("dashboardId") { type = NavType.StringType })
        ) { backStackEntry ->
            val dashboardId = backStackEntry.arguments?.getString("dashboardId") ?: return@composable
            ExpenseListScreen(
                dashboardId = dashboardId,
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() },
                onAddExpenseClick = {
                    navController.navigate("add_expense/$dashboardId")
                },
                onEditExpenseClick = { expenseId ->
                    navController.navigate("add_expense/$dashboardId?expenseId=$expenseId")
                }
            )
        }
        composable(
            "add_expense/{dashboardId}?expenseId={expenseId}",
            arguments = listOf(
                navArgument("dashboardId") { type = NavType.StringType },
                navArgument("expenseId") { 
                    type = NavType.StringType 
                    nullable = true
                }
            )
        ) { backStackEntry ->
            val dashboardId = backStackEntry.arguments?.getString("dashboardId") ?: return@composable
            val expenseId = backStackEntry.arguments?.getString("expenseId")
            AddExpenseScreen(
                dashboardId = dashboardId,
                expenseId = expenseId,
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(
            "history/{dashboardId}",
            arguments = listOf(navArgument("dashboardId") { 
                type = NavType.StringType 
                nullable = true
            })
        ) { backStackEntry ->
            val dashboardId = backStackEntry.arguments?.getString("dashboardId")
            HistoryScreen(
                viewModel = viewModel,
                dashboardId = dashboardId,
                onBack = { navController.popBackStack() },
                onEditExpense = { expenseId ->
                    navController.navigate("add_expense/${dashboardId ?: "all"}?expenseId=$expenseId")
                }
            )
        }
        composable(
            "charts/{dashboardId}",
            arguments = listOf(navArgument("dashboardId") { 
                type = NavType.StringType 
                nullable = true
            })
        ) { backStackEntry ->
            val dashboardId = backStackEntry.arguments?.getString("dashboardId")
            ChartsScreen(
                viewModel = viewModel,
                dashboardId = dashboardId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

@dagger.hilt.android.AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HatiV2Theme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    AppNavigation()
                }
            }
        }
    }
}