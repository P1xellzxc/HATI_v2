package com.hativ2.domain.usecase

import com.hativ2.data.dao.DashboardDao
import com.hativ2.data.dao.ExpenseDao
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * Exports all data for a dashboard as structured JSON.
 *
 * Why JSON in addition to CSV:
 *   - JSON preserves the hierarchical relationship between expenses and their
 *     splits, which CSV flattens into a single row.
 *   - JSON is machine-readable and can be re-imported or consumed by other tools
 *     more reliably than CSV.
 *   - Includes dashboard metadata and settlement history that CSV omits.
 */
class ExportDashboardJsonUseCase @Inject constructor(
    private val expenseDao: ExpenseDao,
    private val dashboardDao: DashboardDao
) {
    suspend fun execute(dashboardId: String): String {
        val dashboard = dashboardDao.getDashboardById(dashboardId)
        val expenses = expenseDao.getExpensesForDashboard(dashboardId).first()
        val settlements = expenseDao.getSettlementsForDashboard(dashboardId).first()
        val members = dashboardDao.getDashboardMembers(dashboardId).first()
        val membersMap = members.associateBy { it.id }

        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)

        val root = JSONObject()

        // Dashboard metadata
        root.put("exportVersion", 1)
        root.put("exportedAt", dateFormat.format(Date()))

        val dashboardJson = JSONObject()
        if (dashboard != null) {
            dashboardJson.put("id", dashboard.id)
            dashboardJson.put("title", dashboard.title)
            dashboardJson.put("type", dashboard.dashboardType)
            dashboardJson.put("currencySymbol", dashboard.currencySymbol)
            dashboardJson.put("themeColor", dashboard.themeColor)
            dashboardJson.put("createdAt", dateFormat.format(Date(dashboard.createdAt)))
        }
        root.put("dashboard", dashboardJson)

        // Members
        val membersArray = JSONArray()
        members.forEach { person ->
            val memberJson = JSONObject()
            memberJson.put("id", person.id)
            memberJson.put("name", person.name)
            memberJson.put("avatarColor", person.avatarColor)
            membersArray.put(memberJson)
        }
        root.put("members", membersArray)

        // Expenses with splits
        val expensesArray = JSONArray()
        expenses.forEach { expense ->
            val expenseJson = JSONObject()
            expenseJson.put("id", expense.id)
            expenseJson.put("description", expense.description)
            expenseJson.put("amount", expense.amount)
            expenseJson.put("category", expense.category)
            expenseJson.put("createdAt", dateFormat.format(Date(expense.createdAt)))

            val payerName = if (expense.paidBy == "user-current") "You"
                else membersMap[expense.paidBy]?.name ?: "Unknown"
            expenseJson.put("paidById", expense.paidBy)
            expenseJson.put("paidByName", payerName)

            // Splits
            val splits = expenseDao.getSplitsForExpense(expense.id)
            val splitsArray = JSONArray()
            splits.forEach { split ->
                val splitJson = JSONObject()
                splitJson.put("personId", split.personId)
                splitJson.put("personName",
                    if (split.personId == "user-current") "You"
                    else membersMap[split.personId]?.name ?: "Unknown"
                )
                splitJson.put("amount", split.amount)
                splitsArray.put(splitJson)
            }
            expenseJson.put("splits", splitsArray)

            expensesArray.put(expenseJson)
        }
        root.put("expenses", expensesArray)

        // Settlements
        val settlementsArray = JSONArray()
        settlements.forEach { settlement ->
            val settlementJson = JSONObject()
            settlementJson.put("id", settlement.id)
            settlementJson.put("fromId", settlement.fromId)
            settlementJson.put("fromName",
                if (settlement.fromId == "user-current") "You"
                else membersMap[settlement.fromId]?.name ?: "Unknown"
            )
            settlementJson.put("toId", settlement.toId)
            settlementJson.put("toName",
                if (settlement.toId == "user-current") "You"
                else membersMap[settlement.toId]?.name ?: "Unknown"
            )
            settlementJson.put("amount", settlement.amount)
            settlementJson.put("createdAt", dateFormat.format(Date(settlement.createdAt)))
            settlementsArray.put(settlementJson)
        }
        root.put("settlements", settlementsArray)

        // Summary stats
        val summary = JSONObject()
        summary.put("totalExpenses", expenses.size)
        summary.put("totalSettlements", settlements.size)
        summary.put("totalSpending", expenses.sumOf { it.amount })
        summary.put("memberCount", members.size)
        root.put("summary", summary)

        return root.toString(2)
    }
}
