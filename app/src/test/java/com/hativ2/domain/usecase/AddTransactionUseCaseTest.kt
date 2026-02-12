package com.hativ2.domain.usecase

import com.hativ2.data.entity.ExpenseEntity
import com.hativ2.data.entity.SplitEntity
import com.hativ2.domain.repository.TransactionRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.times

class AddTransactionUseCaseTest {

    @Mock
    private lateinit var transactionRepository: TransactionRepository

    private lateinit var addTransactionUseCase: AddTransactionUseCase

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        addTransactionUseCase = AddTransactionUseCase(transactionRepository)
    }

    @Test
    fun `execute creates valid transaction`() = runTest {
        val dashboardId = "dash-1"
        val description = "Lunch"
        val amount = 100.0
        val paidBy = "user-1"
        val category = "Food"
        val splitWith = listOf("user-1", "user-2")

        val result = addTransactionUseCase.execute(
            dashboardId, description, amount, paidBy, category, splitWith
        )

        assertTrue(result.isSuccess)

        val expenseCaptor = argumentCaptor<ExpenseEntity>()
        val splitsCaptor = argumentCaptor<List<SplitEntity>>()

        verify(transactionRepository).addExpense(expenseCaptor.capture(), splitsCaptor.capture())

        val capturedExpense = expenseCaptor.firstValue
        assertEquals(description, capturedExpense.description)
        assertEquals(amount, capturedExpense.amount, 0.0)
        assertEquals(dashboardId, capturedExpense.dashboardId)

        val capturedSplits = splitsCaptor.firstValue
        assertEquals(2, capturedSplits.size)
        assertEquals(50.0, capturedSplits[0].amount, 0.0)
    }

    @Test
    fun `execute rejects negative amount`() = runTest {
        val result = addTransactionUseCase.execute(
            "dash-1", "Lunch", -10.0, "user-1", "Food", listOf("user-1")
        )
        assertTrue(result.isFailure)
        assertEquals("Amount must be greater than 0", result.exceptionOrNull()?.message)
        verify(transactionRepository, times(0)).addExpense(any(), any())
    }

    @Test
    fun `execute rejects empty description`() = runTest {
        val result = addTransactionUseCase.execute(
            "dash-1", "", 100.0, "user-1", "Food", listOf("user-1")
        )
        assertTrue(result.isFailure)
        assertEquals("Description cannot be empty", result.exceptionOrNull()?.message)
        verify(transactionRepository, times(0)).addExpense(any(), any())
    }
    
    @Test
    fun `execute rejects empty split list`() = runTest {
        val result = addTransactionUseCase.execute(
            "dash-1", "Lunch", 100.0, "user-1", "Food", emptyList()
        )
        assertTrue(result.isFailure)
        assertEquals("Must split with at least one person", result.exceptionOrNull()?.message)
        verify(transactionRepository, times(0)).addExpense(any(), any())
    }
}
