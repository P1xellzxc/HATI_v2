package com.hativ2.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.hativ2.util.CsvExportManager
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hativ2.data.entity.ExpenseEntity
import com.hativ2.data.entity.PersonEntity
import com.hativ2.domain.model.DashboardWithStats
import com.hativ2.ui.MainViewModel
import com.hativ2.ui.theme.MangaBlack
import com.hativ2.ui.theme.NotionBlue
import com.hativ2.ui.theme.NotionGreen
import com.hativ2.ui.theme.NotionRed
import com.hativ2.ui.theme.NotionWhite
import com.hativ2.ui.theme.NotionYellow
import com.hativ2.ui.components.MangaBackButton

// Categories matching FinSplit
data class CategoryItem(
    val value: String,
    val label: String,
    val emoji: String,
    val color: Color
)

val CATEGORIES = listOf(
    CategoryItem("food", "Food", "🍽️", NotionYellow),
    CategoryItem("transport", "Transport", "🚗", NotionBlue),
    CategoryItem("shopping", "Shopping", "🛍️", NotionRed),
    CategoryItem("entertainment", "Entertainment", "🎬", Color(0xFFE879F9)),
    CategoryItem("utilities", "Utilities", "💡", NotionGreen),
    CategoryItem("other", "Other", "📦", Color.Gray)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: MainViewModel,
    dashboardId: String?,
    onBack: () -> Unit,
    onEditExpense: (String) -> Unit
) {
    val dashboardsWithStats by viewModel.dashboardsWithStats.collectAsState()
    
    val expensesFlow = remember(dashboardId) { viewModel.getExpenses(dashboardId ?: "") }
    val expenses by expensesFlow.collectAsState()
    
    val peopleFlow = remember(dashboardId) { viewModel.getPeople(dashboardId ?: "") }
    val people by peopleFlow.collectAsState()
    
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var selectedVolumeId by remember { mutableStateOf(dashboardId) }
    var deleteConfirmId by remember { mutableStateOf<String?>(null) }
    
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    
    // Get current dashboard
    val currentDashboard = dashboardsWithStats.find { it.id == selectedVolumeId }
    
    // Get all expenses for selected volume or all
    val allExpenses = if (selectedVolumeId == null) {
        // Get all expenses across all dashboards
        dashboardsWithStats.flatMap { dashboard ->
            // We need to filter from the main expenses list
            expenses.filter { it.dashboardId == dashboard.id }
        }
    } else {
        expenses
    }
    
    // Filter expenses
    val filteredExpenses = remember(allExpenses, searchQuery, selectedCategory) {
        allExpenses.filter { expense ->
            val matchesSearch = expense.description.lowercase().contains(searchQuery.lowercase())
            val matchesCategory = selectedCategory == null || expense.category == selectedCategory
            matchesSearch && matchesCategory
        }.sortedByDescending { it.createdAt }
    }

    // Export Launcher
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val csvContent = com.hativ2.util.CsvExportManager.generateCsv(filteredExpenses, people)
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.write(csvContent.toByteArray())
                    }
                    scope.launch { snackbarHostState.showSnackbar("Export successful!") }
                } catch (e: Exception) {
                    e.printStackTrace()
                    scope.launch { snackbarHostState.showSnackbar("Export failed: ${e.message}") }
                }
            }
        }
    }
    
    // Analytics
    val totalSpending = filteredExpenses.sumOf { it.amount }
    val expenseCount = filteredExpenses.size
    
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                navigationIcon = {
                    MangaBackButton(
                        onClick = onBack,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                },
                title = {
                    Column {
                        Text(
                            "Chapter Log",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            if (selectedVolumeId == null) 
                                "${dashboardsWithStats.size} volumes • $expenseCount chapters"
                            else 
                                currentDashboard?.title ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha=0.6f)
                        )
                    }
                },
                actions = {
                    // Export button
                    IconButton(onClick = { 
                        val defaultFilename = "finsplit_export_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())}.csv"
                        exportLauncher.launch(defaultFilename)
                    }) {
                        Box(
                            modifier = Modifier
                                .border(2.dp, MaterialTheme.colorScheme.onBackground, RoundedCornerShape(2.dp))
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(2.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Share, "Export", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurface)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Export", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Volume Selector Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // All Volumes tab
                VolumeTab(
                    title = "All Volumes",
                    isSelected = selectedVolumeId == null,
                    onClick = { selectedVolumeId = null }
                )
                
                // Individual volume tabs
                dashboardsWithStats.forEach { dashboard ->
                    VolumeTab(
                        title = dashboard.title,
                        isSelected = selectedVolumeId == dashboard.id,
                        onClick = { selectedVolumeId = dashboard.id }
                    )
                }
            }
            
            // Total Spending Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Shadow
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .offset(x = 4.dp, y = 4.dp)
                        .background(MaterialTheme.colorScheme.onBackground, RoundedCornerShape(2.dp))
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(2.dp))
                        .border(2.dp, MaterialTheme.colorScheme.onBackground, RoundedCornerShape(2.dp))
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("💰 Total Arc Spending", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha=0.6f))
                        }
                        Text(
                            "₱${String.format("%,.2f", totalSpending)}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            
            // Search Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .border(2.dp, MaterialTheme.colorScheme.onBackground, RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(2.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Search, "Search", modifier = Modifier.size(16.dp), tint = Color.Gray)
                    Spacer(modifier = Modifier.width(8.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.weight(1f),
                        textStyle = TextStyle(fontSize = 14.sp, color = MangaBlack),
                        cursorBrush = SolidColor(MangaBlack),
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text("Search expenses...", fontSize = 14.sp, color = Color.Gray)
                            }
                            innerTextField()
                        }
                    )
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Default.Clear, "Clear", modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
            
            // Category Filter Pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CategoryPill(
                    label = "All",
                    isSelected = selectedCategory == null,
                    onClick = { selectedCategory = null }
                )
                CATEGORIES.forEach { category ->
                    CategoryPill(
                        label = category.emoji + " " + category.label,
                        isSelected = selectedCategory == category.value,
                        color = category.color,
                        onClick = { selectedCategory = category.value }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Expense List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (filteredExpenses.isEmpty()) {
                    item {
                        com.hativ2.ui.components.MangaEmptyState(
                            message = if (searchQuery.isNotEmpty() || selectedCategory != null)
                                "No chapters match your filters"
                            else
                                "No chapters yet in this arc",
                            subMessage = "Start the story by adding an expense!",
                            modifier = Modifier.padding(top = 48.dp)
                        )
                    }
                } else {
                    items(filteredExpenses, key = { it.id }) { expense ->
                        ExpenseHistoryCard(
                            expense = expense,
                            people = people,
                            volumeTitle = if (selectedVolumeId == null) 
                                dashboardsWithStats.find { it.id == expense.dashboardId }?.title 
                            else null,
                            isDeleteConfirm = deleteConfirmId == expense.id,
                            onEdit = { onEditExpense(expense.id) },
                            onDeleteClick = { deleteConfirmId = expense.id },
                            onDeleteConfirm = { 
                                viewModel.deleteExpense(expense.id)
                                deleteConfirmId = null
                            },
                            onDeleteCancel = { deleteConfirmId = null }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VolumeTab(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clickable(onClick = onClick)
            .background(
                if (isSelected) MangaBlack else NotionWhite,
                RoundedCornerShape(2.dp)
            )
            .border(2.dp, MangaBlack, RoundedCornerShape(2.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) Color.White else MangaBlack,
            maxLines = 1
        )
    }
}

@Composable
fun CategoryPill(
    label: String,
    isSelected: Boolean,
    color: Color = NotionWhite,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clickable(onClick = onClick)
            .background(
                if (isSelected) color else NotionWhite,
                RoundedCornerShape(2.dp)
            )
            .border(2.dp, MangaBlack, RoundedCornerShape(2.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = MangaBlack
        )
    }
}

@Composable
fun ExpenseHistoryCard(
    expense: ExpenseEntity,
    people: List<PersonEntity>,
    volumeTitle: String?,
    isDeleteConfirm: Boolean,
    onEdit: () -> Unit,
    onDeleteClick: () -> Unit,
    onDeleteConfirm: () -> Unit,
    onDeleteCancel: () -> Unit
) {
    val isPayer = expense.paidBy == "user-current"
    val payerName = if (isPayer) "You" else people.find { it.id == expense.paidBy }?.name ?: "Unknown"
    val category = CATEGORIES.find { it.value == expense.category }
    val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
    val formattedDate = dateFormat.format(Date(expense.createdAt))
    
    // Light design for better readability
    val accentColor = if (isPayer) NotionGreen else NotionRed
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp) // Slight padding for shadow
    ) {
        // Shadow
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 4.dp, y = 4.dp)
                .background(MangaBlack, RoundedCornerShape(4.dp))
        )
        
        // Card Content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(NotionWhite, RoundedCornerShape(4.dp))
                .border(2.dp, MangaBlack, RoundedCornerShape(4.dp))
                .clickable { if (!isDeleteConfirm) onEdit() }
        ) {
            Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                // Colored Left Strip
                Box(
                    modifier = Modifier
                        .width(6.dp)
                        .fillMaxHeight()
                        .background(accentColor, RoundedCornerShape(topStart = 2.dp, bottomStart = 2.dp))
                )
                
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(12.dp)
                ) {
                    // Header: Description + Amount
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                expense.description,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Category Badge
                                if (category != null) {
                                    Box(
                                        modifier = Modifier
                                            .background(category.color.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                            .border(1.dp, category.color, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            category.label.uppercase(),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MangaBlack
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                
                                Text(
                                    formattedDate,
                                    fontSize = 12.sp,
                                    color = Color.Gray,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "₱${String.format("%,.2f", expense.amount)}",
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp
                            )
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            Text(
                                if (isPayer) "You paid" else "$payerName paid",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isPayer) NotionGreen else NotionRed
                            )
                        }
                    }
                    
                    // Actions (Delete Confirmation)
                    AnimatedVisibility(visible = isDeleteConfirm) {
                        Column {
                            Spacer(modifier = Modifier.height(12.dp))
                            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.LightGray))
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Delete this chapter?", fontSize = 12.sp, color = Color.Gray)
                                Spacer(modifier = Modifier.weight(1f))
                                Text(
                                    "CANCEL",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    modifier = Modifier.clickable(onClick = onDeleteCancel).padding(8.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clickable(onClick = onDeleteConfirm)
                                        .background(NotionRed, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("DELETE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
