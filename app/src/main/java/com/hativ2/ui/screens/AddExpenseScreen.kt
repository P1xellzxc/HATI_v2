package com.hativ2.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight

import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hativ2.ui.MainViewModel
import com.hativ2.ui.components.MangaTextField
import com.hativ2.ui.components.MangaDeleteDialog
import com.hativ2.ui.theme.MangaBlack
import com.hativ2.ui.theme.NotionBlue
import com.hativ2.ui.theme.NotionGreen
import com.hativ2.ui.theme.NotionRed
import com.hativ2.ui.theme.NotionWhite
import com.hativ2.ui.theme.NotionYellow
import kotlinx.coroutines.launch
import com.hativ2.ui.components.MangaBackButton
import com.hativ2.ui.components.MangaCornerRadius
import com.hativ2.ui.components.MangaBorderWidth

// Expense types
val EXPENSE_TYPES = listOf(
    "expense" to "💸 Expense"
)

// Split types
enum class SplitType { EQUAL, PERCENTAGE, EXACT }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(
    dashboardId: String,
    expenseId: String? = null,
    viewModel: MainViewModel,
    onBackClick: () -> Unit
) {
    var description by rememberSaveable { mutableStateOf("") }
    var amount by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("food") }
    var expenseType by rememberSaveable { mutableStateOf("expense") }
    var splitType by rememberSaveable { mutableStateOf(SplitType.EQUAL) }
    
    val people by viewModel.getPeople(dashboardId).collectAsState()
    
    var paidBy by rememberSaveable { mutableStateOf("user-current") }
    val splitWith = remember { mutableStateListOf<String>() }
    val splitPercentages = remember { mutableStateMapOf<String, String>() }
    val splitExactAmounts = remember { mutableStateMapOf<String, String>() }
    
    var hasInitializedSplits by rememberSaveable { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    var showPaidByDropdown by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    var areAllSelected by remember { mutableStateOf(true) }

    // Callbacks for SplitMemberRow
    val onToggleSplit = remember {
        { personId: String ->
            if (splitWith.contains(personId)) splitWith.remove(personId)
            else splitWith.add(personId)
            Unit
        }
    }
    
    val onPercentageChange = remember {
        { personId: String, value: String ->
            splitPercentages[personId] = value
            Unit
        }
    }

    val onExactAmountChange = remember {
        { personId: String, value: String ->
            splitExactAmounts[personId] = value
            Unit
        }
    }

    // Load expense data if editing
    LaunchedEffect(expenseId) {
        if (expenseId != null) {
            val expense = viewModel.getExpenseById(expenseId)
            if (expense != null) {
                isEditing = true
                description = expense.description
                amount = expense.amount.toString()
                paidBy = expense.paidBy ?: "user-current"
                category = expense.category
                
                val splits = viewModel.getSplitsForExpense(expenseId)
                splitWith.clear()
                splitWith.addAll(splits.map { it.personId })
            }
        }
    }

    // Initialize splitWith with all members by default
    LaunchedEffect(people) {
        if (!isEditing && !hasInitializedSplits) {
            if (!splitWith.contains("user-current")) {
                splitWith.add("user-current")
            }
            if (people.isNotEmpty()) {
                people.forEach { person ->
                    if (!splitWith.contains(person.id)) {
                        splitWith.add(person.id)
                    }
                }
                hasInitializedSplits = true
            }
        }
    }

    // Delete Dialog
    if (showDeleteDialog) {
        MangaDeleteDialog(
            title = "Delete Expense?",
            text = "Are you sure you want to delete this expense? This cannot be undone.",
            onConfirm = {
                if (expenseId != null) {
                    viewModel.deleteExpense(expenseId)
                    onBackClick()
                }
                showDeleteDialog = false
            },
            onDismiss = { showDeleteDialog = false }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        if (isEditing) "Edit Expense" else "Add Expense",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground
                    ) 
                },
                navigationIcon = {
                    MangaBackButton(
                        onClick = onBackClick,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                ),
                actions = {
                    if (isEditing) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = NotionRed)
                        }
                    }
                }
            )
        },
        bottomBar = {
            // Sticky Save Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(16.dp)
            ) {
                com.hativ2.ui.components.MangaButton(
                    onClick = {
                        val amountVal = amount.toDoubleOrNull()
                        
                        if (description.isBlank()) {
                            scope.launch { snackbarHostState.showSnackbar("Please enter a description") }
                            return@MangaButton
                        }
                        
                        if (amountVal == null || amountVal <= 0) {
                            scope.launch { snackbarHostState.showSnackbar("Amount must be greater than 0") }
                            return@MangaButton
                        }
                        
                        if (splitWith.isEmpty()) {
                            scope.launch { snackbarHostState.showSnackbar("Select at least one person to split with") }
                            return@MangaButton
                        }
                        
                        // VALIDATION: Percentage & Exact (Need to recalculate totals here since we are outside the item scope)
                        // We can access state directly
                        
                        if (splitType == SplitType.PERCENTAGE) {
                            val currentTotalPercent = splitWith.sumOf { splitPercentages[it]?.toDoubleOrNull() ?: 0.0 }
                            if (kotlin.math.abs(currentTotalPercent - 100.0) > 0.1) {
                                scope.launch { snackbarHostState.showSnackbar("Total percentage must be 100% (Current: $currentTotalPercent%)") }
                                return@MangaButton
                            }
                        }

                        if (splitType == SplitType.EXACT) {
                            val totalExact = splitWith.sumOf { splitExactAmounts[it]?.toDoubleOrNull() ?: 0.0 }
                            if (kotlin.math.abs(totalExact - amountVal) > 0.01) {
                                val diff = amountVal - totalExact
                                scope.launch { snackbarHostState.showSnackbar("Total exact amounts must equal ₱$amountVal (Missing: ₱${String.format("%.2f", diff)})") }
                                return@MangaButton
                            }
                        }

                        if (isEditing && expenseId != null) {
                            viewModel.updateExpense(
                                expenseId = expenseId,
                                dashboardId = dashboardId,
                                description = description,
                                amount = amountVal,
                                paidBy = paidBy,
                                category = category,
                                splitWith = splitWith.toList()
                            )
                        } else {
                            viewModel.createExpense(
                                dashboardId = dashboardId,
                                description = description,
                                amount = amountVal,
                                paidBy = paidBy,
                                category = category,
                                splitWith = splitWith.toList()
                            )
                        }
                        onBackClick()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = NotionGreen,
                    contentColor = MangaBlack
                ) {
                    Text(
                        if (isEditing) "UPDATE EXPENSE" else "SAVE EXPENSE"
                    )
                }
            }
        }
    ) { paddingValues ->
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Expense Type Selector
            item(key = "section_type") {
                SectionLabel("Type")
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    EXPENSE_TYPES.forEach { (type, label) ->
                        SelectableChip(
                            label = label,
                            isSelected = expenseType == type,
                            onClick = { expenseType = type },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Description
            item(key = "section_description") {
                MangaTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = "Description",
                    placeholder = "What was this for?",
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                )
            }

            // Amount
            item(key = "section_amount") {
                MangaTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = "Amount (₱)",
                    placeholder = "0.00",
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }

            // Category
            item(key = "section_category") {
                SectionLabel("Category")
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CATEGORIES.forEach { cat ->
                        SelectableChip(
                            label = "${cat.emoji} ${cat.label}",
                            isSelected = category == cat.value,
                            onClick = { category = cat.value },
                            selectedColor = cat.color
                        )
                    }
                }
            }

            // Paid By Dropdown
            item(key = "section_paid_by") {
                SectionLabel("Paid By")
                Spacer(modifier = Modifier.height(8.dp))
                Box {
                    DropdownSelector(
                        selectedLabel = if (paidBy == "user-current") "You" 
                            else people.find { it.id == paidBy }?.name ?: "Select",
                        onClick = { showPaidByDropdown = true }
                    )
                    DropdownMenu(
                        expanded = showPaidByDropdown,
                        onDismissRequest = { showPaidByDropdown = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("You") },
                            onClick = { 
                                paidBy = "user-current"
                                showPaidByDropdown = false
                            },
                            leadingIcon = {
                                if (paidBy == "user-current") Icon(Icons.Default.Check, null, tint = NotionGreen)
                            }
                        )
                        people.filter { it.id != "user-current" }.forEach { person ->
                            DropdownMenuItem(
                                text = { Text(person.name) },
                                onClick = { 
                                    paidBy = person.id
                                    showPaidByDropdown = false
                                },
                                leadingIcon = {
                                    if (paidBy == person.id) Icon(Icons.Default.Check, null, tint = NotionGreen)
                                }
                            )
                        }
                    }
                }
            }

            // Split Type Selector
            item(key = "section_split_type") {
                SectionLabel("Split Type")
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SelectableChip(
                        label = "⚖️ Equal",
                        isSelected = splitType == SplitType.EQUAL,
                        onClick = { splitType = SplitType.EQUAL },
                        modifier = Modifier.weight(1f)
                    )
                    SelectableChip(
                        label = "📊 Percentage",
                        isSelected = splitType == SplitType.PERCENTAGE,
                        onClick = { splitType = SplitType.PERCENTAGE },
                        modifier = Modifier.weight(1f)
                    )
                    SelectableChip(
                        label = "💵 Exact",
                        isSelected = splitType == SplitType.EXACT,
                        onClick = { splitType = SplitType.EXACT },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Split With Header
            item(key = "section_split_header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionLabel("Split With")
                    
                    // Select All / Deselect All Toggle
                    Text(
                        text = if (areAllSelected) "Deselect All" else "Select All",
                        style = MaterialTheme.typography.labelMedium,
                        color = NotionBlue,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            areAllSelected = !areAllSelected
                            if (areAllSelected) {
                                if (!splitWith.contains("user-current")) splitWith.add("user-current")
                                people.forEach { if (!splitWith.contains(it.id)) splitWith.add(it.id) }
                            } else {
                                splitWith.clear()
                            }
                        }
                    )
                }
            }
            
            // You
            item(key = "user-current") {
                SplitMemberRow(
                    personId = "user-current",
                    name = "You",
                    isSelected = splitWith.contains("user-current"),
                    onToggle = onToggleSplit,
                    splitType = splitType,
                    percentage = splitPercentages["user-current"] ?: "",
                    onPercentageChange = onPercentageChange,
                    exactAmount = splitExactAmounts["user-current"] ?: "",
                    onExactAmountChange = onExactAmountChange
                )
            }
            
            // Other People
            items(
                items = people.filter { it.id != "user-current" },
                key = { it.id }
            ) { person ->
                SplitMemberRow(
                    personId = person.id,
                    name = person.name,
                    isSelected = splitWith.contains(person.id),
                    onToggle = onToggleSplit,
                    splitType = splitType,
                    percentage = splitPercentages[person.id] ?: "",
                    onPercentageChange = onPercentageChange,
                    exactAmount = splitExactAmounts[person.id] ?: "",
                    onExactAmountChange = onExactAmountChange
                )
            }

            // Preview split amounts (Footer)
            item(key = "section_footer") {
                 // Running Total for Percentage
                val totalPercent by remember(splitPercentages, splitWith) {
                    androidx.compose.runtime.derivedStateOf {
                         if (splitType == SplitType.PERCENTAGE) {
                            splitWith.sumOf { splitPercentages[it]?.toDoubleOrNull() ?: 0.0 }
                         } else 0.0
                    }
                }
                
                if (splitType == SplitType.PERCENTAGE) {
                    val isInvalid = kotlin.math.abs(totalPercent - 100.0) > 0.1
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isInvalid) NotionRed.copy(alpha = 0.1f) else NotionGreen.copy(alpha = 0.1f), RoundedCornerShape(MangaCornerRadius))
                            .border(MangaBorderWidth, if (isInvalid) NotionRed else NotionGreen, RoundedCornerShape(MangaCornerRadius))
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Total Percentage:",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "${String.format("%.1f", totalPercent)}%",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isInvalid) NotionRed else NotionGreen
                            )
                        }
                        if (isInvalid) {
                            Text(
                                "Must be 100%",
                                style = MaterialTheme.typography.labelSmall,
                                color = NotionRed,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                val amountValue = amount.toDoubleOrNull() ?: 0.0
                if (amountValue > 0 && splitWith.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        val splitAmount = amountValue / splitWith.size
                        Text(
                            "Each person pays: ₱${String.format("%,.2f", splitAmount)}",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }

    }
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground
    )
}

@Composable
fun SelectableChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selectedColor: Color = NotionYellow
) {
    Box(
        modifier = modifier
            .clickable(onClick = onClick)
            .background(
                if (isSelected) selectedColor else Color.Transparent,
                RoundedCornerShape(MangaCornerRadius)
            )
            .border(
                MangaBorderWidth,
                if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                RoundedCornerShape(MangaCornerRadius)
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun DropdownSelector(
    selectedLabel: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(MangaCornerRadius))
            .border(MangaBorderWidth, MaterialTheme.colorScheme.onSurface, RoundedCornerShape(MangaCornerRadius))
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(selectedLabel, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        Icon(Icons.Default.KeyboardArrowDown, null, tint = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun SplitMemberRow(
    personId: String,
    name: String,
    isSelected: Boolean,
    onToggle: (String) -> Unit,
    splitType: SplitType,
    percentage: String,
    onPercentageChange: (String, String) -> Unit,
    exactAmount: String,
    onExactAmountChange: (String, String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(personId) }
            .background(
                if (isSelected) NotionGreen.copy(alpha = 0.2f) else Color.Transparent,
                RoundedCornerShape(MangaCornerRadius)
            )
            .border(
                MangaBorderWidth,
                if (isSelected) NotionGreen else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                RoundedCornerShape(MangaCornerRadius)
            )
            .padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(
                        if (isSelected) NotionGreen else Color.Transparent,
                        RoundedCornerShape(MangaCornerRadius)
                    )
                    .border(MangaBorderWidth, MaterialTheme.colorScheme.onSurface, RoundedCornerShape(MangaCornerRadius)),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(Icons.Default.Check, null, tint = MangaBlack, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(name, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        }
        
        // Show input field for percentage or exact based on split type
        AnimatedVisibility(visible = isSelected && splitType == SplitType.PERCENTAGE) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BasicTextField(
                    value = percentage,
                    onValueChange = { onPercentageChange(personId, it) },
                    textStyle = TextStyle(fontSize = 14.sp, textAlign = TextAlign.End, color = MaterialTheme.colorScheme.onSurface),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .width(50.dp)
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(MangaCornerRadius))
                        .border(1.dp, MaterialTheme.colorScheme.onSurface, RoundedCornerShape(MangaCornerRadius))
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("%", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }
        }
        
        AnimatedVisibility(visible = isSelected && splitType == SplitType.EXACT) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("₱", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.width(4.dp))
                BasicTextField(
                    value = exactAmount,
                    onValueChange = { onExactAmountChange(personId, it) },
                    textStyle = TextStyle(fontSize = 14.sp, textAlign = TextAlign.End, color = MaterialTheme.colorScheme.onSurface),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .width(70.dp)
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(MangaCornerRadius))
                        .border(1.dp, MaterialTheme.colorScheme.onSurface, RoundedCornerShape(MangaCornerRadius))
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                )
            }
        }
    }
}