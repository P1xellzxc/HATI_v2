package com.hativ2.data.repository

import com.hativ2.data.dao.ExpenseDao
import com.hativ2.data.entity.ExpenseEntity
import com.hativ2.data.entity.SplitEntity
import com.hativ2.domain.repository.TransactionRepository
import javax.inject.Inject

class TransactionRepositoryImpl @Inject constructor(
    private val expenseDao: ExpenseDao
) : TransactionRepository {
    override suspend fun addExpense(expense: ExpenseEntity, splits: List<SplitEntity>) {
        expenseDao.saveExpenseWithSplits(expense, splits)
    }

    override fun getExpensesByDateRange(dashboardId: String, startDate: Long, endDate: Long): kotlinx.coroutines.flow.Flow<List<ExpenseEntity>> {
        return expenseDao.getExpensesByDateRange(dashboardId, startDate, endDate)
    }
}
