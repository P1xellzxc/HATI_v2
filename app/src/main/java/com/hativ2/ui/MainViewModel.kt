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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val database = (application as App).database
    private val dashboardDao = database.dashboardDao()
    private val personDao = database.personDao()
    private val expenseDao = database.expenseDao()

    private val calculateDebtsUseCase = CalculateDebtsUseCase()

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
                            val debtSummary = calculateDebtsUseCase.execute(
                                expenses = expenses,
                                splits = splits,
                                settlements = settlements,
                                userIds = members.map { it.id },
                                currentUserId = "user-current"
                            )
                            DashboardWithStats(
                                id = dashboard.id,
                                title = dashboard.title,
                                coverImageUrl = dashboard.coverImageUrl,
                                coverImageOffsetY = dashboard.coverImageOffsetY,
                                coverImageZoom = dashboard.coverImageZoom,
                                currencySymbol = dashboard.currencySymbol,
                                themeColor = dashboard.themeColor,
                                dashboardType = dashboard.dashboardType,
                                createdAt = dashboard.createdAt,
                                order = dashboard.order,
                                expenseCount = expenses.size,
                                totalSpent = expenses.sumOf { it.amount },
                                netBalance = debtSummary.totalOwedToYou - debtSummary.totalYouOwe
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
                     currentUserId = "user-current" 
                 )
             }
        }.stateIn(viewModelScope, SharingStarted.Lazily, com.hativ2.domain.model.DebtSummaryModel(emptyList(), emptyMap(), 0.0, 0.0, emptyMap()))
    }

    fun getPeople(dashboardId: String): StateFlow<List<com.hativ2.data.entity.PersonEntity>> {
        return dashboardDao.getDashboardMembers(dashboardId)
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    }

    fun addPerson(dashboardId: String, name: String) {
        viewModelScope.launch {
            val personId = UUID.randomUUID().toString()
            val person = com.hativ2.data.entity.PersonEntity(
                id = personId,
                name = name,
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
            val expenseId = UUID.randomUUID().toString()
            val expense = com.hativ2.data.entity.ExpenseEntity(
                id = expenseId,
                dashboardId = dashboardId,
                description = description,
                amount = amount,
                paidBy = paidBy,
                category = category,
                createdAt = System.currentTimeMillis()
            )
            
            // Default split strategy: Equal split among selected people
            val splitAmount = amount / splitWith.size
            val splits = splitWith.map { personId ->
                com.hativ2.data.entity.SplitEntity(
                    id = UUID.randomUUID().toString(),
                    expenseId = expenseId,
                    personId = personId,
                    amount = splitAmount
                )
            }

            expenseDao.saveExpenseWithSplits(expense, splits)
        }
    }

    fun createDashboard(title: String, type: String, themeColor: String) {
        viewModelScope.launch {
            val id = UUID.randomUUID().toString()
            val dashboard = DashboardEntity(
                id = id,
                title = title,
                coverImageUrl = null,
                currencySymbol = "₱",
                themeColor = themeColor,
                dashboardType = type,
                createdAt = System.currentTimeMillis(),
                order = dashboards.value.size
            )
            dashboardDao.insertDashboard(dashboard)
            
            ensureCurrentUser()
            
            dashboardDao.addMember(DashboardMemberEntity(id, "user-current", System.currentTimeMillis()))
        }
    }

    fun deleteDashboard(dashboardId: String) {
        viewModelScope.launch {
            dashboardDao.deleteDashboard(dashboardId)
        }
    }

    fun updateDashboard(dashboardId: String, title: String, type: String, themeColor: String) {
        viewModelScope.launch {
            val currentList = dashboards.value
            val current = currentList.find { it.id == dashboardId } ?: return@launch
            dashboardDao.updateDashboard(current.copy(title = title, dashboardType = type, themeColor = themeColor))
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
             val expense = com.hativ2.data.entity.ExpenseEntity(
                id = expenseId,
                dashboardId = dashboardId,
                description = description,
                amount = amount,
                paidBy = paidBy,
                category = category,
                createdAt = System.currentTimeMillis() // Or keep original creation time? Ideally keep original. 
                // For simplified logic we just overwrite, but let's try to keep original createdAt if we fetched it.
                // Since this function signature doesn't take createdAt, we'll just set it to now or we need to fetch first.
                // Let's just update the fields we care about.
            )
            // Ideally we get the existing createdAt. 
            val existing = expenseDao.getExpenseById(expenseId)
            val finalExpense = if(existing != null) expense.copy(createdAt = existing.createdAt) else expense

            // Default split strategy: Equal split among selected people
            val splitAmount = amount / splitWith.size
            val splits = splitWith.map { personId ->
                com.hativ2.data.entity.SplitEntity(
                    id = UUID.randomUUID().toString(),
                    expenseId = expenseId,
                    personId = personId,
                    amount = splitAmount
                )
            }

            expenseDao.saveExpenseWithSplits(finalExpense, splits)
        }
    }

    private suspend fun ensureCurrentUser() {
        if (personDao.getPersonById("user-current") == null) {
            personDao.insertPerson(
                PersonEntity(
                    id = "user-current",
                    name = "You",
                    avatarColor = "#fed7aa",
                    createdAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun settleUp(dashboardId: String, fromId: String, toId: String, amount: Double) {
        viewModelScope.launch {
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
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
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