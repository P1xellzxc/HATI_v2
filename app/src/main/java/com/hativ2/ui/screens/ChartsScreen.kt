package com.hativ2.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hativ2.ui.MainViewModel
import com.hativ2.ui.theme.MangaBlack
import com.hativ2.ui.theme.NotionBlue
import com.hativ2.ui.theme.NotionGreen
import com.hativ2.ui.theme.NotionRed
import com.hativ2.ui.theme.NotionWhite
import com.hativ2.ui.theme.NotionYellow
import com.hativ2.ui.theme.NotionOrange
import com.hativ2.ui.theme.NotionPink
import com.hativ2.ui.theme.NotionPurple
import com.hativ2.ui.theme.NotionGray
import com.hativ2.ui.components.MangaCard
import com.hativ2.ui.components.MangaBackButton
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// Chart colors matching FinSplit
val CHART_COLORS = listOf(
    NotionOrange,
    NotionBlue,
    NotionPurple,
    NotionYellow,
    NotionPink,
    NotionGreen,
    NotionGray,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartsScreen(
    viewModel: MainViewModel,
    dashboardId: String?,
    onBack: () -> Unit
) {
    val dashboardsWithStats by viewModel.dashboardsWithStats.collectAsState()
    val expensesFlow = remember(dashboardId) { viewModel.getExpenses(dashboardId ?: "") }
    val expenses by expensesFlow.collectAsState()

    // Fetch debt summary and people for Offset Computation
    val debtSummaryFlow = remember(dashboardId) { viewModel.getDebtSummary(dashboardId ?: "") }
    val debtSummary by debtSummaryFlow.collectAsState()
    
    val peopleFlow = remember(dashboardId) { viewModel.getPeople(dashboardId ?: "") }
    val people by peopleFlow.collectAsState()

    val currentDashboard = dashboardsWithStats.find { it.id == dashboardId }
    
    var selectedPeriod by remember { mutableStateOf("Last 6 Months") }
    
    // Category spending data
    val categoryData = remember(expenses) {
        val totals = mutableMapOf<String, Double>()
        expenses.forEach { expense ->
            val current = totals[expense.category] ?: 0.0
            totals[expense.category] = current + expense.amount
        }
        CATEGORIES.mapNotNull { cat ->
            val amount = totals[cat.value] ?: 0.0
            if (amount > 0) CategorySpending(cat.label, amount, cat.color) else null
        }
    }
    
    // Monthly spending data
    val monthlyData = remember(expenses) {
        val calendar = Calendar.getInstance()
        val monthTotals = mutableMapOf<String, Double>()
        val dateFormat = SimpleDateFormat("MMM", Locale.getDefault())
        
        // Initialize last 6 months
        repeat(6) { i ->
            calendar.time = Date()
            calendar.add(Calendar.MONTH, -i)
            val monthKey = dateFormat.format(calendar.time)
            monthTotals[monthKey] = 0.0
        }
        
        // Aggregate expenses
        expenses.forEach { expense ->
            val expenseDate = Date(expense.createdAt)
            val monthKey = dateFormat.format(expenseDate)
            if (monthTotals.containsKey(monthKey)) {
                monthTotals[monthKey] = (monthTotals[monthKey] ?: 0.0) + expense.amount
            }
        }
        
        monthTotals.entries.toList().reversed()
    }
    
    val totalSpending = expenses.sumOf { it.amount }

    // Average monthly spending (for the average line on the bar chart)
    val averageMonthlySpending = remember(monthlyData) {
        val nonZeroMonths = monthlyData.filter { it.value > 0 }
        if (nonZeroMonths.isEmpty()) 0.0 else nonZeroMonths.sumOf { it.value } / nonZeroMonths.size
    }

    // Month-over-month change: compare current month to previous month
    val monthOverMonthChange = remember(monthlyData) {
        if (monthlyData.size >= 2) {
            val currentMonth = monthlyData.last().value
            val previousMonth = monthlyData[monthlyData.size - 2].value
            if (previousMonth > 0) {
                ((currentMonth - previousMonth) / previousMonth) * 100.0
            } else if (currentMonth > 0) {
                100.0 // Went from 0 to something
            } else {
                0.0
            }
        } else {
            0.0
        }
    }

    // Top spending category
    val topCategory = remember(categoryData) {
        categoryData.maxByOrNull { it.amount }
    }

    Scaffold(
        containerColor = NotionWhite,
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
                            if (dashboardId == null) "All Volumes" else "Arc Statistics",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black
                        )
                        if (currentDashboard != null) {
                            Text(
                                currentDashboard.title,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                },
                actions = {
                    // Period selector
                    Box(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .border(2.dp, MangaBlack, RoundedCornerShape(2.dp))
                            .background(NotionWhite, RoundedCornerShape(2.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DateRange, null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(selectedPeriod, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NotionWhite)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Total Spending Card with Month-over-Month Indicator
            MangaCard {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Total Arc Spending", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .background(NotionYellow, RoundedCornerShape(2.dp))
                            .border(2.dp, MangaBlack, RoundedCornerShape(2.dp))
                            .padding(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        Text(
                            "₱${String.format("%,.2f", totalSpending)}",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Black
                        )
                    }

                    // Month-over-month change indicator
                    if (monthlyData.size >= 2 && monthOverMonthChange != 0.0) {
                        Spacer(modifier = Modifier.height(12.dp))
                        val isUp = monthOverMonthChange > 0
                        val arrow = if (isUp) "↑" else "↓"
                        val changeColor = if (isUp) NotionRed else NotionGreen
                        val changeLabel = if (isUp) "more" else "less"
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                "$arrow ${String.format("%.1f", kotlin.math.abs(monthOverMonthChange))}%",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = changeColor
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "$changeLabel than last month",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                        }
                    }

                    // Top spending category
                    if (topCategory != null && totalSpending > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        val topPct = (topCategory.amount / totalSpending * 100)
                        Text(
                            "Top: ${topCategory.label} (${String.format("%.0f", topPct)}%)",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                    }
                }
            }
            
            // Category Pie Chart
            MangaCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Header
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("📊", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("CATEGORY BREAKDOWN", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                    Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(MangaBlack))
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (categoryData.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No spending data yet", color = Color.Gray)
                        }
                    } else {
                        // Donut Chart
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            DonutChart(
                                data = categoryData,
                                modifier = Modifier.size(240.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Legend
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            categoryData.forEachIndexed { index, item ->
                                val percentage = if (totalSpending > 0) item.amount / totalSpending * 100 else 0.0
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(2.dp, MangaBlack, RoundedCornerShape(2.dp))
                                        .background(Color.Gray.copy(alpha = 0.1f), RoundedCornerShape(2.dp))
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .background(CHART_COLORS[index % CHART_COLORS.size], RoundedCornerShape(2.dp))
                                                .border(1.dp, MangaBlack, RoundedCornerShape(2.dp))
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(item.label, style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            "(${String.format("%.0f", percentage)}%)",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                            color = Color.Gray
                                        )
                                    }
                                    Text(
                                        "₱${String.format("%,.2f", item.amount)}",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // Monthly Bar Chart with Average Line
            MangaCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("📈", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("MONTHLY TREND", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(MangaBlack))
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (monthlyData.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No monthly data yet", color = Color.Gray)
                        }
                    } else {
                        val maxAmount = monthlyData.maxOfOrNull { it.value } ?: 1.0
                        val chartHeight = 200.dp
                        val barAreaHeight = 140f // Must match bar height calc below
                        
                        // Chart area with average line overlay
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(chartHeight)
                                .padding(top = 8.dp)
                        ) {
                            // Average spending dashed line overlay
                            if (averageMonthlySpending > 0 && maxAmount > 0) {
                                val avgRatio = (averageMonthlySpending / maxAmount).toFloat().coerceIn(0f, 1f)
                                Canvas(
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    // The bar area is approximately the middle portion.
                                    // Bars grow upward from ~24dp above bottom (month label + spacer).
                                    // Amount labels are ~16dp at top.
                                    val bottomPadding = 32.dp.toPx() // month label + spacers
                                    val topPadding = 20.dp.toPx() // amount label + spacer
                                    val drawableHeight = size.height - bottomPadding - topPadding
                                    val lineY = size.height - bottomPadding - (drawableHeight * avgRatio)
                                    
                                    // Draw dashed line
                                    val dashLength = 8.dp.toPx()
                                    val gapLength = 4.dp.toPx()
                                    var x = 0f
                                    while (x < size.width) {
                                        val endX = (x + dashLength).coerceAtMost(size.width)
                                        drawLine(
                                            color = NotionOrange,
                                            start = Offset(x, lineY),
                                            end = Offset(endX, lineY),
                                            strokeWidth = 2.dp.toPx()
                                        )
                                        x += dashLength + gapLength
                                    }
                                }
                            }
                        
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                val animatedHeightFactor = remember { androidx.compose.animation.core.Animatable(0f) }
                                LaunchedEffect(monthlyData) {
                                    animatedHeightFactor.animateTo(
                                        targetValue = 1f,
                                        animationSpec = tween(durationMillis = 1000, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                                    )
                                }
                            
                                monthlyData.forEach { (month, amount) ->
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        // Amount label
                                        Text(
                                            "₱${String.format("%,.0f", amount)}",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                            color = Color.Gray
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        
                                        // Bar
                                        val barHeight = if (maxAmount > 0) 
                                            ((amount / maxAmount) * barAreaHeight).toFloat() * animatedHeightFactor.value
                                        else 0f
                                        val height = barHeight.coerceAtLeast(4f)
                                        
                                        Box(
                                            modifier = Modifier
                                                .width(32.dp)
                                                .height(height.dp)
                                                .background(MangaBlack, RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                                                .border(2.dp, MangaBlack, RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                                        )
                                        
                                        Spacer(modifier = Modifier.height(8.dp))
                                        
                                        // Month label
                                        Text(
                                            month,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                        
                        // Average label
                        if (averageMonthlySpending > 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(16.dp)
                                        .height(2.dp)
                                        .background(NotionOrange)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "Avg: ₱${String.format("%,.0f", averageMonthlySpending)}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = NotionOrange,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Offset Computation Breakdown (Only visible for specific arc)
            if (dashboardId != null && people.isNotEmpty()) {
                MangaCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🧮", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("OFFSET COMPUTATION", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(MangaBlack))
                        Spacer(modifier = Modifier.height(16.dp))

                        // Header Row
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text("Member", modifier = Modifier.weight(1.5f), style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontWeight = FontWeight.Bold)
                            Text("Paid", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.End)
                            Text("Share", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.End)
                            Text("Offset", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.End)
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))

                        people.forEach { person ->
                            val paid = debtSummary.memberShares[person.id] ?: 0.0
                            val balance = debtSummary.balances[person.id] ?: 0.0
                            val fairShare = paid - balance // Derived from: Paid - Share = Offset  =>  Share = Paid - Offset
                            
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        person.name.split(" ").firstOrNull() ?: "Unknown", 
                                        modifier = Modifier.weight(1.5f), 
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp)
                                    )
                                    Text(
                                        "₱${String.format("%,.0f", paid)}", 
                                        modifier = Modifier.weight(1f), 
                                        style = MaterialTheme.typography.bodySmall,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.End
                                    )
                                    Text(
                                        "₱${String.format("%,.0f", fairShare)}", 
                                        modifier = Modifier.weight(1f), 
                                        style = MaterialTheme.typography.bodySmall,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.End
                                    )
                                    Text(
                                        "₱${String.format("%,.0f", balance)}", 
                                        modifier = Modifier.weight(1f), 
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (balance >= 0) NotionGreen else NotionRed,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.End
                                    )
                                }
                                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.LightGray.copy(alpha=0.5f)))
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}



data class CategorySpending(
    val label: String,
    val amount: Double,
    val color: Color
)

@Composable
fun DonutChart(
    data: List<CategorySpending>,
    modifier: Modifier = Modifier
) {
    val total = data.sumOf { it.amount }
    val animatedProgress = remember { androidx.compose.animation.core.Animatable(0f) }
    
    LaunchedEffect(data) {
        animatedProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1000, easing = androidx.compose.animation.core.FastOutSlowInEasing)
        )
    }
    
    Canvas(modifier = modifier) {
        val strokeWidth = 100f
        val radius = (size.minDimension - strokeWidth) / 2
        var startAngle = -90f
        val globalProgress = animatedProgress.value
        
        data.forEachIndexed { index, item ->
            val sweepAngle = (item.amount / total * 360).toFloat() * globalProgress
            val color = CHART_COLORS.getOrElse(index) { Color.Gray }
            
            if (sweepAngle > 0) {
                // Draw arc
                drawArc(
                    color = color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidth)
                )
                
                // Draw border
                drawArc(
                    color = Color.Black,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = 2f)
                )
                
                startAngle += sweepAngle
            }
        }
    }
}
