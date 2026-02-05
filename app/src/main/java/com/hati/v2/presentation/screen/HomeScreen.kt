package com.hati.v2.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hati.v2.data.local.DashboardEntity
import com.hati.v2.presentation.animation.AntigravityEntrance
import com.hati.v2.presentation.animation.FallingLayout
import com.hati.v2.presentation.components.HalftoneOverlay
import com.hati.v2.presentation.components.MangaCard
import com.hati.v2.presentation.theme.MangaColors
import com.hati.v2.presentation.theme.MangaTypography
import kotlinx.coroutines.launch

/**
 * HubScreen (formerly HomeScreen) - Displays user's dashboards (Folders).
 */
@Composable
fun HomeScreen(
    onNavigateToDashboard: (String) -> Unit,
    onLogout: () -> Unit,
    viewModel: HubViewModel = hiltViewModel()
) {
    val dashboards by viewModel.dashboards.collectAsState()
    val userEmail by viewModel.userEmail.collectAsState()
    val scope = rememberCoroutineScope()
    
    // Dialog state for creating new dashboard
    var showCreateDialog by remember { mutableStateOf(false) }

    if (showCreateDialog) {
        CreateDashboardDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { name ->
                viewModel.createDashboard(name)
                showCreateDialog = false
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MangaColors.White)
    ) {
        HalftoneOverlay(dotSize = 2f, spacing = 16f)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            AntigravityEntrance(delay = 0L) {
                MangaCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            BasicText(
                                text = "HATI² HUB",
                                style = MangaTypography.displaySmall.copy(color = MangaColors.Black)
                            )
                            BasicText(
                                text = userEmail ?: "GUEST",
                                style = MangaTypography.labelMedium.copy(color = MangaColors.Black.copy(alpha = 0.6f))
                            )
                        }

                        // Logout
                        Box(modifier = Modifier.clickable {
                            scope.launch {
                                viewModel.logout()
                                onLogout()
                            }
                        }) {
                            MangaCard(backgroundColor = MangaColors.Black, shadowOffset = 2.dp) {
                                BasicText(
                                    text = "LOGOUT",
                                    style = MangaTypography.labelLarge.copy(color = MangaColors.White),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section Title
            FallingLayout(delay = 100L) {
                BasicText(
                    text = "YOUR FOLDERS",
                    style = MangaTypography.headlineLarge.copy(color = MangaColors.Black)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Dashboard List
            if (dashboards.isEmpty()) {
                FallingLayout(delay = 200L) {
                    MangaCard(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            BasicText(
                                text = "NO FOLDERS YET",
                                style = MangaTypography.headlineSmall.copy(color = MangaColors.Black)
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    itemsIndexed(dashboards) { index, dashboard ->
                        FallingLayout(delay = (index + 2) * 50L) {
                            DashboardCard(
                                dashboard = dashboard,
                                onClick = { onNavigateToDashboard(dashboard.id) }
                            )
                        }
                    }
                }
            }
        }

        // FAB: Create Folder
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            AntigravityEntrance(delay = 300L) {
                Box(modifier = Modifier.clickable { showCreateDialog = true }) {
                    MangaCard(backgroundColor = MangaColors.Black, shadowOffset = 6.dp) {
                        BasicText(
                            text = "+ NEW FOLDER",
                            style = MangaTypography.displayMedium.copy(color = MangaColors.White),
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardCard(dashboard: DashboardEntity, onClick: () -> Unit) {
    Box(modifier = Modifier.clickable(onClick = onClick)) {
        MangaCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicText(
                        text = dashboard.name.uppercase(),
                        style = MangaTypography.headlineSmall.copy(color = MangaColors.Black)
                    )
                    
                    // Small "PHP" badge or similar
                    MangaCard(backgroundColor = MangaColors.Accent, shadowOffset = 0.dp) {
                        BasicText(
                            text = dashboard.currency,
                            style = MangaTypography.labelSmall.copy(color = MangaColors.Black),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                BasicText(
                    text = "CREATED: ${java.text.SimpleDateFormat("MMM dd").format(java.util.Date(dashboard.createdAt))}", // Quick format
                    style = MangaTypography.labelMedium.copy(color = MangaColors.Black.copy(alpha = 0.6f))
                )
            }
        }
    }
}

@Composable
fun CreateDashboardDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    
    // Simple overlay dialog
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MangaColors.Black.copy(alpha = 0.5f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Box(modifier = Modifier.clickable(enabled = false) {}) { // Prevent click through
            MangaCard(modifier = Modifier.padding(32.dp)) {
                Column(modifier = Modifier.padding(24.dp)) {
                    BasicText(
                        text = "NEW FOLDER",
                        style = MangaTypography.headlineMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Simple input replacement (Since I don't see the component code yet, using basic)
                    // In real app, use MangaInput
                    androidx.compose.foundation.text.BasicTextField(
                        value = text,
                        onValueChange = { text = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MangaColors.White)
                            .padding(8.dp),
                        textStyle = MangaTypography.bodyLarge
                        // TODO: Add proper border/styling
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        Box(modifier = Modifier.clickable { onConfirm(text) }) {
                            MangaCard(backgroundColor = MangaColors.Black) {
                                BasicText(
                                    text = "CREATE",
                                    style = MangaTypography.labelLarge.copy(color = MangaColors.White),
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

