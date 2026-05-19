package com.hativ2.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hativ2.R
import com.hativ2.ui.MainViewModel
import com.hativ2.ui.TransactionDisplayItem
import com.hativ2.ui.components.EditDashboardDialog
import com.hativ2.ui.components.ExportWarningDialog
import com.hativ2.ui.components.MangaBorderWidth
import com.hativ2.ui.components.MangaButton
import com.hativ2.ui.components.MangaCard
import com.hativ2.ui.components.MangaCornerRadius
import com.hativ2.ui.components.MangaIconButton
import com.hativ2.ui.components.MangaTextField
import com.hativ2.ui.components.MangaTopBar
import com.hativ2.ui.components.SettleUpDialog
import com.hativ2.ui.components.TransactionCard
import com.hativ2.ui.theme.AccentDebit
import com.hativ2.ui.theme.AccentPositive
import com.hativ2.ui.theme.MangaBlack
import com.hativ2.ui.theme.NotionBlue
import com.hativ2.ui.theme.NotionDivider
import com.hativ2.ui.theme.NotionGreen
import com.hativ2.ui.theme.NotionMuted
import com.hativ2.ui.theme.NotionRed
import com.hativ2.ui.theme.NotionYellow
import com.hativ2.ui.theme.SurfaceHero
import kotlinx.coroutines.launch

@Composable
fun DashboardDetailScreen(
    dashboardId: String,
    viewModel: MainViewModel,
    onBackClick: () -> Unit,
    onAddExpenseClick: (String) -> Unit,
    onBalanceClick: (String) -> Unit,
    onViewExpensesClick: (String) -> Unit,
    onExpenseClick: (String) -> Unit,
) {
    val dashboards by viewModel.dashboards.collectAsState()
    val dashboard = dashboards.find { it.id == dashboardId }

    val debtSummaryFlow = remember(dashboardId) { viewModel.getDebtSummary(dashboardId) }
    val debtSummary by debtSummaryFlow.collectAsState()

    val transactionsFlow = remember(dashboardId) { viewModel.getTransactions(dashboardId) }
    val transactions by transactionsFlow.collectAsState()

    val expensesFlow = remember(dashboardId) { viewModel.getExpenses(dashboardId) }
    val expenses by expensesFlow.collectAsState()

    val peopleFlow = remember(dashboardId) { viewModel.getPeople(dashboardId) }
    val people by peopleFlow.collectAsState()

    val context = LocalContext.current

    val csvExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri -> if (uri != null) viewModel.exportCsv(dashboardId, uri, context) }
    val jsonExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> if (uri != null) viewModel.exportJson(dashboardId, uri, context) }

    var showAddPersonDialog by remember { mutableStateOf(false) }
    var showEditDashboardDialog by remember { mutableStateOf(false) }
    var showSettleUpDialog by remember { mutableStateOf(false) }
    var showExportWarning by remember { mutableStateOf(false) }
    var settleUpFromId by remember { mutableStateOf<String?>(null) }
    var settleUpToId by remember { mutableStateOf<String?>(null) }
    var settleUpAmount by remember { mutableStateOf(0.0) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val nameMap = remember(people) {
        buildMap {
            put("user-current", "You")
            people.forEach { put(it.id, it.name) }
        }
    }
    val dateFormat = remember { java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault()) }

    if (dashboard == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.dash_volume_not_found))
        }
        return
    }

    if (showExportWarning) {
        ExportWarningDialog(
            onConfirm = { format ->
                showExportWarning = false
                val filename = "dashboard_${dashboard.title}_export.${format.extension}"
                when (format) {
                    com.hativ2.ui.components.ExportFormat.CSV  -> csvExportLauncher.launch(filename)
                    com.hativ2.ui.components.ExportFormat.JSON -> jsonExportLauncher.launch(filename)
                }
            },
            onDismiss = { showExportWarning = false }
        )
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
            scope.launch { snackbarHostState.showSnackbar("Settled up ₱${"%,.2f".format(amount)}!") }
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
            MangaTopBar(
                title = dashboard.title.uppercase(),
                subtitle = stringResource(
                    R.string.dash_arc_suffix,
                    dashboard.dashboardType.replaceFirstChar { it.uppercase() }
                ),
                left = {
                    MangaIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        onClick = onBackClick,
                        contentDescription = stringResource(R.string.cta_back)
                    )
                },
                right = {
                    MangaIconButton(
                        icon = Icons.Default.Share,
                        onClick = { showExportWarning = true },
                        contentDescription = stringResource(R.string.action_export)
                    )
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .widthIn(max = 600.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ── Quick action cards row ───────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionCard(
                        modifier = Modifier.weight(1f),
                        title = stringResource(R.string.dash_action_history),
                        icon = Icons.Default.List,
                        onClick = { onViewExpensesClick(dashboardId) }
                    )
                    QuickActionCard(
                        modifier = Modifier.weight(1f),
                        title = stringResource(R.string.dash_action_charts),
                        icon = Icons.Default.DateRange,
                        onClick = { onBalanceClick(dashboardId) }
                    )
                    QuickActionCard(
                        modifier = Modifier.weight(1f),
                        title = stringResource(R.string.dash_action_member),
                        icon = Icons.Default.Person,
                        onClick = { showAddPersonDialog = true }
                    )
                }
            }

            // ── Balance Overview ─────────────────────────────
            item {
                var visible by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) { visible = true }
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(400)) + slideInVertically(
                        animationSpec = tween(400, easing = FastOutSlowInEasing),
                        initialOffsetY = { it / 3 }
                    )
                ) {
                    BalanceOverviewCard(
                        title = dashboard.title,
                        balance = debtSummary.balances["user-current"] ?: 0.0,
                        totalSpent = expenses.sumOf { it.amount }
                    )
                }
            }

            if (expenses.isNotEmpty()) {
                item {
                    var visible by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) { visible = true }
                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(tween(400, delayMillis = 200)) + slideInVertically(
                            animationSpec = tween(400, delayMillis = 200, easing = FastOutSlowInEasing),
                            initialOffsetY = { it / 3 }
                        )
                    ) {
                        SpendingByMemberCard(
                            memberShares = debtSummary.memberShares,
                            balances = debtSummary.balances,
                            people = people
                        )
                    }
                }
            } else {
                item {
                    com.hativ2.ui.components.MangaEmptyState(
                        message = stringResource(R.string.dash_no_chapters_yet),
                        subMessage = stringResource(R.string.dash_no_chapters_yet_sub),
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            }

            // ── OWED TO YOU / YOU OWE row ────────────────────
            item {
                val owedToYouDetails = debtSummary.transactions
                    .filter { it.toId == "user-current" }
                    .map { tx ->
                        val name = nameMap[tx.fromId]?.split(" ")?.firstOrNull() ?: "—"
                        name to tx.amount
                    }
                val youOweDetails = debtSummary.transactions
                    .filter { it.fromId == "user-current" }
                    .map { tx ->
                        val name = nameMap[tx.toId]?.split(" ")?.firstOrNull() ?: "—"
                        name to tx.amount
                    }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SummaryCard(
                        modifier = Modifier.weight(1f),
                        title = stringResource(R.string.dash_section_owed_to_you),
                        amount = debtSummary.totalOwedToYou,
                        details = owedToYouDetails,
                        headerColor = AccentPositive.copy(alpha = 0.35f),
                        emptyText = stringResource(R.string.dash_no_one_owes_you),
                        amountColor = AccentPositive,
                        arrow = "↙",
                        onSettleUpClick = if (debtSummary.totalOwedToYou > 0) {
                            {
                                val owed = debtSummary.transactions.filter { it.toId == "user-current" }
                                if (owed.size == 1) {
                                    settleUpFromId = owed.first().fromId
                                    settleUpToId = "user-current"
                                    settleUpAmount = owed.first().amount
                                } else {
                                    settleUpFromId = null
                                    settleUpToId = "user-current"
                                    settleUpAmount = 0.0
                                }
                                showSettleUpDialog = true
                            }
                        } else null
                    )
                    SummaryCard(
                        modifier = Modifier.weight(1f),
                        title = stringResource(R.string.dash_section_you_owe),
                        amount = debtSummary.totalYouOwe,
                        details = youOweDetails,
                        headerColor = AccentDebit.copy(alpha = 0.45f),
                        emptyText = stringResource(R.string.dash_you_dont_owe),
                        amountColor = NotionRed,
                        arrow = "↗",
                        onSettleUpClick = if (debtSummary.totalYouOwe > 0) {
                            {
                                val youOwe = debtSummary.youOwe
                                if (youOwe.size == 1) {
                                    settleUpToId = youOwe.first().toId
                                    settleUpAmount = youOwe.first().amount
                                } else {
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

            // ── RECENT CHAPTERS header ───────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.List, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.dash_section_recent_chapters),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Black
                        )
                    }
                    if (expenses.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .border(MangaBorderWidth, MangaBlack, RoundedCornerShape(MangaCornerRadius))
                                .clickable { onViewExpensesClick(dashboardId) }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                stringResource(R.string.action_view_all),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            if (transactions.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("👋", style = MaterialTheme.typography.displayLarge)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Start by adding people to split with",
                            style = MaterialTheme.typography.bodyMedium,
                            color = NotionMuted
                        )
                    }
                }
            } else {
                items(transactions.take(5), key = { it.id }) { item ->
                    when (item) {
                        is TransactionDisplayItem.ExpenseItem -> {
                            val expense = item.expense
                            val isPayer = expense.paidBy == "user-current"
                            val payerName = nameMap[expense.paidBy] ?: "—"
                            TransactionCard(
                                title = expense.description,
                                subtitle = "paid by $payerName",
                                amount = "₱${"%,.2f".format(expense.amount)}",
                                amountColor = if (isPayer) NotionGreen else MangaBlack,
                                date = dateFormat.format(java.util.Date(expense.createdAt)),
                                avatarText = payerName,
                                avatarColor = if (isPayer) "default" else "white",
                                onClick = { onExpenseClick(expense.id) }
                            )
                        }
                        is TransactionDisplayItem.SettlementItem -> {
                            val settlement = item.settlement
                            val fromName = nameMap[settlement.fromId] ?: "—"
                            val toName = nameMap[settlement.toId] ?: "—"
                            TransactionCard(
                                title = "Settlement",
                                subtitle = "$fromName → $toName",
                                amount = "₱${"%,.2f".format(settlement.amount)}",
                                amountColor = NotionBlue,
                                date = dateFormat.format(java.util.Date(settlement.createdAt)),
                                icon = { Icon(Icons.Default.Check, null, tint = MangaBlack) },
                                onClick = { }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            item { Spacer(modifier = Modifier.height(72.dp)) }
        }
    }
}

@Composable
private fun QuickActionCard(
    modifier: Modifier = Modifier,
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val shadow by animateDpAsState(
        targetValue = if (isPressed) 0.dp else 4.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "qaShadow"
    )
    val pressOffset by animateDpAsState(
        targetValue = if (isPressed) 4.dp else 0.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "qaPress"
    )
    val scaleF by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "qaScale"
    )

    Box(
        modifier = modifier
            .aspectRatio(1.1f)
            .scale(scaleF)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = shadow, y = shadow)
                .background(MangaBlack, RoundedCornerShape(MangaCornerRadius))
        )
        Column(
            modifier = Modifier
                .matchParentSize()
                .offset(x = pressOffset, y = pressOffset)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(MangaCornerRadius))
                .border(MangaBorderWidth, MangaBlack, RoundedCornerShape(MangaCornerRadius)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun BalanceOverviewCard(
    title: String,
    balance: Double,
    totalSpent: Double,
) {
    MangaCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.dash_balance_label, title),
                style = MaterialTheme.typography.bodyMedium,
                color = NotionMuted
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Hero pill — cream tint (warm focal), per cinematography rule
            // of one focal point per screen.
            Box(
                modifier = Modifier
                    .background(SurfaceHero, RoundedCornerShape(MangaCornerRadius))
                    .border(MangaBorderWidth, MangaBlack, RoundedCornerShape(MangaCornerRadius))
                    .padding(horizontal = 28.dp, vertical = 12.dp)
            ) {
                val sign = if (balance >= 0) "+" else ""
                Text(
                    text = "$sign₱${"%,.2f".format(balance)}",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Black,
                    color = MangaBlack,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.dash_total_spent, "%,.2f".format(totalSpent)),
                style = MaterialTheme.typography.bodySmall,
                color = NotionMuted
            )
        }
    }
}

@Composable
fun SpendingByMemberCard(
    memberShares: Map<String, Double>,
    balances: Map<String, Double> = emptyMap(),
    people: List<com.hativ2.data.entity.PersonEntity>,
) {
    MangaCard {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ShoppingCart, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    stringResource(R.string.dash_section_spending_by_member),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Black
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MangaBlack.copy(alpha = 0.15f)))
            Spacer(modifier = Modifier.height(12.dp))

            if (memberShares.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(R.string.dash_no_spending_yet),
                        color = NotionMuted,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text("Member", modifier = Modifier.weight(1.5f), style = MaterialTheme.typography.labelSmall, color = NotionMuted, fontWeight = FontWeight.Bold)
                    Text("Paid", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = NotionMuted, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
                    Text("Share", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = NotionMuted, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
                    Text("Offset", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = NotionMuted, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(MangaBlack))
                Spacer(modifier = Modifier.height(6.dp))
                val allUserIds = (memberShares.keys + balances.keys).distinct()
                allUserIds.forEach { userId ->
                    key(userId) {
                        val name = if (userId == "user-current") "You" else people.find { it.id == userId }?.name?.split(" ")?.firstOrNull() ?: "—"
                        val paid = memberShares[userId] ?: 0.0
                        val bal = balances[userId] ?: 0.0
                        val fairShare = paid - bal
                        MemberSpendingRow(name = name, paid = paid, fairShare = fairShare, offset = bal)
                    }
                }
            }
        }
    }
}

@Composable
fun MemberSpendingRow(name: String, paid: Double, fairShare: Double, offset: Double) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(name, modifier = Modifier.weight(1.5f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
            Text("₱${"%,.0f".format(paid)}", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.End)
            Text("₱${"%,.0f".format(fairShare)}", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.End)
            Text(
                "₱${"%,.0f".format(offset)}",
                modifier = Modifier.weight(1f),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodySmall,
                color = if (offset >= 0) AccentPositive else NotionRed,
                textAlign = TextAlign.End
            )
        }
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(NotionDivider))
    }
}

@Composable
private fun SummaryCard(
    modifier: Modifier = Modifier,
    title: String,
    amount: Double,
    details: List<Pair<String, Double>>,
    headerColor: Color,
    emptyText: String,
    amountColor: Color,
    arrow: String,
    onSettleUpClick: (() -> Unit)? = null,
) {
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 4.dp, y = 4.dp)
                .background(MangaBlack, RoundedCornerShape(MangaCornerRadius))
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(MangaCornerRadius))
                .border(MangaBorderWidth, MangaBlack, RoundedCornerShape(MangaCornerRadius))
        ) {
            // Tinted header strip — AccentPositive for credit, AccentDebit for debt.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(headerColor)
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Text(arrow, style = MaterialTheme.typography.labelLarge, color = MangaBlack)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            title,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Black,
                            color = MangaBlack
                        )
                    }
                    Box(modifier = Modifier.fillMaxWidth().height(MangaBorderWidth).background(MangaBlack))
                }
            }
            Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                Text(
                    text = "₱${"%,.2f".format(amount)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = MangaBlack
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (details.isEmpty()) {
                    Text(emptyText, style = MaterialTheme.typography.bodySmall, color = NotionMuted)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        details.forEach { (name, debt) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(name, style = MaterialTheme.typography.labelSmall, color = NotionMuted, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "₱${"%,.0f".format(debt)}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = amountColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                if (onSettleUpClick != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(NotionYellow, RoundedCornerShape(MangaCornerRadius))
                            .border(1.dp, MangaBlack, RoundedCornerShape(MangaCornerRadius))
                            .clickable(onClick = onSettleUpClick)
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            stringResource(R.string.dash_cta_settle_up),
                            fontWeight = FontWeight.Black,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AddPersonDialog(onDismiss: () -> Unit, onAdd: (String) -> Unit) {
    var name by remember { mutableStateOf("") }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        MangaCard {
            Column(modifier = Modifier.padding(8.dp)) {
                Text("New Character", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
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
                    ) { Text(stringResource(R.string.cta_cancel), fontWeight = FontWeight.Bold) }
                    MangaButton(
                        onClick = { if (name.isNotBlank()) onAdd(name) },
                        backgroundColor = NotionGreen
                    ) { Text(stringResource(R.string.cta_add), fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}
