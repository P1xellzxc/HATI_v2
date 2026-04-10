package com.hativ2.domain.usecase

import com.hativ2.data.dao.ExpenseDao
import com.hativ2.data.dao.DashboardDao
import com.hativ2.util.CsvExportManager
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
        sb.append("Date,Description,Category,Amount,Paid By,Split Details\n")
        
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        val allSplits = expenseDao.getSplitsForDashboard(dashboardId).first()
        val splitsByExpense = allSplits.groupBy { it.expenseId }
        
        expenses.forEach { expense ->
            val date = dateFormat.format(Date(expense.createdAt))
            // Escape description and category for CSV safety (wrap in quotes if contains comma)
            val description = CsvExportManager.escapeCsv(expense.description)
            val category = CsvExportManager.escapeCsv(expense.category)
            val amount = String.format(Locale.US, "%.2f", expense.amount)
            
            val payerName = if (expense.paidBy == "user-current") "You" 
                            else peopleMap[expense.paidBy]?.name ?: "Unknown"
            val safePayer = CsvExportManager.escapeCsv(payerName)

            // Get splits
            val splits = splitsByExpense[expense.id].orEmpty()
            val splitDetails = splits.joinToString(" | ") { split ->
                val name = if (split.personId == "user-current") "You" 
                           else peopleMap[split.personId]?.name ?: "Unknown"
                val amountStr = String.format(Locale.US, "%.2f", split.amount)
                "$name: $amountStr"
            }
            val safeSplitDetails = CsvExportManager.escapeCsv(splitDetails)

            sb.append("$date,$description,$category,$amount,$safePayer,$safeSplitDetails\n")
        }
        
        return sb.toString()
    }

}
