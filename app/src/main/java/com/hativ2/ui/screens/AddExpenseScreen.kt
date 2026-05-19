package com.hativ2.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hativ2.R
import com.hativ2.ui.MainViewModel
import com.hativ2.ui.components.MangaBorderWidth
import com.hativ2.ui.components.MangaButton
import com.hativ2.ui.components.MangaCard
import com.hativ2.ui.components.MangaCategoryChipGrid
import com.hativ2.ui.components.MangaCornerRadius
import com.hativ2.ui.components.MangaDashedCard
import com.hativ2.ui.components.MangaDeleteDialog
import com.hativ2.ui.components.MangaIconButton
import com.hativ2.ui.components.MangaSegmentedControl
import com.hativ2.ui.components.MangaTextField
import com.hativ2.ui.components.MangaTopBar
import com.hativ2.ui.theme.AccentCategoryFood
import com.hativ2.ui.theme.MangaBlack
import com.hativ2.ui.theme.NotionBlue
import com.hativ2.ui.theme.NotionGreen
import com.hativ2.ui.theme.NotionMuted
import com.hativ2.ui.theme.NotionRed
import com.hativ2.ui.theme.SurfaceHero
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

enum class SplitType { EQUAL, EXACT, PERCENTAGE, SHARES }

/** Map the on-disk category code → narrative label + selected fill. */
private val CHAPTER_CATEGORIES: List<com.hativ2.ui.components.CategoryChipItem>
    @Composable get() = listOf(
        com.hativ2.ui.components.CategoryChipItem("food",          stringResource(R.string.category_food),          "🍽️", AccentCategoryFood),
        com.hativ2.ui.components.CategoryChipItem("transport",     stringResource(R.string.category_transport),     "🚗", com.hativ2.ui.theme.NotionBlue),
        com.hativ2.ui.components.CategoryChipItem("entertainment", stringResource(R.string.category_entertainment), "🎬", com.hativ2.ui.theme.NotionPurple),
        com.hativ2.ui.components.CategoryChipItem("utilities",     stringResource(R.string.category_utilities),     "💡", NotionGreen),
        com.hativ2.ui.components.CategoryChipItem("shopping",      stringResource(R.string.category_shopping),      "🛍️", com.hativ2.ui.theme.NotionPink),
        com.hativ2.ui.components.CategoryChipItem("other",         stringResource(R.string.category_other),         "📦", com.hativ2.ui.theme.NotionGray),
    )

@Composable
fun AddExpenseScreen(
    dashboardId: String,
    expenseId: String? = null,
    viewModel: MainViewModel,
    onBackClick: () -> Unit,
) {
    val context = LocalContext.current

    val dashboards by viewModel.dashboards.collectAsState()
    val dashboard = dashboards.find { it.id == dashboardId }

    var description by rememberSaveable { mutableStateOf("") }
    var amount by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("food") }
    var splitType by rememberSaveable { mutableStateOf(SplitType.EQUAL) }
    var note by rememberSaveable { mutableStateOf("") }
    var receiptPath by rememberSaveable { mutableStateOf<String?>(null) }

    val people by viewModel.getPeople(dashboardId).collectAsState()

    var paidBy by rememberSaveable { mutableStateOf("user-current") }
    val splitWith = remember { mutableStateListOf<String>() }
    val splitPercentages = remember { mutableStateMapOf<String, String>() }
    val splitExactAmounts = remember { mutableStateMapOf<String, String>() }
    val splitShares = remember { mutableStateMapOf<String, String>() }

    var hasInitializedSplits by rememberSaveable { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showPaidByDropdown by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val onToggleSplit = remember<(String) -> Unit> {
        { personId -> if (splitWith.contains(personId)) splitWith.remove(personId) else splitWith.add(personId) }
    }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            // Copy the picked image into our app sandbox so the URI
            // remains readable across reboots and permission grants.
            val receiptsDir = File(context.filesDir, "receipts").apply { mkdirs() }
            val target = File(receiptsDir, "${UUID.randomUUID()}.jpg")
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    target.outputStream().use { output -> input.copyTo(output) }
                }
                receiptPath = target.absolutePath
            }
        }
    }

    LaunchedEffect(expenseId) {
        if (expenseId != null) {
            val expense = viewModel.getExpenseById(expenseId)
            if (expense != null) {
                isEditing = true
                description = expense.description
                amount = expense.amount.toString()
                paidBy = expense.paidBy ?: "user-current"
                category = expense.category
                note = expense.note.orEmpty()
                receiptPath = expense.receiptPath

                val splits = viewModel.getSplitsForExpense(expenseId)
                splitWith.clear()
                splitWith.addAll(splits.map { it.personId })
            }
        }
    }

    LaunchedEffect(people) {
        if (!isEditing && !hasInitializedSplits) {
            if (!splitWith.contains("user-current")) splitWith.add("user-current")
            people.forEach { p -> if (!splitWith.contains(p.id)) splitWith.add(p.id) }
            if (people.isNotEmpty()) hasInitializedSplits = true
        }
    }

    if (showDeleteDialog) {
        MangaDeleteDialog(
            title = stringResource(R.string.chapter_delete_title),
            text = stringResource(R.string.chapter_delete_body),
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

    val descError = stringResource(R.string.chapter_validation_description)
    val amountError = stringResource(R.string.chapter_validation_amount)
    val splitError = stringResource(R.string.chapter_validation_split)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            MangaTopBar(
                title = if (isEditing) stringResource(R.string.chapter_form_title_edit)
                else stringResource(R.string.chapter_form_title_new),
                subtitle = dashboard?.title?.let { stringResource(R.string.chapter_form_subtitle, it) },
                left = {
                    MangaIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        onClick = onBackClick,
                        contentDescription = stringResource(R.string.cta_back)
                    )
                },
                right = {
                    if (isEditing) {
                        MangaIconButton(
                            icon = Icons.Default.Delete,
                            onClick = { showDeleteDialog = true },
                            contentDescription = stringResource(R.string.cta_delete),
                            tint = NotionRed
                        )
                    }
                }
            )
        },
        bottomBar = {
            // Sticky bottom CTA. Full-width, dark fill, disabled gray
            // until form validation passes.
            val amountVal = amount.toDoubleOrNull() ?: 0.0
            val formValid = description.isNotBlank() && amountVal > 0 && splitWith.isNotEmpty()

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(16.dp)
            ) {
                MangaButton(
                    onClick = {
                        if (description.isBlank()) {
                            scope.launch { snackbarHostState.showSnackbar(descError) }
                            return@MangaButton
                        }
                        if (amountVal <= 0) {
                            scope.launch { snackbarHostState.showSnackbar(amountError) }
                            return@MangaButton
                        }
                        if (splitWith.isEmpty()) {
                            scope.launch { snackbarHostState.showSnackbar(splitError) }
                            return@MangaButton
                        }
                        if (splitType == SplitType.PERCENTAGE) {
                            val total = splitWith.sumOf { splitPercentages[it]?.toDoubleOrNull() ?: 0.0 }
                            if (kotlin.math.abs(total - 100.0) > 0.1) {
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        context.getString(R.string.chapter_validation_total_percent, "%.1f".format(total))
                                    )
                                }
                                return@MangaButton
                            }
                        }
                        if (splitType == SplitType.EXACT) {
                            val total = splitWith.sumOf { splitExactAmounts[it]?.toDoubleOrNull() ?: 0.0 }
                            if (kotlin.math.abs(total - amountVal) > 0.01) {
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        context.getString(
                                            R.string.chapter_validation_total_exact,
                                            "%,.2f".format(amountVal),
                                            "%,.2f".format(amountVal - total)
                                        )
                                    )
                                }
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
                                splitWith = splitWith.toList(),
                                note = note.takeIf { it.isNotBlank() },
                                receiptPath = receiptPath
                            )
                        } else {
                            viewModel.createExpense(
                                dashboardId = dashboardId,
                                description = description,
                                amount = amountVal,
                                paidBy = paidBy,
                                category = category,
                                splitWith = splitWith.toList(),
                                note = note.takeIf { it.isNotBlank() },
                                receiptPath = receiptPath
                            )
                        }
                        onBackClick()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = if (formValid) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
                    contentColor = MaterialTheme.colorScheme.surface,
                    enabled = formValid,
                ) {
                    Text(
                        text = if (isEditing) stringResource(R.string.chapter_cta_update)
                        else stringResource(R.string.chapter_cta_add),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .widthIn(max = 600.dp)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ── EXPENSE DETAILS card ─────────────────────────────
            MangaCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SectionLabel(stringResource(R.string.chapter_section_expense_details))
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MangaBlack.copy(alpha = 0.15f)))
                    Spacer(modifier = Modifier.height(16.dp))

                    MangaTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = stringResource(R.string.chapter_field_description),
                        placeholder = stringResource(R.string.chapter_field_description_hint),
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Amount — hero focal pill (cream tint) per cinematography.
                    Column {
                        SectionLabel(stringResource(R.string.chapter_field_amount))
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SurfaceHero, RoundedCornerShape(MangaCornerRadius))
                                .border(MangaBorderWidth, MangaBlack, RoundedCornerShape(MangaCornerRadius))
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "₱",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = MangaBlack
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                BasicTextField(
                                    value = amount,
                                    onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                                    textStyle = MaterialTheme.typography.headlineSmall.copy(
                                        color = MangaBlack,
                                        fontWeight = FontWeight.Black
                                    ),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    cursorBrush = SolidColor(MangaBlack),
                                    modifier = Modifier.fillMaxWidth(),
                                    decorationBox = { inner ->
                                        if (amount.isEmpty()) {
                                            Text(
                                                text = stringResource(R.string.chapter_field_amount_hint),
                                                style = MaterialTheme.typography.headlineSmall,
                                                color = NotionMuted
                                            )
                                        }
                                        inner()
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Paid By
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SectionLabel(stringResource(R.string.chapter_field_paid_by))
                        Spacer(modifier = Modifier.weight(1f))
                        Box {
                            Row(
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(MangaCornerRadius))
                                    .border(MangaBorderWidth, MaterialTheme.colorScheme.onSurface, RoundedCornerShape(MangaCornerRadius))
                                    .clickable { showPaidByDropdown = true }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (paidBy == "user-current") stringResource(R.string.chapter_field_paid_by_you)
                                    else people.find { it.id == paidBy }?.name ?: "—",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(Icons.Default.KeyboardArrowDown, null, tint = MaterialTheme.colorScheme.onSurface)
                            }
                            DropdownMenu(
                                expanded = showPaidByDropdown,
                                onDismissRequest = { showPaidByDropdown = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.chapter_field_paid_by_you)) },
                                    onClick = { paidBy = "user-current"; showPaidByDropdown = false },
                                    leadingIcon = {
                                        if (paidBy == "user-current") Icon(Icons.Default.Check, null, tint = NotionGreen)
                                    }
                                )
                                people.filter { it.id != "user-current" }.forEach { person ->
                                    DropdownMenuItem(
                                        text = { Text(person.name) },
                                        onClick = { paidBy = person.id; showPaidByDropdown = false },
                                        leadingIcon = {
                                            if (paidBy == person.id) Icon(Icons.Default.Check, null, tint = NotionGreen)
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Category (3-per-row grid)
                    SectionLabel(stringResource(R.string.chapter_field_category), centered = true)
                    Spacer(modifier = Modifier.height(8.dp))
                    MangaCategoryChipGrid(
                        categories = CHAPTER_CATEGORIES,
                        selected = category,
                        onSelect = { category = it }
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Note
                    MangaTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = stringResource(R.string.chapter_field_note),
                        placeholder = stringResource(R.string.chapter_field_note_hint),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Receipt
                    SectionLabel(stringResource(R.string.chapter_field_receipt))
                    Spacer(modifier = Modifier.height(6.dp))
                    if (receiptPath != null) {
                        MangaDashedCard(onClick = { photoPicker.launch("image/*") }) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.chapter_field_receipt_attached),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(
                                    Icons.Default.Clear,
                                    contentDescription = stringResource(R.string.chapter_field_receipt_remove),
                                    tint = NotionRed,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clickable { receiptPath = null }
                                )
                            }
                        }
                    } else {
                        MangaDashedCard(onClick = { photoPicker.launch("image/*") }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.chapter_field_add_photo),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // ── SPLIT BETWEEN card ───────────────────────────────
            MangaCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SectionLabel(
                        text = "👥  ${stringResource(R.string.chapter_section_split_between)}",
                        centered = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MangaBlack.copy(alpha = 0.15f)))
                    Spacer(modifier = Modifier.height(16.dp))

                    MangaSegmentedControl(
                        options = listOf(
                            SplitType.EQUAL      to stringResource(R.string.chapter_split_equal),
                            SplitType.EXACT      to stringResource(R.string.chapter_split_exact),
                            SplitType.PERCENTAGE to stringResource(R.string.chapter_split_percent),
                            SplitType.SHARES     to stringResource(R.string.chapter_split_shares),
                        ),
                        selected = splitType,
                        onSelect = { splitType = it }
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    val areAllSelected = remember(splitWith.size, people.size) {
                        val userCurrent = splitWith.contains("user-current")
                        val others = people.all { splitWith.contains(it.id) }
                        userCurrent && others
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = if (areAllSelected) stringResource(R.string.chapter_deselect_all)
                            else stringResource(R.string.chapter_select_all),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = NotionBlue,
                            modifier = Modifier.clickable {
                                if (areAllSelected) {
                                    splitWith.clear()
                                } else {
                                    splitWith.clear()
                                    splitWith.add("user-current")
                                    people.forEach { if (it.id != "user-current") splitWith.add(it.id) }
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    SplitMemberRow(
                        personId = "user-current",
                        name = stringResource(R.string.chapter_field_paid_by_you),
                        isSelected = splitWith.contains("user-current"),
                        onToggle = onToggleSplit,
                        splitType = splitType,
                        amount = amount,
                        splitWithSize = splitWith.size,
                        percentage = splitPercentages["user-current"] ?: "",
                        onPercentageChange = { _, v -> splitPercentages["user-current"] = v },
                        exactAmount = splitExactAmounts["user-current"] ?: "",
                        onExactAmountChange = { _, v -> splitExactAmounts["user-current"] = v },
                        shares = splitShares["user-current"] ?: "",
                        onSharesChange = { _, v -> splitShares["user-current"] = v },
                    )

                    people.filter { it.id != "user-current" }.forEach { person ->
                        key(person.id) {
                            Spacer(modifier = Modifier.height(6.dp))
                            SplitMemberRow(
                                personId = person.id,
                                name = person.name,
                                isSelected = splitWith.contains(person.id),
                                onToggle = onToggleSplit,
                                splitType = splitType,
                                amount = amount,
                                splitWithSize = splitWith.size,
                                percentage = splitPercentages[person.id] ?: "",
                                onPercentageChange = { id, v -> splitPercentages[id] = v },
                                exactAmount = splitExactAmounts[person.id] ?: "",
                                onExactAmountChange = { id, v -> splitExactAmounts[id] = v },
                                shares = splitShares[person.id] ?: "",
                                onSharesChange = { id, v -> splitShares[id] = v },
                            )
                        }
                    }

                    // Preview block (split-type aware)
                    val amountValue by remember(amount) { derivedStateOf { amount.toDoubleOrNull() ?: 0.0 } }
                    val totalPercent by remember(splitPercentages, splitWith) {
                        derivedStateOf {
                            if (splitType == SplitType.PERCENTAGE) {
                                splitWith.sumOf { splitPercentages[it]?.toDoubleOrNull() ?: 0.0 }
                            } else 0.0
                        }
                    }
                    val totalShares by remember(splitShares, splitWith) {
                        derivedStateOf {
                            if (splitType == SplitType.SHARES) {
                                splitWith.sumOf { splitShares[it]?.toDoubleOrNull() ?: 0.0 }
                            } else 0.0
                        }
                    }

                    if (splitType == SplitType.PERCENTAGE) {
                        Spacer(modifier = Modifier.height(16.dp))
                        val invalid = kotlin.math.abs(totalPercent - 100.0) > 0.1
                        SplitPreviewPill(
                            label = "Total Percentage",
                            value = "${"%.1f".format(totalPercent)}%",
                            color = if (invalid) NotionRed else NotionGreen,
                            hint = if (invalid) "Must be 100%" else null
                        )
                    }

                    if (splitType == SplitType.EQUAL && amountValue > 0 && splitWith.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        SplitPreviewPill(
                            label = stringResource(
                                R.string.chapter_preview_each_pays,
                                "%,.2f".format(amountValue / splitWith.size)
                            ),
                            value = "",
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            hint = null
                        )
                    }

                    if (splitType == SplitType.SHARES && amountValue > 0 && totalShares > 0) {
                        Spacer(modifier = Modifier.height(16.dp))
                        SplitPreviewPill(
                            label = stringResource(
                                R.string.chapter_preview_each_share,
                                "%,.2f".format(amountValue / totalShares)
                            ),
                            value = "",
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            hint = null
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionLabel(text: String, centered: Boolean = false) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Black,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = if (centered) Modifier.fillMaxWidth() else Modifier,
        textAlign = if (centered) TextAlign.Center else TextAlign.Start
    )
}

@Composable
private fun SplitPreviewPill(
    label: String,
    value: String,
    color: Color,
    hint: String?,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(MangaCornerRadius))
            .border(MangaBorderWidth, color, RoundedCornerShape(MangaCornerRadius))
            .padding(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (value.isNotBlank()) {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                }
            }
            if (hint != null) {
                Text(
                    text = hint,
                    style = MaterialTheme.typography.labelSmall,
                    color = color,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun SplitMemberRow(
    personId: String,
    name: String,
    isSelected: Boolean,
    onToggle: (String) -> Unit,
    splitType: SplitType,
    amount: String,
    splitWithSize: Int,
    percentage: String,
    onPercentageChange: (String, String) -> Unit,
    exactAmount: String,
    onExactAmountChange: (String, String) -> Unit,
    shares: String,
    onSharesChange: (String, String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(personId) }
            .background(
                if (isSelected) NotionGreen.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surface,
                RoundedCornerShape(MangaCornerRadius)
            )
            .border(
                MangaBorderWidth,
                if (isSelected) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                RoundedCornerShape(MangaCornerRadius)
            )
            .padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            when {
                isSelected && splitType == SplitType.EQUAL -> {
                    val each = (amount.toDoubleOrNull() ?: 0.0) / splitWithSize.coerceAtLeast(1)
                    Text(
                        text = "₱${"%,.2f".format(each)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = NotionMuted
                    )
                }
                isSelected && splitType == SplitType.PERCENTAGE -> {
                    InlineMiniField(
                        value = percentage,
                        onChange = { onPercentageChange(personId, it) },
                        suffix = "%",
                        keyboardType = KeyboardType.Number,
                        width = 60.dp
                    )
                }
                isSelected && splitType == SplitType.EXACT -> {
                    InlineMiniField(
                        value = exactAmount,
                        onChange = { onExactAmountChange(personId, it) },
                        prefix = "₱",
                        keyboardType = KeyboardType.Decimal,
                        width = 80.dp
                    )
                }
                isSelected && splitType == SplitType.SHARES -> {
                    InlineMiniField(
                        value = shares,
                        onChange = { onSharesChange(personId, it) },
                        suffix = "×",
                        keyboardType = KeyboardType.Number,
                        width = 60.dp
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            CheckBoxIndicator(checked = isSelected)
        }
    }
}

@Composable
private fun InlineMiniField(
    value: String,
    onChange: (String) -> Unit,
    keyboardType: KeyboardType,
    prefix: String? = null,
    suffix: String? = null,
    width: androidx.compose.ui.unit.Dp,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (prefix != null) {
            Text(prefix, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.width(4.dp))
        }
        BasicTextField(
            value = value,
            onValueChange = onChange,
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                textAlign = TextAlign.End,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            ),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            singleLine = true,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
            modifier = Modifier
                .width(width)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(MangaCornerRadius))
                .border(1.dp, MaterialTheme.colorScheme.onSurface, RoundedCornerShape(MangaCornerRadius))
                .padding(horizontal = 8.dp, vertical = 6.dp)
        )
        if (suffix != null) {
            Spacer(modifier = Modifier.width(4.dp))
            Text(suffix, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun CheckBoxIndicator(checked: Boolean) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .background(
                if (checked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surface,
                RoundedCornerShape(MangaCornerRadius)
            )
            .border(MangaBorderWidth, MaterialTheme.colorScheme.onSurface, RoundedCornerShape(MangaCornerRadius)),
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.surface,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
