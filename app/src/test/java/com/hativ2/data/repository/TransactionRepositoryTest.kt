package com.hativ2.data.repository

import com.hativ2.data.dao.ExpenseDao
import com.hativ2.data.entity.ExpenseEntity
import com.hativ2.data.entity.SplitEntity
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations

class TransactionRepositoryTest {

    @Mock
    private lateinit var expenseDao: ExpenseDao

    private lateinit var repository: TransactionRepositoryImpl

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        repository = TransactionRepositoryImpl(expenseDao)
    }

    @Test
    fun `addExpense calls dao saveExpenseWithSplits`() = runTest {
        // Given
        val expense = ExpenseEntity("e1", "dash1", "Food", 100.0, "u1", "Food", 1000)
        val splits = listOf(SplitEntity("s1", "e1", "u1", 100.0))

        // When
        repository.addExpense(expense, splits)

        // Then
        verify(expenseDao).saveExpenseWithSplits(expense, splits)
    }

    @Test
    fun `getExpensesByDateRange returns flow from dao`() = runTest {
        // Given
        val dashboardId = "dash1"
        val start = 1000L
        val end = 2000L
        val expenses = listOf(ExpenseEntity("e1", "dash1", "Food", 100.0, "u1", "Food", 1500))
        
        `when`(expenseDao.getExpensesByDateRange(dashboardId, start, end)).thenReturn(flowOf(expenses))

        // When
        val result = repository.getExpensesByDateRange(dashboardId, start, end)

        // Then
        val collected = result.toList()
        assertEquals(1, collected.size)
        assertEquals(expenses, collected[0])
        verify(expenseDao).getExpensesByDateRange(dashboardId, start, end)
    }
}
