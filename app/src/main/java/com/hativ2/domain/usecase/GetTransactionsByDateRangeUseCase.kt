package com.hativ2.domain.usecase

import com.hativ2.data.entity.ExpenseEntity
import com.hativ2.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetTransactionsByDateRangeUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    fun execute(dashboardId: String, startDate: Long, endDate: Long): Flow<List<ExpenseEntity>> {
        if (startDate > endDate) {
            throw IllegalArgumentException("Start date cannot be after end date")
        }
        return transactionRepository.getExpensesByDateRange(dashboardId, startDate, endDate)
    }
}
