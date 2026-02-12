package com.hativ2.domain.usecase

import com.hativ2.data.entity.ExpenseEntity
import com.hativ2.data.entity.SplitEntity
import com.hativ2.domain.repository.TransactionRepository
import java.util.UUID
import javax.inject.Inject

class AddTransactionUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    suspend fun execute(
        dashboardId: String,
        description: String,
        amount: Double,
        paidBy: String,
        category: String,
        splitWith: List<String>
    ): Result<String> {
        if (amount <= 0) {
            return Result.failure(IllegalArgumentException("Amount must be greater than 0"))
        }

        if (description.isBlank()) {
            return Result.failure(IllegalArgumentException("Description cannot be empty"))
        }

        val expenseId = UUID.randomUUID().toString()
        val expense = ExpenseEntity(
            id = expenseId,
            dashboardId = dashboardId,
            description = description,
            amount = amount,
            paidBy = paidBy,
            category = category,
            createdAt = System.currentTimeMillis()
        )

        // Default split strategy: Equal split among selected people
        // Prevent division by zero if splitWith is empty (though UI should prevent this)
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
            transactionRepository.addExpense(expense, splits)
            Result.success(expenseId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
