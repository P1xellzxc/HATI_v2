package com.hativ2

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.hativ2.ui.auth.BiometricAuthGate
import com.hativ2.ui.components.BottomNavItem
import com.hativ2.ui.components.MangaBottomNav
import com.hativ2.ui.components.MangaFab
import com.hativ2.ui.screens.AddExpenseScreen
import com.hativ2.ui.screens.BalanceScreen
import com.hativ2.ui.screens.ChartsScreen
import com.hativ2.ui.screens.DashboardDetailScreen
import com.hativ2.ui.screens.DashboardListScreen
import com.hativ2.ui.screens.ExpenseListScreen
import com.hativ2.ui.screens.HistoryScreen
import com.hativ2.ui.screens.SettingsScreen
import com.hativ2.ui.theme.HatiV2Theme

private object Routes {
    const val HUB              = "hub"
    const val DASHBOARD_DETAIL = "dashboard_detail/{dashboardId}"
    const val BALANCE          = "balance/{dashboardId}"
    const val EXPENSE_LIST     = "expense_list/{dashboardId}"
    const val ADD_EXPENSE      = "add_expense/{dashboardId}?expenseId={expenseId}"
    const val HISTORY          = "history/{dashboardId}"
    const val CHARTS           = "charts/{dashboardId}"
    const val SETTINGS         = "settings"

    fun dashboardDetail(id: String) = "dashboard_detail/$id"
    fun addExpense(id: String, expenseId: String? = null) =
        if (expenseId == null) "add_expense/$id" else "add_expense/$id?expenseId=$expenseId"
    fun history(id: String? = null) = "history/${id ?: "all"}"
    fun charts(id: String? = null) = "charts/${id ?: "all"}"
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val viewModel: com.hativ2.ui.MainViewModel = viewModel()

    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val currentDashboardId = backStack?.arguments?.getString("dashboardId")
        ?.takeIf { it.isNotBlank() && it != "all" }

    val showBottomNav = currentRoute in setOf(
        Routes.HUB, Routes.DASHBOARD_DETAIL, Routes.HISTORY, Routes.CHARTS
    )
    val showFab = currentRoute == Routes.DASHBOARD_DETAIL ||
        (currentRoute == Routes.CHARTS && currentDashboardId != null)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomNav) {
                MangaBottomNav(
                    items = bottomNavItems(currentDashboardId),
                    currentRoute = currentRoute,
                    onClick = { route -> navigateTab(navController, route) }
                )
            }
        },
        floatingActionButton = {
            if (showFab && currentDashboardId != null) {
                MangaFab(
                    onClick = { navController.navigate(Routes.addExpense(currentDashboardId)) }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            AppNavHost(navController = navController, viewModel = viewModel)
        }
    }
}

@Composable
private fun AppNavHost(
    navController: androidx.navigation.NavHostController,
    viewModel: com.hativ2.ui.MainViewModel,
) {
    NavHost(
        navController = navController,
        startDestination = Routes.HUB,
        enterTransition = { fadeIn(tween(400)) + slideInHorizontally(tween(400, easing = FastOutSlowInEasing)) { it / 3 } },
        exitTransition  = { fadeOut(tween(250)) + scaleOut(tween(250), targetScale = 0.92f) },
        popEnterTransition = { fadeIn(tween(400)) + slideInHorizontally(tween(400, easing = FastOutSlowInEasing)) { -it / 3 } },
        popExitTransition  = { fadeOut(tween(250)) + slideOutHorizontally(tween(250)) { it / 3 } }
    ) {
        composable(Routes.HUB) {
            DashboardListScreen(
                viewModel = viewModel,
                onDashboardClick = { id -> navController.navigate(Routes.dashboardDetail(id)) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }
        composable(
            Routes.DASHBOARD_DETAIL,
            arguments = listOf(navArgument("dashboardId") { type = NavType.StringType })
        ) { entry ->
            val id = entry.arguments?.getString("dashboardId") ?: return@composable
            DashboardDetailScreen(
                dashboardId = id,
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() },
                onAddExpenseClick = { navController.navigate(Routes.addExpense(id)) },
                onBalanceClick = { navController.navigate(Routes.charts(id)) },
                onViewExpensesClick = { navController.navigate(Routes.history(id)) },
                onExpenseClick = { eid -> navController.navigate(Routes.addExpense(id, eid)) }
            )
        }
        composable(
            Routes.BALANCE,
            arguments = listOf(navArgument("dashboardId") { type = NavType.StringType })
        ) { entry ->
            val id = entry.arguments?.getString("dashboardId") ?: return@composable
            BalanceScreen(
                dashboardId = id,
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(
            Routes.EXPENSE_LIST,
            arguments = listOf(navArgument("dashboardId") { type = NavType.StringType })
        ) { entry ->
            val id = entry.arguments?.getString("dashboardId") ?: return@composable
            ExpenseListScreen(
                dashboardId = id,
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() },
                onAddExpenseClick = { navController.navigate(Routes.addExpense(id)) },
                onEditExpenseClick = { eid -> navController.navigate(Routes.addExpense(id, eid)) }
            )
        }
        composable(
            Routes.ADD_EXPENSE,
            arguments = listOf(
                navArgument("dashboardId") { type = NavType.StringType },
                navArgument("expenseId") {
                    type = NavType.StringType
                    nullable = true
                }
            )
        ) { entry ->
            val id = entry.arguments?.getString("dashboardId") ?: return@composable
            val expenseId = entry.arguments?.getString("expenseId")
            AddExpenseScreen(
                dashboardId = id,
                expenseId = expenseId,
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(
            Routes.HISTORY,
            arguments = listOf(navArgument("dashboardId") { type = NavType.StringType })
        ) { entry ->
            val rawId = entry.arguments?.getString("dashboardId")
            val id = rawId?.takeIf { it != "all" }
            HistoryScreen(
                viewModel = viewModel,
                dashboardId = id,
                onBack = { navController.popBackStack() },
                onEditExpense = { eid -> navController.navigate(Routes.addExpense(id ?: "all", eid)) }
            )
        }
        composable(
            Routes.CHARTS,
            arguments = listOf(navArgument("dashboardId") { type = NavType.StringType })
        ) { entry ->
            val rawId = entry.arguments?.getString("dashboardId")
            val id = rawId?.takeIf { it != "all" }
            ChartsScreen(
                viewModel = viewModel,
                dashboardId = id,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

/**
 * The bottom-nav set depends on whether we're inside a specific
 * Volume (dashboard_detail/{id}). When we are, include a Dashboard
 * tab and route History/Charts to that dashboard. Otherwise the
 * tabs span "all volumes".
 */
private fun bottomNavItems(currentDashboardId: String?): List<BottomNavItem> {
    val dashboardTab = if (currentDashboardId != null) {
        listOf(BottomNavItem(Routes.dashboardDetail(currentDashboardId), Icons.Default.Home, "Dashboard"))
    } else emptyList()
    return dashboardTab + listOf(
        BottomNavItem(Routes.history(currentDashboardId), Icons.Default.List, "History"),
        BottomNavItem(Routes.charts(currentDashboardId), Icons.Default.DateRange, "Charts"),
        BottomNavItem(Routes.HUB, Icons.Default.Menu, "Hub"),
    )
}

private fun navigateTab(navController: NavController, route: String) {
    navController.navigate(route) {
        popUpTo(Routes.HUB) {
            saveState = true
            inclusive = false
        }
        launchSingleTop = true
        restoreState = true
    }
}

// Why FragmentActivity instead of ComponentActivity:
// BiometricPrompt requires a FragmentActivity to manage its lifecycle.
// FragmentActivity extends ComponentActivity, so all Compose and Activity
// APIs remain fully available.
@dagger.hilt.android.AndroidEntryPoint
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: com.hativ2.ui.MainViewModel = viewModel()
            val prefs by viewModel.prefs.collectAsState()
            HatiV2Theme(
                themeMode    = prefs.themeMode,
                densityScale = prefs.density.factor,
                textScale    = prefs.textSize.factor,
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BiometricAuthGate {
                        AppNavigation()
                    }
                }
            }
        }
    }
}
