package com.hativ2.ui

import android.app.Application
import com.hativ2.data.dao.DashboardDao
import com.hativ2.data.dao.ExpenseDao
import com.hativ2.data.dao.PersonDao
import com.hativ2.data.entity.DashboardEntity
import com.hativ2.data.entity.PersonEntity
import com.hativ2.data.entity.SettlementEntity
import com.hativ2.domain.usecase.AddTransactionUseCase
import com.hativ2.domain.usecase.CalculateDashboardStatsUseCase
import com.hativ2.domain.usecase.CalculateDebtsUseCase
import com.hativ2.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Mock
    private lateinit var application: Application
    @Mock
    private lateinit var dashboardDao: DashboardDao
    @Mock
    private lateinit var personDao: PersonDao
    @Mock
    private lateinit var expenseDao: ExpenseDao
    @Mock
    private lateinit var addTransactionUseCase: AddTransactionUseCase
    @Mock
    private lateinit var calculateDashboardStatsUseCase: CalculateDashboardStatsUseCase
    @Mock
    private lateinit var calculateDebtsUseCase: CalculateDebtsUseCase

    @Mock
    private lateinit var updateExpenseUseCase: com.hativ2.domain.usecase.UpdateExpenseUseCase

    @Mock
    private lateinit var exportDashboardCsvUseCase: com.hativ2.domain.usecase.ExportDashboardCsvUseCase

    @Mock
    private lateinit var exportDashboardJsonUseCase: com.hativ2.domain.usecase.ExportDashboardJsonUseCase

    private lateinit var viewModel: MainViewModel

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        
        // Default mocks
        whenever(dashboardDao.getAllDashboards()).thenReturn(flowOf(emptyList()))
        
        viewModel = MainViewModel(
            application,
            dashboardDao,
            personDao,
            expenseDao,
            addTransactionUseCase,
            calculateDashboardStatsUseCase,
            calculateDebtsUseCase,
            updateExpenseUseCase,
            exportDashboardCsvUseCase,
            exportDashboardJsonUseCase
        )
    }

    // -------- Dashboard CRUD tests --------

    @Test
    fun `createDashboard inserts dashboard and member`() = runTest {
        val title = "New Trip"
        val type = "travel"
        val theme = "#FF0000"
        
        whenever(personDao.getPersonById("user-current")).thenReturn(null)

        viewModel.createDashboard(title, type, theme)
        advanceUntilIdle()

        val dashboardCaptor = argumentCaptor<DashboardEntity>()
        verify(dashboardDao).insertDashboard(dashboardCaptor.capture())
        
        val capturedDashboard = dashboardCaptor.firstValue
        assertEquals("New Trip", capturedDashboard.title)
        assertEquals("travel", capturedDashboard.dashboardType)
        
        verify(dashboardDao).addMember(any())
        verify(personDao).insertPerson(any())
    }

    @Test
    fun `createDashboard does not insert when title is blank`() = runTest {
        viewModel.createDashboard("", "travel", "#FF0000")
        advanceUntilIdle()

        verify(dashboardDao, never()).insertDashboard(any())
    }

    @Test
    fun `createDashboard does not insert when title is whitespace only`() = runTest {
        viewModel.createDashboard("   ", "travel", "#FF0000")
        advanceUntilIdle()

        verify(dashboardDao, never()).insertDashboard(any())
    }

    @Test
    fun `createDashboard sanitizes title`() = runTest {
        whenever(personDao.getPersonById("user-current")).thenReturn(null)

        viewModel.createDashboard("  My  Trip  ", "travel", "#FF0000")
        advanceUntilIdle()

        val dashboardCaptor = argumentCaptor<DashboardEntity>()
        verify(dashboardDao).insertDashboard(dashboardCaptor.capture())
        assertEquals("My Trip", dashboardCaptor.firstValue.title)
    }

    @Test
    fun `createDashboard does not recreate existing current user`() = runTest {
        val existingUser = PersonEntity("user-current", "You", "default", 1000L)
        whenever(personDao.getPersonById("user-current")).thenReturn(existingUser)

        viewModel.createDashboard("Trip", "travel", "#FF0000")
        advanceUntilIdle()

        verify(personDao, never()).insertPerson(any())
    }

    @Test
    fun `deleteDashboard calls dao`() = runTest {
        viewModel.deleteDashboard("dash-1")
        advanceUntilIdle()

        verify(dashboardDao).deleteDashboard("dash-1")
    }

    @Test
    fun `updateDashboard validates title and updates`() = runTest {
        val existingDashboard = DashboardEntity(
            id = "dash-1", title = "Old Title", coverImageUrl = null,
            currencySymbol = "₱", themeColor = "#000000",
            dashboardType = "travel", createdAt = 1000L, order = 0
        )
        whenever(dashboardDao.getAllDashboards()).thenReturn(flowOf(listOf(existingDashboard)))

        // Recreate viewModel with the new mock
        viewModel = MainViewModel(
            application, dashboardDao, personDao, expenseDao,
            addTransactionUseCase, calculateDashboardStatsUseCase,
            calculateDebtsUseCase, updateExpenseUseCase,
            exportDashboardCsvUseCase, exportDashboardJsonUseCase
        )

        viewModel.updateDashboard("dash-1", "New Title", "household", "#111111")
        advanceUntilIdle()

        val captor = argumentCaptor<DashboardEntity>()
        verify(dashboardDao).updateDashboard(captor.capture())
        assertEquals("New Title", captor.firstValue.title)
        assertEquals("household", captor.firstValue.dashboardType)
        assertEquals("#111111", captor.firstValue.themeColor)
    }

    @Test
    fun `updateDashboard does not update with blank title`() = runTest {
        viewModel.updateDashboard("dash-1", "", "travel", "#FF0000")
        advanceUntilIdle()

        verify(dashboardDao, never()).updateDashboard(any())
    }

    // -------- Expense tests --------

    @Test
    fun `getExpenses returns correct flow from dao`() = runTest {
        val dashboardId = "dash-1"
        val expenses = listOf(
            com.hativ2.data.entity.ExpenseEntity("e1", dashboardId, "Food", 100.0, "u1", "Food", 1000)
        )
        whenever(expenseDao.getExpensesForDashboard(dashboardId)).thenReturn(flowOf(expenses))

        val resultFlow = viewModel.getExpenses(dashboardId)

        verify(expenseDao).getExpensesForDashboard(dashboardId)
    }

    @Test
    fun `deleteExpense calls dao`() = runTest {
        viewModel.deleteExpense("exp-1")
        advanceUntilIdle()

        verify(expenseDao).deleteExpense("exp-1")
    }

    @Test
    fun `createExpense validates and delegates to useCase`() = runTest {
        whenever(addTransactionUseCase.execute(any(), any(), any(), any(), any(), any()))
            .thenReturn(Result.success("exp-1"))

        viewModel.createExpense("dash-1", "Lunch", 50.0, "u1", "Food", listOf("u1", "u2"))
        advanceUntilIdle()

        verify(addTransactionUseCase).execute(
            any(), any(), any(), any(), any(), any()
        )
    }

    @Test
    fun `createExpense does not call useCase with blank description`() = runTest {
        viewModel.createExpense("dash-1", "  ", 50.0, "u1", "Food", listOf("u1"))
        advanceUntilIdle()

        verify(addTransactionUseCase, never()).execute(any(), any(), any(), any(), any(), any())
    }

    @Test
    fun `createExpense does not call useCase with zero amount`() = runTest {
        viewModel.createExpense("dash-1", "Lunch", 0.0, "u1", "Food", listOf("u1"))
        advanceUntilIdle()

        verify(addTransactionUseCase, never()).execute(any(), any(), any(), any(), any(), any())
    }

    // -------- People tests --------

    @Test
    fun `addPerson validates name and inserts`() = runTest {
        viewModel.addPerson("dash-1", "Alice")
        advanceUntilIdle()

        verify(personDao).insertPerson(any())
        verify(dashboardDao).addMember(any())
    }

    @Test
    fun `addPerson does not insert with blank name`() = runTest {
        viewModel.addPerson("dash-1", "")
        advanceUntilIdle()

        verify(personDao, never()).insertPerson(any())
    }

    @Test
    fun `addPerson sanitizes name`() = runTest {
        viewModel.addPerson("dash-1", "  Alice  Bob  ")
        advanceUntilIdle()

        val captor = argumentCaptor<PersonEntity>()
        verify(personDao).insertPerson(captor.capture())
        assertEquals("Alice Bob", captor.firstValue.name)
    }

    // -------- Settlement tests --------

    @Test
    fun `settleUp inserts valid settlement`() = runTest {
        viewModel.settleUp("dash-1", "u1", "u2", 50.0)
        advanceUntilIdle()

        val captor = argumentCaptor<SettlementEntity>()
        verify(expenseDao).insertSettlement(captor.capture())
        assertEquals("dash-1", captor.firstValue.dashboardId)
        assertEquals("u1", captor.firstValue.fromId)
        assertEquals("u2", captor.firstValue.toId)
        assertEquals(50.0, captor.firstValue.amount, 0.0)
    }

    @Test
    fun `settleUp rejects zero amount`() = runTest {
        viewModel.settleUp("dash-1", "u1", "u2", 0.0)
        advanceUntilIdle()

        verify(expenseDao, never()).insertSettlement(any())
    }

    @Test
    fun `settleUp rejects negative amount`() = runTest {
        viewModel.settleUp("dash-1", "u1", "u2", -10.0)
        advanceUntilIdle()

        verify(expenseDao, never()).insertSettlement(any())
    }

    @Test
    fun `settleUp rejects self-settlement`() = runTest {
        viewModel.settleUp("dash-1", "u1", "u1", 50.0)
        advanceUntilIdle()

        verify(expenseDao, never()).insertSettlement(any())
    }

    @Test
    fun `settleUp rejects amount exceeding max`() = runTest {
        // InputValidator.MAX_AMOUNT is 1_000_000.0; anything above should be rejected
        viewModel.settleUp("dash-1", "u1", "u2", com.hativ2.util.InputValidator.MAX_AMOUNT + 1.0)
        advanceUntilIdle()

        verify(expenseDao, never()).insertSettlement(any())
    }

    @Test
    fun `deleteSettlement calls dao`() = runTest {
        viewModel.deleteSettlement("set-1")
        advanceUntilIdle()

        verify(expenseDao).deleteSettlement("set-1")
    }

    // -------- Dark mode tests --------

    @Test
    fun `initial dark mode is null (system default)`() {
        assertNull(viewModel.isDarkMode.value)
    }

    @Test
    fun `toggleDarkMode cycles null to true to false to null`() {
        // null → true
        viewModel.toggleDarkMode()
        assertEquals(true, viewModel.isDarkMode.value)

        // true → false
        viewModel.toggleDarkMode()
        assertEquals(false, viewModel.isDarkMode.value)

        // false → null
        viewModel.toggleDarkMode()
        assertNull(viewModel.isDarkMode.value)
    }

    // -------- getExpenseById / getSplitsForExpense tests --------

    @Test
    fun `getExpenseById delegates to dao`() = runTest {
        val expense = com.hativ2.data.entity.ExpenseEntity("e1", "d1", "Test", 10.0, "u1", "Food", 1000L)
        whenever(expenseDao.getExpenseById("e1")).thenReturn(expense)

        val result = viewModel.getExpenseById("e1")
        assertEquals(expense, result)
    }

    @Test
    fun `getExpenseById returns null for nonexistent expense`() = runTest {
        whenever(expenseDao.getExpenseById("nonexistent")).thenReturn(null)

        val result = viewModel.getExpenseById("nonexistent")
        assertNull(result)
    }

    @Test
    fun `getSplitsForExpense delegates to dao`() = runTest {
        val splits = listOf(
            com.hativ2.data.entity.SplitEntity("s1", "e1", "u1", 50.0),
            com.hativ2.data.entity.SplitEntity("s2", "e1", "u2", 50.0)
        )
        whenever(expenseDao.getSplitsForExpense("e1")).thenReturn(splits)

        val result = viewModel.getSplitsForExpense("e1")
        assertEquals(2, result.size)
        assertEquals(splits, result)
    }
}
