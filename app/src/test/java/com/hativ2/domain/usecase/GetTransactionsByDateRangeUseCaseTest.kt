package com.hativ2.domain.usecase

import com.hativ2.data.entity.ExpenseEntity
import com.hativ2.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations

class GetTransactionsByDateRangeUseCaseTest {

    @Mock
    private lateinit var transactionRepository: TransactionRepository

    private lateinit var getTransactionsByDateRangeUseCase: GetTransactionsByDateRangeUseCase

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        getTransactionsByDateRangeUseCase = GetTransactionsByDateRangeUseCase(transactionRepository)
    }

    @Test
    fun `execute returns transactions within range`() = runTest {
        val dashboardId = "dash-1"
        val startDate = 1000L
        val endDate = 2000L
        val expectedExpenses = listOf(
            ExpenseEntity("id-1", dashboardId, "Lunch", 100.0, "user-1", "Food", 1500L),
            ExpenseEntity("id-2", dashboardId, "Dinner", 200.0, "user-1", "Food", 1600L)
        )

        `when`(transactionRepository.getExpensesByDateRange(dashboardId, startDate, endDate))
            .thenReturn(flowOf(expectedExpenses))

        val resultFlow = getTransactionsByDateRangeUseCase.execute(dashboardId, startDate, endDate)
        
        resultFlow.collect { expenses ->
            assertEquals(expectedExpenses, expenses)
        }

        verify(transactionRepository).getExpensesByDateRange(dashboardId, startDate, endDate)
    }

    @Test
    fun `execute throws exception when startDate is after endDate`() {
        val dashboardId = "dash-1"
        val startDate = 2000L
        val endDate = 1000L

        val exception = assertThrows(IllegalArgumentException::class.java) {
            getTransactionsByDateRangeUseCase.execute(dashboardId, startDate, endDate)
        }

        assertEquals("Start date cannot be after end date", exception.message)
    }
}
