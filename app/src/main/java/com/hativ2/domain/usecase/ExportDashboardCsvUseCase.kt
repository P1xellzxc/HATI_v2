package com.hativ2.domain.usecase

import com.hativ2.data.dao.ExpenseDao
import com.hativ2.data.dao.DashboardDao
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class ExportDashboardCsvUseCase @Inject constructor(
    private val expenseDao: ExpenseDao,
    private val dashboardDao: DashboardDao
) {
    suspend fun execute(dashboardId: String): String {
        val expenses = expenseDao.getExpensesForDashboard(dashboardId).first()
        val people = dashboardDao.getDashboardMembers(dashboardId).first()
        val peopleMap = people.associateBy { it.id }
        
        val sb = StringBuilder()
        // Header
        sb.append("Date,Description,Category,Total Amount,Paid By,Split Details\n")
        
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        
        expenses.forEach { expense ->
            val date = dateFormat.format(Date(expense.createdAt))
            // Escape description and category for CSV safety (wrap in quotes if contains comma)
            val description = matchCsv(expense.description)
            val category = matchCsv(expense.category)
            val amount = String.format(Locale.US, "%.2f", expense.amount)
            
            val payerName = if (expense.paidBy == "user-current") "You" 
                            else peopleMap[expense.paidBy]?.name ?: "Unknown"
            val safePayer = matchCsv(payerName)

            // Get splits
            val splits = expenseDao.getSplitsForExpense(expense.id)
            val splitDetails = splits.joinToString(" | ") { split ->
                val name = if (split.personId == "user-current") "You" 
                           else peopleMap[split.personId]?.name ?: "Unknown"
                val amountStr = String.format(Locale.US, "%.2f", split.amount)
                "$name: $amountStr"
            }
            val safeSplitDetails = matchCsv(splitDetails)

            sb.append("$date,$description,$category,$amount,$safePayer,$safeSplitDetails\n")
        }
        
        return sb.toString()
    }

    private fun matchCsv(value: String): String {
        var safe = value.replace("\"", "\"\"") // Escape double quotes
        if (safe.contains(",") || safe.contains("\n") || safe.contains("\"")) {
            safe = "\"$safe\""
        }
        return safe
    }
}
