package com.hativ2.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import java.text.SimpleDateFormat
import java.util.*
import com.hativ2.data.entity.ExpenseEntity
import com.hativ2.data.entity.PersonEntity
import com.hativ2.data.entity.SettlementEntity
import com.hativ2.domain.model.DashboardWithStats
import com.hativ2.ui.MainViewModel
import com.hativ2.ui.TransactionDisplayItem
import com.hativ2.ui.theme.*
import com.hativ2.ui.components.MangaBackButton
import com.hativ2.util.CsvExportManager

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
    CategoryItem("entertainment", "Entertainment", "🎬", NotionPurple),
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
    
    // Always fetch all transactions to support "All Volumes" properly
    val transactions by viewModel.getAllTransactions().collectAsState()
    
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
    
    // Filter by volume
    val visibleTransactions = remember(transactions, selectedVolumeId) {
        if (selectedVolumeId == null) {
            transactions
        } else {
            transactions.filter { item ->
                when(item) {
                    is TransactionDisplayItem.ExpenseItem -> item.expense.dashboardId == selectedVolumeId
                    is TransactionDisplayItem.SettlementItem -> item.settlement.dashboardId == selectedVolumeId
                }
            }
        }
    }
    
    // Filter by search & category
    val filteredTransactions = remember(visibleTransactions, searchQuery, selectedCategory) {
        visibleTransactions.filter { item ->
            val (description, category, amount) = when(item) {
                is TransactionDisplayItem.ExpenseItem -> Triple(item.expense.description, item.expense.category, item.expense.amount)
                is TransactionDisplayItem.SettlementItem -> Triple("Settlement", "payment", item.settlement.amount)
            }
            
            val matchesSearch = description.lowercase().contains(searchQuery.lowercase())
            val matchesCategory = selectedCategory == null || category == selectedCategory
            matchesSearch && matchesCategory
        }
    }

    // Export Launcher (Expenses Only for now to keep CSV format)
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val expensesToExport = filteredTransactions.mapNotNull { 
                        (it as? TransactionDisplayItem.ExpenseItem)?.expense 
                    }
                    val csvContent = com.hativ2.util.CsvExportManager.generateCsv(expensesToExport, people)
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
    val totalSpending = filteredTransactions.sumOf { 
         when(it) {
             is TransactionDisplayItem.ExpenseItem -> it.expense.amount
             is TransactionDisplayItem.SettlementItem -> 0.0 // Don't verify settlements in spending total
         }
    }
    val transactionCount = filteredTransactions.size
    
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
                                "${dashboardsWithStats.size} volumes • $transactionCount chapters"
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
            
            // Transaction List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (filteredTransactions.isEmpty()) {
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
                    items(filteredTransactions, key = { it.id }) { item ->
                        when(item) {
                            is TransactionDisplayItem.ExpenseItem -> {
                                val expense = item.expense
                                val isPayer = expense.paidBy == "user-current"
                                val payerName = if (isPayer) "You" else people.find { it.id == expense.paidBy }?.name ?: "Unknown"
                                
                                com.hativ2.ui.components.TransactionCard(
                                    title = expense.description,
                                    subtitle = "paid by $payerName",
                                    amount = "₱${String.format("%,.2f", expense.amount)}",
                                    amountColor = if (isPayer) NotionGreen else MangaBlack,
                                    date = java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault()).format(java.util.Date(expense.createdAt)),
                                    avatarText = payerName,
                                    avatarColor = if (isPayer) "default" else "white", // Simplified
                                    onClick = { onEditExpense(expense.id) }
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                            is TransactionDisplayItem.SettlementItem -> {
                                val settlement = item.settlement
                                val fromName = if(settlement.fromId == "user-current") "You" else people.find { it.id == settlement.fromId }?.name ?: "Unknown"
                                val toName = if(settlement.toId == "user-current") "You" else people.find { it.id == settlement.toId }?.name ?: "Unknown"
                                
                                com.hativ2.ui.components.TransactionCard(
                                    title = "Settlement",
                                    subtitle = "$fromName -> $toName",
                                    amount = "₱${String.format("%,.2f", settlement.amount)}",
                                    amountColor = NotionBlue,
                                    date = java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault()).format(java.util.Date(settlement.createdAt)),
                                    icon = { Icon(Icons.Default.Check, null, tint = MangaBlack) },
                                    onClick = { /* No action for now */ }
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
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


