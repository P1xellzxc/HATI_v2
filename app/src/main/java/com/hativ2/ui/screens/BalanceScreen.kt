package com.hativ2.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp // Using Up as generic chart icon replacement if needed
import androidx.compose.material.icons.filled.DateRange // Reusing standard icons
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hativ2.ui.MainViewModel
import com.hativ2.ui.theme.MangaBlack
import com.hativ2.ui.theme.NotionGreen
import com.hativ2.ui.theme.NotionWhite
import com.hativ2.ui.theme.NotionYellow
import com.hativ2.ui.theme.NotionPurple
import com.hativ2.ui.theme.NotionGray
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BalanceScreen(
    dashboardId: String,
    viewModel: MainViewModel,
    onBackClick: () -> Unit
) {
    val expenses by viewModel.getExpenses(dashboardId).collectAsState()
    val dashboards by viewModel.dashboards.collectAsState()
    val dashboard = dashboards.find { it.id == dashboardId }

    // Logic: Filter last 6 months
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.MONTH, -6)
    val sixMonthsAgo = calendar.timeInMillis
    
    val filteredExpenses = expenses.filter { it.createdAt >= sixMonthsAgo }
    val totalSpending = filteredExpenses.sumOf { it.amount }

    // Monthly Data
    val monthlySpending = remember(filteredExpenses) {
        filteredExpenses.groupBy { 
            val date = Date(it.createdAt)
            val cal = Calendar.getInstance()
            cal.time = date
            "${cal.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault())}" // Key: "Oct"
        }.mapValues { entry -> entry.value.sumOf { it.amount } }
    }
    
    // Order months (simple logic for display, just taking last 6 months keys from calendar would be better for stable x-axis)
    // For now, let's just map the last 6 months explicitly to ensure empty months show up
    val last6Months = (0..5).map { i ->
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, - (5-i))
        cal.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault())!!
    }

    Scaffold(
        containerColor = NotionWhite,
        topBar = {
            Column(modifier = Modifier.background(NotionWhite)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                         IconButton(onClick = onBackClick) {
                             Box(
                                 modifier = Modifier
                                     .size(40.dp)
                                     .border(2.dp, MangaBlack, RoundedCornerShape(2.dp))
                                     .background(NotionWhite),
                                 contentAlignment = Alignment.Center
                             ) {
                                 Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MangaBlack)
                             }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("ARC STATISTICS", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                            Text(dashboard?.title?.uppercase() ?: "TEST FOLDER", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        }
                    }
                    
                    // Dropdown
                    Box(
                        modifier = Modifier
                            .border(2.dp, MangaBlack, RoundedCornerShape(2.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Last 6 Months", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
                Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(MangaBlack))
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
            // Total Arc Spending
            item {
                StatCard(title = "") {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Total Arc Spending", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(16.dp))
                        Box(
                            modifier = Modifier
                                .background(NotionYellow.copy(alpha=0.5f))
                                .border(3.dp, MangaBlack)
                                .padding(horizontal = 32.dp, vertical = 12.dp)
                        ) {
                            Text(
                                "₱${String.format("%.2f", totalSpending)}", 
                                fontSize = 32.sp, 
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp,
                                color = MangaBlack // Ensure text is visible
                            )
                        }
                    }
                }
            }

            // Category Breakdown
            item {
                StatCard(title = "CATEGORY BREAKDOWN", icon = Icons.Default.DateRange) { // Using DateRange/Pie icon equivalent
                     if (filteredExpenses.isEmpty()) {
                         Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                             Text("No chapters to display", color = Color.Gray, fontWeight = FontWeight.SemiBold)
                         }
                     } else {
                         // Simple Category List
                        Column(modifier = Modifier.padding(16.dp)) {
                             filteredExpenses.groupBy { it.category }.forEach { (cat, list) ->
                                 Row(
                                     modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), 
                                     horizontalArrangement = Arrangement.SpaceBetween
                                 ) {
                                     Text(cat.replaceFirstChar { it.uppercase() }, fontWeight = FontWeight.Bold)
                                     Text("₱${String.format("%.2f", list.sumOf { it.amount })}", fontWeight = FontWeight.Bold)
                                 }
                             }
                        }
                     }
                }
            }

            // Monthly Trend
            item {
                StatCard(title = "MONTHLY TREND", icon = Icons.AutoMirrored.Filled.ArrowBack) { // Using generic icon
                     Box(modifier = Modifier.fillMaxWidth().height(300.dp).padding(16.dp)) {
                         MonthlyTrendChart(monthlyData = monthlySpending, months = last6Months)
                     }
                }
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    content: @Composable () -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        // Hard Shadow
        Box(
             modifier = Modifier.matchParentSize().offset(x = 6.dp, y = 6.dp).background(MangaBlack)
        )
        // Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(NotionWhite)
                .border(4.dp, MangaBlack)
        ) {
            Column {
                if (title.isNotEmpty()) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (icon != null) {
                             // Rotate arrow for trend if needed, or just standard icon
                             Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp)) 
                             Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 1.sp)
                    }
                    Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(MangaBlack))
                }
                
                content()
            }
        }
    }
}

@Composable
fun MonthlyTrendChart(
    monthlyData: Map<String, Double>,
    months: List<String>
) {
    val maxValue = (monthlyData.values.maxOrNull() ?: 100.0).coerceAtLeast(100.0)
    
    Canvas(modifier = Modifier.fillMaxSize()) {
        val barWidth = size.width / (months.size * 2f)
        val spacing = size.width / months.size
        val bottomY = size.height - 40.dp.toPx()
        val graphHeight = size.height - 60.dp.toPx() // Reserve top space
        
        // Axis Lines
        drawLine(
            color = MangaBlack,
            start = Offset(40.dp.toPx(), 10.dp.toPx()),
            end = Offset(40.dp.toPx(), bottomY),
            strokeWidth = 3.dp.toPx()
        )
        drawLine(
            color = MangaBlack,
            start = Offset(40.dp.toPx(), bottomY),
            end = Offset(size.width, bottomY),
            strokeWidth = 3.dp.toPx()
        )

        // Draw Y Labels (P0, P1, P2.. scaled)
        val steps = 4
        for (i in 0..steps) {
            val y = bottomY - (graphHeight * i / steps)
            val labelValue = (maxValue * i / steps).toInt()
            
            drawContext.canvas.nativeCanvas.drawText(
                "P${labelValue/1000}k", // Simplified formatting
                10.dp.toPx(),
                y,
                android.graphics.Paint().apply {
                    color = android.graphics.Color.BLACK
                    textSize = 30f
                    textAlign = android.graphics.Paint.Align.LEFT
                }
            )
        }

        // Draw Bars
        months.forEachIndexed { index, month ->
            val startX = 60.dp.toPx() + (index * spacing)
            val amount = monthlyData[month] ?: 0.0
            val barHeight = (amount / maxValue * graphHeight).toFloat()
            
            // Bar
            drawRect(
                color = NotionGray, // Light gray bar
                topLeft = Offset(startX, bottomY - barHeight),
                size = Size(barWidth, barHeight)
            )
            
            // X Label
            drawContext.canvas.nativeCanvas.drawText(
                month,
                startX + barWidth / 2,
                size.height,
                android.graphics.Paint().apply {
                    color = android.graphics.Color.BLACK
                    textSize = 35f
                    textAlign = android.graphics.Paint.Align.CENTER
                }
            )
            
            // Tooltip like box for active month (simulated for last month or max)
            if (amount > 0 && amount == monthlyData.values.maxOrNull()) {
                 // Draw simple tooltip box
                 val tooltipX = startX + barWidth + 10f
                 val tooltipY = bottomY - barHeight + 20f
                 
                 drawRect(
                     color = NotionWhite,
                     topLeft = Offset(tooltipX, tooltipY),
                     size = Size(200f, 100f),
                 )
                 drawRect(
                     color = MangaBlack,
                     topLeft = Offset(tooltipX, tooltipY),
                     size = Size(200f, 100f),
                     style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
                 )
                 
                 drawContext.canvas.nativeCanvas.drawText(
                    month,
                    tooltipX + 20f,
                    tooltipY + 40f,
                    android.graphics.Paint().apply {
                        textSize = 30f
                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                    }
                 )
                 
                 drawContext.canvas.nativeCanvas.drawText(
                    "P${amount.toInt()}",
                    tooltipX + 20f,
                    tooltipY + 80f,
                    android.graphics.Paint().apply {
                        textSize = 30f
                    }
                 )
            }
        }
    }
}
