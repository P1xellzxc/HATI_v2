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
import org.mockito.kotlin.never

class UpdateExpenseUseCaseTest {

    @Mock
    private lateinit var transactionRepository: TransactionRepository

    private lateinit var useCase: UpdateExpenseUseCase

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        useCase = UpdateExpenseUseCase(transactionRepository)
    }

    @Test
    fun `execute updates expense with correct fields`() = runTest {
        val result = useCase.execute(
            expenseId = "exp-1",
            dashboardId = "dash-1",
            description = "Updated Lunch",
            amount = 120.0,
            paidBy = "user-1",
            category = "Food",
            splitWith = listOf("user-1", "user-2"),
            originalCreatedAt = 1000L
        )

        assertTrue(result.isSuccess)

        val expenseCaptor = argumentCaptor<ExpenseEntity>()
        val splitsCaptor = argumentCaptor<List<SplitEntity>>()
        verify(transactionRepository).updateExpense(expenseCaptor.capture(), splitsCaptor.capture())

        val captured = expenseCaptor.firstValue
        assertEquals("exp-1", captured.id)
        assertEquals("dash-1", captured.dashboardId)
        assertEquals("Updated Lunch", captured.description)
        assertEquals(120.0, captured.amount, 0.0)
        assertEquals("user-1", captured.paidBy)
        assertEquals("Food", captured.category)
        assertEquals(1000L, captured.createdAt)
    }

    @Test
    fun `execute creates equal splits among participants`() = runTest {
        useCase.execute(
            expenseId = "exp-1",
            dashboardId = "dash-1",
            description = "Dinner",
            amount = 300.0,
            paidBy = "user-1",
            category = "Food",
            splitWith = listOf("user-1", "user-2", "user-3"),
            originalCreatedAt = 1000L
        )

        val splitsCaptor = argumentCaptor<List<SplitEntity>>()
        verify(transactionRepository).updateExpense(any(), splitsCaptor.capture())

        val splits = splitsCaptor.firstValue
        assertEquals(3, splits.size)
        splits.forEach { split ->
            assertEquals(100.0, split.amount, 0.0)
            assertEquals("exp-1", split.expenseId)
        }
    }

    @Test
    fun `execute preserves original expenseId in splits`() = runTest {
        useCase.execute(
            expenseId = "exp-42",
            dashboardId = "dash-1",
            description = "Test",
            amount = 50.0,
            paidBy = "user-1",
            category = "Misc",
            splitWith = listOf("user-1"),
            originalCreatedAt = 5000L
        )

        val splitsCaptor = argumentCaptor<List<SplitEntity>>()
        verify(transactionRepository).updateExpense(any(), splitsCaptor.capture())

        splitsCaptor.firstValue.forEach { split ->
            assertEquals("exp-42", split.expenseId)
        }
    }

    @Test
    fun `execute rejects negative amount`() = runTest {
        val result = useCase.execute(
            expenseId = "exp-1",
            dashboardId = "dash-1",
            description = "Lunch",
            amount = -10.0,
            paidBy = "user-1",
            category = "Food",
            splitWith = listOf("user-1"),
            originalCreatedAt = 1000L
        )

        assertTrue(result.isFailure)
        assertEquals("Amount must be greater than 0", result.exceptionOrNull()?.message)
        verify(transactionRepository, never()).updateExpense(any(), any())
    }

    @Test
    fun `execute rejects zero amount`() = runTest {
        val result = useCase.execute(
            expenseId = "exp-1",
            dashboardId = "dash-1",
            description = "Lunch",
            amount = 0.0,
            paidBy = "user-1",
            category = "Food",
            splitWith = listOf("user-1"),
            originalCreatedAt = 1000L
        )

        assertTrue(result.isFailure)
        assertEquals("Amount must be greater than 0", result.exceptionOrNull()?.message)
        verify(transactionRepository, never()).updateExpense(any(), any())
    }

    @Test
    fun `execute rejects empty description`() = runTest {
        val result = useCase.execute(
            expenseId = "exp-1",
            dashboardId = "dash-1",
            description = "",
            amount = 100.0,
            paidBy = "user-1",
            category = "Food",
            splitWith = listOf("user-1"),
            originalCreatedAt = 1000L
        )

        assertTrue(result.isFailure)
        assertEquals("Description cannot be empty", result.exceptionOrNull()?.message)
        verify(transactionRepository, never()).updateExpense(any(), any())
    }

    @Test
    fun `execute rejects blank description`() = runTest {
        val result = useCase.execute(
            expenseId = "exp-1",
            dashboardId = "dash-1",
            description = "   ",
            amount = 100.0,
            paidBy = "user-1",
            category = "Food",
            splitWith = listOf("user-1"),
            originalCreatedAt = 1000L
        )

        assertTrue(result.isFailure)
        assertEquals("Description cannot be empty", result.exceptionOrNull()?.message)
        verify(transactionRepository, never()).updateExpense(any(), any())
    }

    @Test
    fun `execute rejects empty split list`() = runTest {
        val result = useCase.execute(
            expenseId = "exp-1",
            dashboardId = "dash-1",
            description = "Lunch",
            amount = 100.0,
            paidBy = "user-1",
            category = "Food",
            splitWith = emptyList(),
            originalCreatedAt = 1000L
        )

        assertTrue(result.isFailure)
        assertEquals("Must split with at least one person", result.exceptionOrNull()?.message)
        verify(transactionRepository, never()).updateExpense(any(), any())
    }

    @Test
    fun `execute handles repository exception gracefully`() = runTest {
        org.mockito.kotlin.whenever(transactionRepository.updateExpense(any(), any()))
            .thenThrow(RuntimeException("DB error"))

        val result = useCase.execute(
            expenseId = "exp-1",
            dashboardId = "dash-1",
            description = "Lunch",
            amount = 100.0,
            paidBy = "user-1",
            category = "Food",
            splitWith = listOf("user-1"),
            originalCreatedAt = 1000L
        )

        assertTrue(result.isFailure)
        assertEquals("DB error", result.exceptionOrNull()?.message)
    }

    @Test
    fun `execute with single person split gives full amount`() = runTest {
        useCase.execute(
            expenseId = "exp-1",
            dashboardId = "dash-1",
            description = "Solo meal",
            amount = 75.0,
            paidBy = "user-1",
            category = "Food",
            splitWith = listOf("user-1"),
            originalCreatedAt = 1000L
        )

        val splitsCaptor = argumentCaptor<List<SplitEntity>>()
        verify(transactionRepository).updateExpense(any(), splitsCaptor.capture())

        val splits = splitsCaptor.firstValue
        assertEquals(1, splits.size)
        assertEquals(75.0, splits[0].amount, 0.0)
        assertEquals("user-1", splits[0].personId)
    }
}
