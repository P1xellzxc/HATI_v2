package com.hativ2.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hativ2.App
import com.hativ2.data.entity.DashboardEntity
import com.hativ2.data.entity.DashboardMemberEntity
import com.hativ2.data.entity.PersonEntity
import com.hativ2.data.entity.SettlementEntity
import com.hativ2.domain.usecase.CalculateDebtsUseCase
import com.hativ2.domain.model.DashboardWithStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import com.hativ2.domain.usecase.ExportDashboardCsvUseCase
import com.hativ2.domain.usecase.ExportDashboardJsonUseCase
import android.net.Uri
import android.content.Context
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

@dagger.hilt.android.lifecycle.HiltViewModel
open class MainViewModel @javax.inject.Inject constructor(
    application: Application,
    private val dashboardDao: com.hativ2.data.dao.DashboardDao,
    private val personDao: com.hativ2.data.dao.PersonDao,
    private val expenseDao: com.hativ2.data.dao.ExpenseDao,
    private val addTransactionUseCase: com.hativ2.domain.usecase.AddTransactionUseCase,
    private val calculateDashboardStatsUseCase: com.hativ2.domain.usecase.CalculateDashboardStatsUseCase,
    private val calculateDebtsUseCase: CalculateDebtsUseCase,
    private val updateExpenseUseCase: com.hativ2.domain.usecase.UpdateExpenseUseCase,
    private val exportDashboardCsvUseCase: ExportDashboardCsvUseCase,
    private val exportDashboardJsonUseCase: ExportDashboardJsonUseCase
) : AndroidViewModel(application) {
    
    // Dark mode state
    private val _isDarkMode = MutableStateFlow<Boolean?>(null) // null = follow system
    val isDarkMode: StateFlow<Boolean?> = _isDarkMode
    
    fun toggleDarkMode() {
        _isDarkMode.value = when (_isDarkMode.value) {
            null -> true   // system -> dark
            true -> false  // dark -> light
            false -> null  // light -> system
        }
    }

    val dashboards: StateFlow<List<DashboardEntity>> = dashboardDao.getAllDashboards()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val dashboardsWithStats: StateFlow<List<DashboardWithStats>> = dashboardDao.getAllDashboards()
        .flatMapLatest { dashboardList ->
            if (dashboardList.isEmpty()) {
                flowOf(emptyList())
            } else {
                combine(dashboardList.map { dashboard ->
                    combine(
                        expenseDao.getExpensesForDashboard(dashboard.id),
                        expenseDao.getSplitsForDashboard(dashboard.id),
                        expenseDao.getSettlementsForDashboard(dashboard.id),
                        dashboardDao.getDashboardMembers(dashboard.id)
                    ) { expenses, splits, settlements, members ->
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                            calculateDashboardStatsUseCase.execute(
                                dashboard = dashboard,
                                expenses = expenses,
                                splits = splits,
                                settlements = settlements,
                                members = members,
                                currentUserId = CURRENT_USER_ID
                            )
                        }
                    }
                }) { it.toList() }
            }
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun getExpenses(dashboardId: String): StateFlow<List<com.hativ2.data.entity.ExpenseEntity>> {
        return expenseDao.getExpensesForDashboard(dashboardId)
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    }

    fun getDebtSummary(dashboardId: String): StateFlow<com.hativ2.domain.model.DebtSummaryModel> {
        return combine(
            expenseDao.getExpensesForDashboard(dashboardId),
            expenseDao.getSplitsForDashboard(dashboardId),
            expenseDao.getSettlementsForDashboard(dashboardId),
            dashboardDao.getDashboardMembers(dashboardId)
        ) { expenses, splits, settlements, members ->
             kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                 calculateDebtsUseCase.execute(
                     expenses = expenses,
                     splits = splits,
                     settlements = settlements,
                     userIds = members.map { it.id },
                     currentUserId = CURRENT_USER_ID
                 )
             }
        }.stateIn(
            viewModelScope,
            SharingStarted.Lazily,
            com.hativ2.domain.model.DebtSummaryModel(
                transactions = emptyList(),
                balances = emptyMap(),
                totalOwedToYou = 0.0,
                totalYouOwe = 0.0,
                memberShares = emptyMap()
            )
        )
    }

    open fun getPeople(dashboardId: String): StateFlow<List<com.hativ2.data.entity.PersonEntity>> {
        return dashboardDao.getDashboardMembers(dashboardId)
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    }

    fun addPerson(dashboardId: String, name: String) {
        viewModelScope.launch {
            // Why use InputValidator.validatePersonName() instead of inline checks:
            // Centralizes validation rules in one place. If limits change, only
            // InputValidator needs updating.
            val result = com.hativ2.util.InputValidator.validatePersonName(name)
            if (result !is com.hativ2.util.InputValidator.ValidationResult.Valid) return@launch

            val personId = UUID.randomUUID().toString()
            val person = com.hativ2.data.entity.PersonEntity(
                id = personId,
                name = result.sanitized,
                avatarColor = "default",
                createdAt = System.currentTimeMillis()
            )
            personDao.insertPerson(person)
            dashboardDao.addMember(com.hativ2.data.entity.DashboardMemberEntity(dashboardId, personId, System.currentTimeMillis()))
        }
    }

    fun createExpense(
        dashboardId: String,
        description: String,
        amount: Double,
        paidBy: String,
        category: String,
        splitWith: List<String> 
    ) {
        viewModelScope.launch {
            val descResult = com.hativ2.util.InputValidator.validateExpenseDescription(description)
            val amountResult = com.hativ2.util.InputValidator.validateAmount(amount.toString())
            if (descResult !is com.hativ2.util.InputValidator.ValidationResult.Valid) return@launch
            if (amountResult !is com.hativ2.util.InputValidator.AmountValidationResult.Valid) return@launch

            addTransactionUseCase.execute(
                dashboardId = dashboardId,
                description = descResult.sanitized,
                amount = amountResult.amount,
                paidBy = paidBy,
                category = category,
                splitWith = splitWith
            )
        }
    }

    fun createDashboard(title: String, type: String, themeColor: String) {
        viewModelScope.launch {
            val titleResult = com.hativ2.util.InputValidator.validateDashboardTitle(title)
            if (titleResult !is com.hativ2.util.InputValidator.ValidationResult.Valid) return@launch

            val id = UUID.randomUUID().toString()
            val dashboard = DashboardEntity(
                id = id,
                title = titleResult.sanitized,
                coverImageUrl = null,
                currencySymbol = "₱",
                themeColor = themeColor,
                dashboardType = type,
                createdAt = System.currentTimeMillis(),
                order = dashboards.value.size
            )
            dashboardDao.insertDashboard(dashboard)
            
            ensureCurrentUser()
            
            dashboardDao.addMember(DashboardMemberEntity(id, CURRENT_USER_ID, System.currentTimeMillis()))
        }
    }

    fun deleteDashboard(dashboardId: String) {
        viewModelScope.launch {
            dashboardDao.deleteDashboard(dashboardId)
        }
    }

    fun updateDashboard(dashboardId: String, title: String, type: String, themeColor: String) {
        viewModelScope.launch {
            val titleResult = com.hativ2.util.InputValidator.validateDashboardTitle(title)
            if (titleResult !is com.hativ2.util.InputValidator.ValidationResult.Valid) return@launch

            val currentList = dashboards.value
            val current = currentList.find { it.id == dashboardId } ?: return@launch
            dashboardDao.updateDashboard(current.copy(title = titleResult.sanitized, dashboardType = type, themeColor = themeColor))
        }
    }

    fun deleteExpense(expenseId: String) {
        viewModelScope.launch {
            expenseDao.deleteExpense(expenseId)
        }
    }
    
    // We also need to expose getting a single expense for editing
    suspend fun getExpenseById(expenseId: String): com.hativ2.data.entity.ExpenseEntity? {
         return expenseDao.getExpenseById(expenseId)
    }
    
    suspend fun getSplitsForExpense(expenseId: String): List<com.hativ2.data.entity.SplitEntity> {
        return expenseDao.getSplitsForExpense(expenseId)
    }

    fun updateExpense(
        expenseId: String,
        dashboardId: String,
        description: String,
        amount: Double,
        paidBy: String,
        category: String,
        splitWith: List<String>
    ) {
        viewModelScope.launch {
            val descResult = com.hativ2.util.InputValidator.validateExpenseDescription(description)
            val amountResult = com.hativ2.util.InputValidator.validateAmount(amount.toString())
            if (descResult !is com.hativ2.util.InputValidator.ValidationResult.Valid) return@launch
            if (amountResult !is com.hativ2.util.InputValidator.AmountValidationResult.Valid) return@launch

            // Get existing createdAt to preserve it
            val existing = expenseDao.getExpenseById(expenseId)
            val createdAt = existing?.createdAt ?: System.currentTimeMillis()

            updateExpenseUseCase.execute(
                expenseId = expenseId,
                dashboardId = dashboardId,
                description = descResult.sanitized,
                amount = amountResult.amount,
                paidBy = paidBy,
                category = category,
                splitWith = splitWith,
                originalCreatedAt = createdAt
            )
        }
    }

    private suspend fun ensureCurrentUser() {
        if (personDao.getPersonById(CURRENT_USER_ID) == null) {
            personDao.insertPerson(
                PersonEntity(
                    id = CURRENT_USER_ID,
                    name = "You",
                    avatarColor = com.hativ2.ui.theme.HEX_NOTION_ORANGE,
                    createdAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun settleUp(dashboardId: String, fromId: String, toId: String, amount: Double) {
        viewModelScope.launch {
            // Validate settlement amount — prevent negative, zero, or excessive values.
            if (amount <= 0 || amount > com.hativ2.util.InputValidator.MAX_AMOUNT) return@launch
            // Prevent self-settlement which would corrupt balance calculations.
            if (fromId == toId) return@launch

            val settlement = SettlementEntity(
                id = UUID.randomUUID().toString(),
                dashboardId = dashboardId,
                fromId = fromId,
                toId = toId,
                amount = amount,
                createdAt = System.currentTimeMillis()
            )
            expenseDao.insertSettlement(settlement)
        }
    }

    fun getTransactions(dashboardId: String): StateFlow<List<TransactionDisplayItem>> {
        return combine(
             expenseDao.getExpensesForDashboard(dashboardId),
             expenseDao.getSettlementsForDashboard(dashboardId)
        ) { expenses, settlements ->
            val expenseItems = expenses.map { TransactionDisplayItem.ExpenseItem(it) }
            val settlementItems = settlements.map { TransactionDisplayItem.SettlementItem(it) }
            (expenseItems + settlementItems).sortedByDescending { it.date }
        }
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    }
    fun getAllTransactions(): StateFlow<List<TransactionDisplayItem>> {
        return combine(
             expenseDao.getAllExpenses(),
             expenseDao.getAllSettlements()
        ) { expenses, settlements ->
            val expenseItems = expenses.map { TransactionDisplayItem.ExpenseItem(it) }
            val settlementItems = settlements.map { TransactionDisplayItem.SettlementItem(it) }
            (expenseItems + settlementItems).sortedByDescending { it.date }
        }
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    }

    fun deleteSettlement(settlementId: String) {
        viewModelScope.launch {
            expenseDao.deleteSettlement(settlementId)
        }
    }

    fun exportCsv(dashboardId: String, uri: Uri, context: Context) {
        viewModelScope.launch {
            try {
                val csvContent = exportDashboardCsvUseCase.execute(dashboardId)
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.write(csvContent.toByteArray())
                    }
                }
                Toast.makeText(context, "Export successful", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                // Why not e.printStackTrace() + e.message in Toast:
                // 1. printStackTrace() writes to Logcat which can be read by other
                //    apps on rooted devices, potentially leaking file paths or
                //    internal class names.
                // 2. Showing e.message to users could reveal internal details
                //    (file paths, SQL errors, etc.) that aid attackers.
                // Instead: log a generic message and show a user-friendly toast.
                android.util.Log.w("MainViewModel", "CSV export failed", e)
                Toast.makeText(context, "Export failed. Please try again.", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun exportJson(dashboardId: String, uri: Uri, context: Context) {
        viewModelScope.launch {
            try {
                val jsonContent = exportDashboardJsonUseCase.execute(dashboardId)
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.write(jsonContent.toByteArray())
                    }
                }
                Toast.makeText(context, "JSON export successful", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                android.util.Log.w("MainViewModel", "JSON export failed", e)
                Toast.makeText(context, "Export failed. Please try again.", Toast.LENGTH_LONG).show()
            }
        }
    }

    companion object {
        const val CURRENT_USER_ID = "user-current"
    }
}

sealed interface TransactionDisplayItem {
    val id: String
    val date: Long
    
    data class ExpenseItem(val expense: com.hativ2.data.entity.ExpenseEntity) : TransactionDisplayItem {
         override val id = expense.id
         override val date = expense.createdAt
    }
    
    data class SettlementItem(val settlement: com.hativ2.data.entity.SettlementEntity) : TransactionDisplayItem {
         override val id = settlement.id
         override val date = settlement.createdAt
    }
}