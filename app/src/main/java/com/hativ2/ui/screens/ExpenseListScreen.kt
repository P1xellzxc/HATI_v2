package com.hativ2.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hativ2.data.entity.ExpenseEntity
import com.hativ2.ui.MainViewModel
import com.hativ2.ui.components.MangaCard
import com.hativ2.ui.theme.MangaBlack
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.hativ2.ui.components.MangaDeleteDialog
import com.hativ2.ui.theme.NotionWhite
import com.hativ2.ui.theme.NotionYellow
import com.hativ2.ui.theme.NotionRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseListScreen(
    dashboardId: String,
    viewModel: MainViewModel,
    onBackClick: () -> Unit,
    onAddExpenseClick: (String) -> Unit,
    onEditExpenseClick: (String) -> Unit // New callback
) {
    val expenses by viewModel.getExpenses(dashboardId).collectAsState()
    val people by viewModel.getPeople(dashboardId).collectAsState()
    
    var showDeleteDialog by remember { mutableStateOf<ExpenseEntity?>(null) }

    if (showDeleteDialog != null) {
        MangaDeleteDialog(
            title = "Delete Expense?",
            text = "Are you sure you want to delete '${showDeleteDialog?.description}'?",
            onConfirm = {
                showDeleteDialog?.let { viewModel.deleteExpense(it.id) }
                showDeleteDialog = null
            },
            onDismiss = { showDeleteDialog = null }
        )
    }

    // Helper to get name
    fun getName(id: String): String {
        return if (id == "user-current") "You" else people.find { it.id == id }?.name ?: "Unknown"
    }

    Scaffold(
        containerColor = NotionWhite,
        topBar = {
            TopAppBar(
                title = { Text("Expenses", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MangaBlack)
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
                onClick = { onAddExpenseClick(dashboardId) },
                containerColor = NotionYellow,
                contentColor = MangaBlack,
                modifier = Modifier.border(2.dp, MangaBlack, RoundedCornerShape(16.dp))
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Expense")
            }
        }
    ) { paddingValues ->
        if (expenses.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("No expenses yet. Start the arc!", style = MaterialTheme.typography.bodyLarge, color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(expenses) { expense ->
                    ExpenseItem(
                        expense = expense,
                        payerName = getName(expense.paidBy ?: "user-current"),
                        onEdit = { onEditExpenseClick(expense.id) },
                        onDelete = { showDeleteDialog = expense }
                    )
                }
            }
        }
    }
}

@Composable
fun ExpenseItem(
    expense: ExpenseEntity, 
    payerName: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    MangaCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = NotionWhite
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.description,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Paid by $payerName",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "₱${expense.amount}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = MangaBlack
                )
                
                Box {
                    IconButton(onClick = { expanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More", tint = MangaBlack)
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        containerColor = NotionWhite,
                        modifier = Modifier.background(NotionWhite).border(2.dp, MangaBlack)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            onClick = { expanded = false; onEdit() }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", color = NotionRed) },
                            onClick = { expanded = false; onDelete() }
                        )
                    }
                }
            }
        }
    }
}