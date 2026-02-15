package com.hativ2.domain.usecase

import com.hativ2.data.entity.ExpenseEntity
import com.hativ2.data.entity.SplitEntity
import com.hativ2.domain.repository.TransactionRepository
import java.util.UUID
import javax.inject.Inject

class UpdateExpenseUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    suspend fun execute(
        expenseId: String,
        dashboardId: String,
        description: String,
        amount: Double,
        paidBy: String,
        category: String,
        splitWith: List<String>,
        originalCreatedAt: Long
    ): Result<Unit> {
        if (amount <= 0) {
            return Result.failure(IllegalArgumentException("Amount must be greater than 0"))
        }

        if (description.isBlank()) {
            return Result.failure(IllegalArgumentException("Description cannot be empty"))
        }

        val expense = ExpenseEntity(
            id = expenseId,
            dashboardId = dashboardId,
            description = description,
            amount = amount,
            paidBy = paidBy,
            category = category,
            createdAt = originalCreatedAt
        )

        // Default split strategy: Equal split among selected people
        val splitCount = splitWith.size
        if (splitCount == 0) {
             return Result.failure(IllegalArgumentException("Must split with at least one person"))
        }

        val splitAmount = amount / splitCount
        val splits = splitWith.map { personId ->
            SplitEntity(
                id = UUID.randomUUID().toString(),
                expenseId = expenseId,
                personId = personId,
                amount = splitAmount
            )
        }

        return try {
            transactionRepository.updateExpense(expense, splits)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
