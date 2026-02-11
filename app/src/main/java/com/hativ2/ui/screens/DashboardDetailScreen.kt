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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hativ2.ui.MainViewModel
import com.hativ2.ui.components.MangaButton
import com.hativ2.ui.components.MangaCard
import com.hativ2.ui.components.MangaTextField
import com.hativ2.ui.components.SettleUpDialog
import com.hativ2.ui.theme.MangaBlack
import com.hativ2.ui.theme.NotionBlue
import com.hativ2.ui.theme.NotionGreen
import com.hativ2.ui.theme.NotionRed
import com.hativ2.ui.theme.NotionWhite
import com.hativ2.ui.theme.NotionYellow
import com.hativ2.ui.components.EditDashboardDialog
import com.hativ2.data.entity.ExpenseEntity
import com.hativ2.ui.TransactionDisplayItem
import com.hativ2.ui.components.MangaBackButton
import com.hativ2.ui.components.MangaCornerRadius
import com.hativ2.ui.components.MangaBorderWidth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardDetailScreen(
    dashboardId: String,
    viewModel: MainViewModel,
    onBackClick: () -> Unit,
    onAddExpenseClick: (String) -> Unit,
    onBalanceClick: (String) -> Unit,
    onViewExpensesClick: (String) -> Unit,
    onExpenseClick: (String) -> Unit
) {
    val dashboards by viewModel.dashboards.collectAsState()
    val dashboard = dashboards.find { it.id == dashboardId }
    
    // Remember flows to avoid re-creating them on every recomposition
    val debtSummaryFlow = remember(dashboardId) { viewModel.getDebtSummary(dashboardId) }
    val debtSummary by debtSummaryFlow.collectAsState()
    
    val transactionsFlow = remember(dashboardId) { viewModel.getTransactions(dashboardId) }
    val transactions by transactionsFlow.collectAsState()
    
    // We need people to map creator IDs to names if needed, but for now we just show simplistic info
    val peopleFlow = remember(dashboardId) { viewModel.getPeople(dashboardId) }
    val people by peopleFlow.collectAsState()

    var showAddPersonDialog by remember { mutableStateOf(false) }
    var showEditDashboardDialog by remember { mutableStateOf(false) }
    var showSettleUpDialog by remember { mutableStateOf(false) }
    var settleUpFromId by remember { mutableStateOf<String?>(null) }
    var settleUpToId by remember { mutableStateOf<String?>(null) }
    var settleUpAmount by remember { mutableStateOf(0.0) }
    
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    if (dashboard == null) {
        // Handle loading or deleted state
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Volume not found or loading...")
        }
        return
    }

        if (showEditDashboardDialog) {
            EditDashboardDialog(
                title = dashboard.title,
                type = dashboard.dashboardType,
                color = dashboard.themeColor,
                onDismiss = { showEditDashboardDialog = false },
                onSave = { newTitle, newType, newColor ->
                    viewModel.updateDashboard(dashboardId, newTitle, newType, newColor)
                    showEditDashboardDialog = false
                }
            )
        }

        SettleUpDialog(
            isOpen = showSettleUpDialog,
            onDismiss = { showSettleUpDialog = false },
            onSettle = { fromId, toId, amount ->
                viewModel.settleUp(dashboardId, fromId, toId, amount)
                scope.launch { snackbarHostState.showSnackbar("Settled up ₱${String.format("%,.2f", amount)}!") }
            },
            initialFromId = settleUpFromId,
            initialToId = settleUpToId,
            initialAmount = settleUpAmount,
            allPeople = people
        )

        if (showAddPersonDialog) {
            AddPersonDialog(
                onDismiss = { showAddPersonDialog = false },
                onAdd = { name ->
                    viewModel.addPerson(dashboardId, name)
                    showAddPersonDialog = false
                    scope.launch { snackbarHostState.showSnackbar("Added $name to the party!") }
                }
            )
        }

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                // Custom Top Bar to match mockup
                Column(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Left: Back Button
                        MangaBackButton(onClick = onBackClick)

                        // Center: Title and Subtitle
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    getIconForType(dashboard.dashboardType), 
                                    contentDescription = null, 
                                    tint = NotionBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = dashboard.title.uppercase(),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                            Text(
                                text = "${dashboard.dashboardType} Arc",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha=0.6f)
                            )
                        }

                        // Right: Actions (Download & Add)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                             // Download Button Placeholder
                             Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .border(2.dp, MaterialTheme.colorScheme.onBackground, RoundedCornerShape(2.dp))
                                    .clickable { /* TODO: Download Action */ },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Warning, contentDescription = "Download", tint = MaterialTheme.colorScheme.onBackground)
                            }
                            
                            Spacer(modifier = Modifier.width(8.dp))

                            // + ADD Button Removed (Moved to FAB)
                        }
                    }
                    // Divider
                    Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(MaterialTheme.colorScheme.onBackground))
                }
            },
        floatingActionButton = {
            // Manga Style FAB
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clickable { onAddExpenseClick(dashboardId) }
            ) {
                // Hard Shadow
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .offset(x = 4.dp, y = 4.dp)
                        .background(MaterialTheme.colorScheme.onBackground, RoundedCornerShape(MangaCornerRadius))
                )
                // FAB Content
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(NotionGreen, RoundedCornerShape(MangaCornerRadius))
                        .border(2.dp, MaterialTheme.colorScheme.onBackground, RoundedCornerShape(MangaCornerRadius)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Add, 
                        contentDescription = "Add Expense", 
                        tint = MangaBlack, // Always black on Green
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
        ) { paddingValues ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Balance Overview Card
            item {
                BalanceOverviewCard(
                    title = dashboard.title,
                    balance = debtSummary.balances["user-current"] ?: 0.0,
                    totalSpent = expenses.sumOf { it.amount }
                )
            }

            // Action Grid
            item {
                ActionGrid(
                    onAddExpense = { onAddExpenseClick(dashboardId) },
                    onViewHistory = { onViewExpensesClick(dashboardId) },
                    onViewCharts = { onBalanceClick(dashboardId) },
                    onAddMember = { showAddPersonDialog = true }
                )
            }
            
            // Empty State
            if (expenses.isEmpty()) {
                item {
                    com.hativ2.ui.components.MangaEmptyState(
                        message = "No chapters written yet",
                        subMessage = "Start the story by adding an expense!",
                        modifier = Modifier.padding(top = 32.dp)
                    )
                }
            }
            
            // Spending By Member
            if (expenses.isNotEmpty()) {
                item {
                    SpendingByMemberCard(
                        memberShares = debtSummary.memberShares,
                        people = people
                    )
                }
            }

            // Owed To You & You Owe
            item {
                // Calculate dynamic names for debts
                // Calculate detailed debts
                val owedToYouDetails = debtSummary.transactions
                    .filter { it.toId == "user-current" }
                    .map { tx -> 
                        val name = people.find { it.id == tx.fromId }?.name?.split(" ")?.firstOrNull() ?: "Unknown"
                        name to tx.amount
                    }

                val youOweDetails = debtSummary.transactions
                    .filter { it.fromId == "user-current" }
                    .map { tx -> 
                         val name = people.find { it.id == tx.toId }?.name?.split(" ")?.firstOrNull() ?: "Unknown"
                         name to tx.amount
                    }

                Row(
                   modifier = Modifier.fillMaxWidth(),
                   horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Owed To You
                    SummaryCard(
                        modifier = Modifier.weight(1f),
                        title = "OWED TO YOU",
                        amount = debtSummary.totalOwedToYou,
                        details = owedToYouDetails,
                        headerColor = NotionGreen.copy(alpha=0.15f),
                        icon = null, // Icons.Default.ArrowBack (rotated)
                        onSettleUpClick = { /* TODO: Settle Up Logic */ }
                    )
                    // You Owe
                    SummaryCard(
                        modifier = Modifier.weight(1f),
                        title = "YOU OWE",
                        amount = debtSummary.totalYouOwe,
                        details = youOweDetails,
                        headerColor = NotionRed.copy(alpha=0.15f),
                        onSettleUpClick = if (debtSummary.totalYouOwe > 0) {
                            {
                                val youOweList = debtSummary.youOwe
                                if (youOweList.size == 1) {
                                    val first = youOweList.first()
                                    settleUpToId = first.toId
                                    settleUpAmount = first.amount
                                } else {
                                    // Multiple people or generic settle up
                                    settleUpToId = null
                                    settleUpAmount = 0.0
                                }
                                settleUpFromId = "user-current"
                                showSettleUpDialog = true
                            }
                        } else null
                    )
                }
            }

            // Recent Chapters (Transactions)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                         Icon(Icons.Default.List, contentDescription = null, modifier = Modifier.size(16.dp))
                         Spacer(modifier = Modifier.width(8.dp))
                         Text(
                            "RECENT CHAPTERS",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    if (expenses.isNotEmpty()) {
                         Box(
                            modifier = Modifier
                                .border(1.dp, MangaBlack, RoundedCornerShape(2.dp))
                                .clickable { onViewExpensesClick(dashboardId) }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("View All >", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (expenses.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("👋", fontSize = 32.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Start by adding people to split with",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }
                }
            } else {
                items(expenses.take(5)) { expense ->
                     val payerName = people.find { it.id == expense.paidBy }?.name ?: "Unknown"
                     ChapterItem(
                         expense = expense,
                         payerName = payerName,
                         onClick = { onExpenseClick(expense.id) }
                     )
                     Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun BalanceOverviewCard(
    title: String,
    balance: Double,
    totalSpent: Double
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        // Hard Shadow
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 6.dp, y = 6.dp)
                .background(MangaBlack)
        )
        
        // Card Content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(NotionWhite)
                .border(MangaBorderWidth, MangaBlack)
                .padding(24.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Your Balance in \"${title.uppercase()}\"",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Balance Box
                Box(
                    modifier = Modifier
                        .background(NotionGreen.copy(alpha = 0.1f)) // Light Green
                        .border(MangaBorderWidth, MangaBlack)
                        .padding(horizontal = 32.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "₱${String.format("%,.2f", balance)}",
                        style = MaterialTheme.typography.displayMedium, // Large text
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                        color = MangaBlack
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Total spent: ₱${String.format("%.2f", totalSpent)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun SpendingByMemberCard(
    memberShares: Map<String, Double>,
    people: List<com.hativ2.data.entity.PersonEntity>
) {
    val totalSpending = memberShares.values.sum()
    
    Box(modifier = Modifier.fillMaxWidth()) {
        // Hard Shadow
        Box(
             modifier = Modifier.matchParentSize().offset(x = 6.dp, y = 6.dp).background(MangaBlack)
        )
        // Card Content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(NotionWhite)
                .border(MangaBorderWidth, MangaBlack)
                .padding(16.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                     Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp)) 
                     Spacer(modifier = Modifier.width(8.dp))
                     Text("SPENDING BY MEMBER", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(MangaBlack))
                Spacer(modifier = Modifier.height(24.dp))
                
                if (memberShares.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                         Text("No spending data yet", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                    }
                } else {
                     memberShares.forEach { (userId, amount) ->
                         key(userId) {
                             val name = if(userId == "user-current") "You" else people.find { it.id == userId }?.name ?: "Unknown"
                             val percentage = if (totalSpending > 0) (amount / totalSpending * 100) else 0.0
                             val progress = (percentage / 100f).toFloat().coerceIn(0f, 1f)
                             
                             MemberSpendingRow(
                                 name = name,
                                 amount = amount,
                                 progress = progress,
                                 percentage = percentage
                             )
                         }
                     }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun MemberSpendingRow(
    name: String,
    amount: Double,
    progress: Float,
    percentage: Double
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "progress"
    )

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        // Name and amount row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(
                "₱${String.format("%,.2f", amount)}",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
        
        Spacer(modifier = Modifier.height(6.dp))
        
        // Progress bar with percentage
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Progress bar container
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(12.dp)
                    .background(Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(2.dp))
                    .border(1.dp, MangaBlack, RoundedCornerShape(2.dp))
            ) {
                // Static progress fill (no animation to prevent stuttering)
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedProgress)
                        .background(NotionBlue, RoundedCornerShape(1.dp))
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Percentage label
            Text(
                "${String.format("%.0f", percentage)}%",
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = Color.Gray,
                modifier = Modifier.width(36.dp)
            )
        }
    }
}

@Composable
fun ActionGrid(
    onAddExpense: () -> Unit,
    onViewHistory: () -> Unit,
    onViewCharts: () -> Unit,
    onAddMember: () -> Unit
) {
    // 1x4 Row Grid
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ActionCard(
            modifier = Modifier.weight(1f),
            title = "History",
            icon = Icons.Default.List, // History Icon
            onClick = onViewHistory
        )
        ActionCard(
            modifier = Modifier.weight(1f),
            title = "Charts",
            icon = Icons.Default.DateRange, // Charts Icon
            onClick = onViewCharts
        )
        ActionCard(
            modifier = Modifier.weight(1f),
            title = "Add",
            icon = Icons.Default.Add,
            onClick = onAddExpense
        )
        ActionCard(
            modifier = Modifier.weight(1f),
            title = "Member",
            icon = Icons.Default.Person,
            onClick = onAddMember
        )
    }
}

@Composable
fun ActionCard(
    modifier: Modifier = Modifier,
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .aspectRatio(1f) // Square
            .clickable(onClick = onClick)
    ) {
         // Hard Shadow
         Box(
             modifier = Modifier
                 .matchParentSize()
                 .offset(x = 4.dp, y = 4.dp)
                 .background(MangaBlack)
         )
         // Main content
         Box(
             modifier = Modifier
                 .fillMaxSize()
                 .background(NotionWhite)
                 .border(MangaBorderWidth, MangaBlack),
             contentAlignment = Alignment.Center
         ) {
             Column(horizontalAlignment = Alignment.CenterHorizontally) {
                 Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
                 Spacer(modifier = Modifier.height(4.dp))
                 Text(title, fontWeight = FontWeight.Bold, fontSize = 10.sp)
             }
         }
    }
}

@Composable
fun SummaryCard(
    modifier: Modifier = Modifier,
    title: String,
    amount: Double,
    details: List<Pair<String, Double>> = emptyList(),
    headerColor: Color,
    icon: ImageVector? = null,
    onSettleUpClick: (() -> Unit)? = null
) {
    Box(modifier = modifier) {
         // Hard Shadow
         Box(
             modifier = Modifier.matchParentSize().offset(x = 4.dp, y = 4.dp).background(MangaBlack)
         )
         
         // Card Container
         Column(
             modifier = Modifier
                 .fillMaxWidth()
                 .background(NotionWhite)
                 .border(MangaBorderWidth, MangaBlack)
         ) {
             // Header
             Box(
                 modifier = Modifier
                     .fillMaxWidth()
                     .background(headerColor)
                     .border(width = 0.dp, color = Color.Transparent) // No border for header itself inside the card
             ) {
                 Box(
                     modifier = Modifier
                         .fillMaxWidth()
                         .height(1.dp)
                         .align(Alignment.BottomCenter)
                         .background(MangaBlack)
                 )
                 
                 Row(
                     verticalAlignment = Alignment.CenterVertically,
                     modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                 ) {
                     // Arrow Icon
                     Text(
                         if(title.contains("OWED TO YOU")) "↙" else "↗", 
                         fontWeight = FontWeight.Bold,
                         fontSize = 12.sp,
                         color = MangaBlack
                     )
                     Spacer(modifier = Modifier.width(8.dp))
                     Text(
                         title, 
                         fontSize = 11.sp, 
                         fontWeight = FontWeight.Bold, 
                         letterSpacing = 1.sp,
                         color = MangaBlack
                     )
                 }
             }

             // Body
             Column(
                 modifier = Modifier
                     .fillMaxWidth()
                     .padding(16.dp)
             ) {
                 Text(
                     "₱${String.format("%,.2f", amount)}", 
                     fontWeight = FontWeight.Black, 
                     fontSize = 20.sp, 
                     color = MangaBlack
                 )
                 
                 Spacer(modifier = Modifier.height(4.dp))
                 
                 Spacer(modifier = Modifier.height(4.dp))
                 
                 if (details.isEmpty()) {
                     Text(
                         if (amount > 0) "No details" else "All settled up", 
                         style = MaterialTheme.typography.labelSmall, 
                         color = Color.Gray,
                         fontWeight = FontWeight.Bold
                     )
                 } else {
                     Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                         details.forEach { (name, debt) ->
                             Row(
                                 modifier = Modifier.fillMaxWidth(),
                                 horizontalArrangement = Arrangement.SpaceBetween
                             ) {
                                 Text(
                                     name,
                                     style = MaterialTheme.typography.labelSmall,
                                     color = Color.Gray,
                                     fontWeight = FontWeight.SemiBold
                                 )
                                 Text(
                                     "₱${String.format("%,.0f", debt)}",
                                     style = MaterialTheme.typography.labelSmall,
                                     color = if(title.contains("OWED")) NotionGreen else NotionRed,
                                     fontWeight = FontWeight.Bold
                                 )
                             }
                         }
                     }
                 }
                 
                 // Settle Up Button
                 if (onSettleUpClick != null) {
                     Spacer(modifier = Modifier.height(8.dp))
                     Box(
                         modifier = Modifier
                             .fillMaxWidth()
                             .background(NotionYellow)
                             .border(1.dp, MangaBlack)
                             .clickable(onClick = onSettleUpClick)
                             .padding(vertical = 6.dp),
                         contentAlignment = Alignment.Center
                     ) {
                         Text(
                             "SETTLE UP",
                             fontWeight = FontWeight.Bold,
                             fontSize = 10.sp,
                             letterSpacing = 1.sp
                         )
                     }
                 }
             }
         }
    }
}

@Composable
fun ChapterItem(
    expense: ExpenseEntity,
    payerName: String,
    onClick: () -> Unit
) {
    // Manga-style list item
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Box(
             modifier = Modifier.matchParentSize().offset(x = 2.dp, y = 2.dp).background(MangaBlack)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(NotionWhite)
                .border(MangaBorderWidth, MangaBlack)
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Category Icon placeholder
                Box(
                    modifier = Modifier.size(32.dp).background(NotionYellow).border(1.dp, MangaBlack),
                    contentAlignment = Alignment.Center
                ) {
                    Text(expense.category.take(1).uppercase(), fontWeight = FontWeight.Bold)
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(expense.description, fontWeight = FontWeight.Bold)
                    Text(
                        "Paid by $payerName", 
                        style = MaterialTheme.typography.labelSmall, 
                        color = Color.Gray
                    )
                }
                
                Text(
                    "₱${String.format("%,.2f", expense.amount)}", 
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun AddPersonDialog(onDismiss: () -> Unit, onAdd: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        MangaCard(modifier = Modifier.fillMaxWidth(), backgroundColor = NotionWhite) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("New Character", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(16.dp))
                MangaTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Name"
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    MangaButton(
                        onClick = onDismiss, 
                        backgroundColor = NotionRed, 
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Cancel")
                    }
                    MangaButton(
                        onClick = { if(name.isNotBlank()) onAdd(name) },
                        backgroundColor = NotionGreen
                    ) {
                        Text("Add")
                    }
                }
            }
        }
    }
}

// Helper for Icons
fun getIconForType(type: String): ImageVector {
    return when(type.lowercase()) {
        "travel" -> Icons.Default.DateRange // Placeholder, ideally specific icons
        "household" -> Icons.Default.Settings // Placeholder
        "event" -> Icons.Default.Settings // Placeholder
        else -> Icons.Default.List
    }
}


