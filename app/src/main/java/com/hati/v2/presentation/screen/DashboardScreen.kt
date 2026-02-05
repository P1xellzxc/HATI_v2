package com.hati.v2.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hati.v2.domain.model.Transaction
import com.hati.v2.presentation.animation.AntigravityEntrance
import com.hati.v2.presentation.animation.FallingLayout
import com.hati.v2.presentation.components.HalftoneOverlay
import com.hati.v2.presentation.components.MangaCard
import com.hati.v2.presentation.theme.MangaColors
import com.hati.v2.presentation.theme.MangaTypography
import com.hati.v2.data.local.MemberEntity

@Composable
fun DashboardScreen(
    onBack: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val dashboard by viewModel.dashboard.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    
    var showAddDialog by remember { mutableStateOf(false) }

    if (showAddDialog) {
        AddTransactionDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { desc, amount ->
                viewModel.addTransaction(
                    description = desc,
                    amount = amount,
                    category = "General", // TODO: Category selection
                    paidBy = "Me" // TODO: Member selection
                )
                showAddDialog = false
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
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Back Button
                        Box(modifier = Modifier.clickable(onClick = onBack)) {
                            BasicText(
                                text = "< BACK",
                                style = MangaTypography.labelLarge.copy(color = MangaColors.Black),
                                modifier = Modifier.padding(end = 16.dp)
                            )
                        }
                        
                        // Dashboard Name
                        BasicText(
                            text = dashboard?.name?.uppercase() ?: "LOADING...",
                            style = MangaTypography.headlineMedium.copy(color = MangaColors.Black)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Spacer(modifier = Modifier.height(16.dp))

            // Navigation Tabs
            var selectedTab by remember { mutableStateOf("EXPENSES") }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("EXPENSES", "ANALYTICS", "MEMBERS").forEach { tab ->
                     val isSelected = selectedTab == tab
                     Box(
                         modifier = Modifier
                             .clickable { selectedTab = tab }
                             .weight(1f)
                     ) {
                         MangaCard(
                             backgroundColor = if (isSelected) MangaColors.Black else MangaColors.White,
                             shadowOffset = if (isSelected) 2.dp else 4.dp
                         ) {
                             Box(
                                 modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                 contentAlignment = Alignment.Center
                             ) {
                                  BasicText(
                                    text = tab,
                                    style = MangaTypography.labelSmall.copy(
                                        color = if (isSelected) MangaColors.White else MangaColors.Black
                                    )
                                  )
                             }
                         }
                     }
                     if (tab != "MEMBERS") Spacer(modifier = Modifier.width(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Stats / Summary (Only show on Expenses or Analytics)
            if (selectedTab == "EXPENSES" || selectedTab == "ANALYTICS") {
                FallingLayout(delay = 100L) {
                    MangaCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = MangaColors.Accent
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            BasicText(
                                text = "TOTAL SPENT",
                                style = MangaTypography.labelSmall.copy(color = MangaColors.Black)
                            )
                            BasicText(
                                text = "₱${String.format("%.2f", transactions.sumOf { it.amount })}",
                                style = MangaTypography.displayMedium.copy(color = MangaColors.Black)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Content Area
            when (selectedTab) {
                "EXPENSES" -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        itemsIndexed(transactions) { index, transaction ->
                            FallingLayout(delay = (index + 2) * 50L) {
                                TransactionItem(transaction = transaction)
                            }
                        }
                    }
                }
                "ANALYTICS" -> {
                    val stats by viewModel.categoryStats.collectAsState()
                    val total = stats.values.sum()
                    
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        item {
                            MangaCard(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    BasicText("CATEGORY BREAKDOWN", style = MangaTypography.headlineSmall.copy(color = MangaColors.Black))
                                    Spacer(modifier = Modifier.height(16.dp))
                                    
                                    if (stats.isEmpty()) {
                                        BasicText("NO DATA YET", style = MangaTypography.bodyMedium)
                                    } else {
                                        stats.forEach { (category, amount) ->
                                            val percentage = if (total > 0) (amount / total * 100).toInt() else 0
                                            
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 8.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                BasicText("$category ($percentage%)", style = MangaTypography.bodyLarge)
                                                BasicText("₱${String.format("%.2f", amount)}", style = MangaTypography.bodyLarge)
                                            }
                                            // Progress bar simulation
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(8.dp)
                                                    .background(MangaColors.Black.copy(alpha = 0.1f))
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth(fraction = if (total > 0) (amount / total).toFloat() else 0f)
                                                        .height(8.dp)
                                                        .background(MangaColors.Black)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                "MEMBERS" -> {
                    val members by viewModel.members.collectAsState()
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        itemsIndexed(members) { index, member ->
                            FallingLayout(delay = (index + 2) * 50L) {
                                MemberItem(member = member)
                            }
                        }
                    }
                }
            }
        }

        // FAB: Add Expense
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            AntigravityEntrance(delay = 300L) {
                Box(modifier = Modifier.clickable { showAddDialog = true }) {
                    MangaCard(backgroundColor = MangaColors.Black, shadowOffset = 6.dp) {
                        BasicText(
                            text = "+ EXPENSE",
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
fun MemberItem(member: MemberEntity) {
    MangaCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar Placeholder (Circle)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MangaColors.Black, shape = androidx.compose.foundation.shape.CircleShape),
                contentAlignment = Alignment.Center
            ) {
                BasicText(
                    text = member.name.take(1).uppercase(),
                    style = MangaTypography.headlineSmall.copy(color = MangaColors.White)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                BasicText(
                    text = member.name.uppercase(),
                    style = MangaTypography.headlineSmall.copy(color = MangaColors.Black)
                )
                BasicText(
                    text = member.role.uppercase(),
                    style = MangaTypography.labelMedium.copy(color = MangaColors.Black.copy(alpha = 0.6f))
                )
            }
        }
    }
}

@Composable
fun TransactionItem(transaction: Transaction) {
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
                    text = transaction.description.uppercase(),
                    style = MangaTypography.headlineSmall.copy(color = MangaColors.Black)
                )
                BasicText(
                    text = "${transaction.category} • ${transaction.paidBy}",
                    style = MangaTypography.labelMedium.copy(color = MangaColors.Black.copy(alpha = 0.6f))
                )
            }
            BasicText(
                text = "₱${String.format("%.2f", transaction.amount)}",
                style = MangaTypography.headlineMedium.copy(color = MangaColors.Black)
            )
        }
    }
}

@Composable
fun AddTransactionDialog(onDismiss: () -> Unit, onConfirm: (String, Double) -> Unit) {
    var desc by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MangaColors.Black.copy(alpha = 0.5f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Box(modifier = Modifier.clickable(enabled = false) {}) {
            MangaCard(modifier = Modifier.padding(32.dp)) {
                Column(modifier = Modifier.padding(24.dp)) {
                    BasicText("NEW EXPENSE", style = MangaTypography.headlineMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    BasicTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MangaColors.White)
                            .padding(8.dp),
                        textStyle = MangaTypography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    BasicTextField(
                        value = amountStr,
                        onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) amountStr = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MangaColors.White)
                            .padding(8.dp),
                        textStyle = MangaTypography.bodyLarge
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        Box(modifier = Modifier.clickable { 
                            val amount = amountStr.toDoubleOrNull() ?: 0.0
                            if (desc.isNotBlank() && amount > 0) {
                                onConfirm(desc, amount)
                            }
                        }) {
                            MangaCard(backgroundColor = MangaColors.Black) {
                                BasicText(
                                    "ADD",
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
