package com.hati.v2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.hati.v2.presentation.screen.HomeScreen
import com.hati.v2.presentation.screen.LoginScreen
import com.hati.v2.presentation.theme.HatiTheme
import com.hati.v2.presentation.theme.MangaColors
import dagger.hilt.android.AndroidEntryPoint
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var supabaseClient: SupabaseClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            HatiTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MangaColors.White)
                ) {
                    HatiNavigation(supabaseClient)
                }
            }
        }
    }
}

@Composable
fun HatiNavigation(supabaseClient: SupabaseClient) {
    val navController = rememberNavController()
    var startDestination by remember { mutableStateOf<String?>(null) }

    // Check session on launch
    LaunchedEffect(Unit) {
        val session = supabaseClient.auth.currentSessionOrNull()
        startDestination = if (session != null) "home" else "login"
    }

    // Don't render until we know the start destination
    if (startDestination == null) return

    NavHost(
        navController = navController,
        startDestination = startDestination!!
    ) {
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }
        composable("home") {
            HomeScreen(
                onLogout = {
                    navController.navigate("login") {
                        popUpTo("home") { inclusive = true }
                    }
                }
            )
        }
    }
}
