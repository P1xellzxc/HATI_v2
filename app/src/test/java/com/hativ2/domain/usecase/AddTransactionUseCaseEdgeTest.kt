package com.hativ2.domain.usecase

import com.hativ2.domain.repository.TransactionRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class AddTransactionUseCaseEdgeTest {

    private lateinit var useCase: AddTransactionUseCase
    private val repository: TransactionRepository = mock()

    @Before
    fun setup() {
        useCase = AddTransactionUseCase(repository)
        runTest {
            // repository.addExpense returns just Unit or throws exception based on implementation. 
            // The UseCase calls `transactionRepository.addExpense(expense, splits)`.
            // If it returns Unit (suspend function), then mocking it to return Unit is default or we can stub it.
            whenever(repository.addExpense(any(), any())).thenReturn(Unit)
        }
    }

    @Test
    fun `very large amount`() = runTest {
        val largeAmount = 999_999_999.99
        val result = useCase.execute(
            dashboardId = "dash1",
            description = "Mega Yacht",
            amount = largeAmount,
            paidBy = "user1",
            category = "Luxury",
            splitWith = listOf("user1", "user2")
        )
        
        assertTrue(result.isSuccess)
    }

    @Test
    fun `very small amount`() = runTest {
        val smallAmount = 0.01
        val result = useCase.execute(
            dashboardId = "dash1",
            description = "Gum",
            amount = smallAmount,
            paidBy = "user1",
            category = "Food",
            splitWith = listOf("user1")
        )
        
        assertTrue(result.isSuccess)
    }

    @Test
    fun `special characters in description`() = runTest {
        val desc = "Expense !@#$%^&*()_+"
        val result = useCase.execute(
            dashboardId = "dash1",
            description = desc,
            amount = 10.0,
            paidBy = "user1",
            category = "Misc",
            splitWith = listOf("user1")
        )
        
        assertTrue(result.isSuccess)
    }

    @Test
    fun `long description`() = runTest {
        val longDesc = "A".repeat(1000)
        val result = useCase.execute(
            dashboardId = "dash1",
            description = longDesc,
            amount = 10.0,
            paidBy = "user1",
            category = "Misc",
            splitWith = listOf("user1")
        )
        
        assertTrue(result.isSuccess)
    }
}
