package com.hativ2.domain.repository

import com.hativ2.data.entity.ExpenseEntity
import com.hativ2.data.entity.SplitEntity
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    suspend fun addExpense(expense: ExpenseEntity, splits: List<SplitEntity>)
    suspend fun updateExpense(expense: ExpenseEntity, splits: List<SplitEntity>)
    fun getExpensesByDateRange(dashboardId: String, startDate: Long, endDate: Long): Flow<List<ExpenseEntity>>
}
