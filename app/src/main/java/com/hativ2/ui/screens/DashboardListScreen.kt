package com.hativ2.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hativ2.data.entity.DashboardEntity
import com.hativ2.domain.model.DashboardWithStats
import com.hativ2.ui.MainViewModel
import com.hativ2.ui.components.AddDashboardDialog
import com.hativ2.ui.components.MangaDeleteDialog
import com.hativ2.ui.theme.MangaBlack
import com.hativ2.ui.theme.NotionBlue
import com.hativ2.ui.theme.NotionGreen
import com.hativ2.ui.theme.NotionRed
import com.hativ2.ui.theme.NotionWhite
import com.hativ2.ui.theme.NotionYellow
import com.hativ2.ui.components.EditDashboardDialog
import com.hativ2.ui.components.MangaCornerRadius
import com.hativ2.ui.components.MangaBorderWidth
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardListScreen(
    viewModel: MainViewModel = viewModel(),
    onDashboardClick: (String) -> Unit
) {
    val dashboardsWithStats by viewModel.dashboardsWithStats.collectAsState()
    var showDeleteDialog by remember { mutableStateOf<DashboardWithStats?>(null) }
    var showEditDialog by remember { mutableStateOf<DashboardWithStats?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    if (showAddDialog) {
        AddDashboardDialog(
            onDismiss = { showAddDialog = false },
            onCreate = { title, type, color ->
                viewModel.createDashboard(title, type, color)
                showAddDialog = false
                scope.launch { snackbarHostState.showSnackbar("Volume '$title' created!") }
            }
        )
    }

    if (showDeleteDialog != null) {
        MangaDeleteDialog(
            title = "Delete Volume?",
            text = "Are you sure you want to delete '${showDeleteDialog?.title}'? This action cannot be undone.",
            onConfirm = {
                val title = showDeleteDialog?.title ?: "Volume"
                showDeleteDialog?.let { viewModel.deleteDashboard(it.id) }
                showDeleteDialog = null
                scope.launch { snackbarHostState.showSnackbar("$title deleted") }
            },
            onDismiss = { showDeleteDialog = null }
        )
    }

    if (showEditDialog != null) {
        EditDashboardDialog(
            title = showEditDialog!!.title,
            type = showEditDialog!!.dashboardType,
            color = showEditDialog!!.themeColor,
            onDismiss = { showEditDialog = null },
            onSave = { newTitle, newType, newColor ->
                showEditDialog?.let { dashboard ->
                    viewModel.updateDashboard(dashboard.id, newTitle, newType, newColor)
                }
                showEditDialog = null
            }
        )
    }

    Scaffold(
        containerColor = NotionWhite,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Logo box
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(MangaBlack, RoundedCornerShape(4.dp))
                                .border(2.dp, MangaBlack, RoundedCornerShape(4.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("H", color = NotionWhite, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "HATI", 
                            style = MaterialTheme.typography.displaySmall,
                            color = MangaBlack
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NotionWhite,
                    titleContentColor = MangaBlack
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = NotionYellow,
                contentColor = MangaBlack,
                modifier = Modifier.border(MangaBorderWidth, MangaBlack, RoundedCornerShape(MangaCornerRadius))
            ) {
                Row(modifier = Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Add, contentDescription = "Add Dashboard")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Volume", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // Header Section
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Your Story Volumes",
                style = MaterialTheme.typography.headlineMedium,
                color = MangaBlack
            )
            Text(
                "Each volume contains a separate expense arc.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(24.dp))

            // Grid Content
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                if (dashboardsWithStats.isEmpty()) {
                    item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                         com.hativ2.ui.components.MangaEmptyState(
                            message = "No volumes found", 
                            subMessage = "Create a new volume to start your journey!",
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }
                }
                
                items(dashboardsWithStats) { dashboard ->
                    VolumeCard(
                        dashboard = dashboard, 
                        onClick = { onDashboardClick(dashboard.id) },
                        onDelete = { showDeleteDialog = dashboard },
                        onEdit = { showEditDialog = dashboard }
                    )
                }
                
                item {
                    NewVolumeCard(onClick = { showAddDialog = true })
                }
            }
        }
    }
}

@Composable
fun VolumeCard(
    dashboard: DashboardWithStats, 
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    val spineColor = when (dashboard.themeColor) {
        "blue" -> NotionBlue
        "green" -> NotionGreen
        "red" -> NotionRed
        "yellow" -> NotionYellow
        else -> NotionWhite
    }

    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.75f)
            .clickable(onClick = onClick)
    ) {
        // Hard Shadow
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset(x = 6.dp, y = 6.dp)
                .background(MangaBlack, RoundedCornerShape(MangaCornerRadius))
        )

        // Main Card
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(NotionWhite, RoundedCornerShape(MangaCornerRadius))
                .border(2.dp, MangaBlack, RoundedCornerShape(MangaCornerRadius))
                .clip(RoundedCornerShape(MangaCornerRadius))
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                // Spine
                Box(
                    modifier = Modifier
                        .width(8.dp)
                        .fillMaxSize()
                        .background(spineColor)
                )

                Column(modifier = Modifier.weight(1f)) {
                    // Cover Image Area
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.5f)
                            .background(spineColor.copy(alpha = 0.4f))
                            .border(width = 0.dp, color = Color.Transparent)
                    ) {
                        // Border bottom for cover
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .fillMaxWidth()
                                .height(2.dp)
                                .background(MangaBlack)
                        )
                        
                        // Menu Button
                        Box(modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)) {
                             IconButton(
                                 onClick = { expanded = true }, 
                                 modifier = Modifier.size(48.dp) // Increased touch target
                             ) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = MangaBlack, modifier = Modifier.size(24.dp))
                             }
                             DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                containerColor = NotionWhite,
                                modifier = Modifier.background(NotionWhite).border(2.dp, MangaBlack)
                             ) {
                                  DropdownMenuItem(
                                    text = { Text("Edit") },
                                    onClick = { onEdit(); expanded = false }
                                  )
                                  DropdownMenuItem(
                                    text = { Text("Delete", color = NotionRed) },
                                    onClick = { expanded = false; onDelete() }
                                  )
                             }
                        }

                        // Icon placeholder
                        Text(
                            text = when(dashboard.dashboardType) {
                                "travel" -> "✈️"
                                "household" -> "🏠"
                                "event" -> "🎉"
                                else -> "📁"
                            },
                            style = MaterialTheme.typography.displayMedium,
                            modifier = Modifier.align(Alignment.Center)
                        )
                        
                        // Balance Tag (Bottom Right) - Now with real data
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(4.dp)
                                .background(if (dashboard.netBalance >= 0) NotionGreen else NotionRed, RoundedCornerShape(MangaCornerRadius))
                                .border(2.dp, MangaBlack, RoundedCornerShape(MangaCornerRadius))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "${if (dashboard.netBalance >= 0) "+" else ""}${dashboard.currencySymbol}${String.format("%.0f", dashboard.netBalance)}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Content Area
                    Column(
                        modifier = Modifier
                            .weight(0.5f)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = dashboard.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${dashboard.dashboardType} • ${dashboard.expenseCount} ch.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                        
                        // Total - Now with real data
                        Column {
                            Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(MangaBlack))
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Total", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                Text("${dashboard.currencySymbol}${String.format("%,.2f", dashboard.totalSpent)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NewVolumeCard(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.75f)
            .clickable(onClick = onClick)
    ) {
         // Shadow
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset(x = 6.dp, y = 6.dp)
                .background(MangaBlack.copy(alpha = 0.2f), RoundedCornerShape(MangaCornerRadius))
        )
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(NotionWhite.copy(alpha = 0.5f), RoundedCornerShape(MangaCornerRadius))
                .border(2.dp, MangaBlack, RoundedCornerShape(MangaCornerRadius)) 
                .clip(RoundedCornerShape(MangaCornerRadius)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(NotionWhite, RoundedCornerShape(MangaCornerRadius))
                        .border(2.dp, MangaBlack, RoundedCornerShape(MangaCornerRadius)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = MangaBlack)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("New Volume", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text("Start a new arc", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }
    }
}
