package com.hativ2.domain.usecase

import com.hativ2.data.dao.DashboardDao
import com.hativ2.data.dao.ExpenseDao
import com.hativ2.data.entity.ExpenseEntity
import com.hativ2.data.entity.PersonEntity
import com.hativ2.data.entity.SplitEntity
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever

class ExportDashboardCsvUseCaseTest {

    @Mock
    private lateinit var expenseDao: ExpenseDao

    @Mock
    private lateinit var dashboardDao: DashboardDao

    private lateinit var useCase: ExportDashboardCsvUseCase

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        useCase = ExportDashboardCsvUseCase(expenseDao, dashboardDao)
    }

    @Test
    fun `execute returns header only for dashboard with no expenses`() = runTest {
        whenever(expenseDao.getExpensesForDashboard("dash-1")).thenReturn(flowOf(emptyList()))
        whenever(dashboardDao.getDashboardMembers("dash-1")).thenReturn(flowOf(emptyList()))

        val csv = useCase.execute("dash-1")
        val lines = csv.trimEnd('\n').lines()
        assertEquals(1, lines.size)
        assertEquals("Date,Description,Category,Total Amount,Paid By,Split Details", lines[0])
    }

    @Test
    fun `execute includes expense rows with correct format`() = runTest {
        val people = listOf(
            PersonEntity("u1", "Alice", "blue", 1000L)
        )
        val expenses = listOf(
            ExpenseEntity("e1", "dash-1", "Lunch", 100.0, "u1", "Food", 1609459200000L)
        )
        val splits = listOf(
            SplitEntity("s1", "e1", "u1", 100.0)
        )

        whenever(expenseDao.getExpensesForDashboard("dash-1")).thenReturn(flowOf(expenses))
        whenever(dashboardDao.getDashboardMembers("dash-1")).thenReturn(flowOf(people))
        whenever(expenseDao.getSplitsForExpense("e1")).thenReturn(splits)

        val csv = useCase.execute("dash-1")
        val lines = csv.trimEnd('\n').lines()
        assertEquals(2, lines.size)

        val dataLine = lines[1]
        assertTrue(dataLine.contains("Lunch"))
        assertTrue(dataLine.contains("100.00"))
        assertTrue(dataLine.contains("Alice"))
    }

    @Test
    fun `execute resolves current user to You`() = runTest {
        val people = listOf(
            PersonEntity("user-current", "You", "default", 1000L)
        )
        val expenses = listOf(
            ExpenseEntity("e1", "dash-1", "Coffee", 5.0, "user-current", "Drinks", 1000L)
        )

        whenever(expenseDao.getExpensesForDashboard("dash-1")).thenReturn(flowOf(expenses))
        whenever(dashboardDao.getDashboardMembers("dash-1")).thenReturn(flowOf(people))
        whenever(expenseDao.getSplitsForExpense("e1")).thenReturn(
            listOf(SplitEntity("s1", "e1", "user-current", 5.0))
        )

        val csv = useCase.execute("dash-1")
        assertTrue(csv.contains("You"))
    }

    @Test
    fun `execute shows Unknown for unresolvable payer`() = runTest {
        val expenses = listOf(
            ExpenseEntity("e1", "dash-1", "Mystery", 50.0, "deleted-user", "Misc", 1000L)
        )

        whenever(expenseDao.getExpensesForDashboard("dash-1")).thenReturn(flowOf(expenses))
        whenever(dashboardDao.getDashboardMembers("dash-1")).thenReturn(flowOf(emptyList()))
        whenever(expenseDao.getSplitsForExpense("e1")).thenReturn(emptyList())

        val csv = useCase.execute("dash-1")
        assertTrue(csv.contains("Unknown"))
    }

    @Test
    fun `execute includes split details with person names and amounts`() = runTest {
        val people = listOf(
            PersonEntity("u1", "Alice", "blue", 1000L),
            PersonEntity("u2", "Bob", "red", 1000L)
        )
        val expenses = listOf(
            ExpenseEntity("e1", "dash-1", "Dinner", 200.0, "u1", "Food", 1000L)
        )
        val splits = listOf(
            SplitEntity("s1", "e1", "u1", 100.0),
            SplitEntity("s2", "e1", "u2", 100.0)
        )

        whenever(expenseDao.getExpensesForDashboard("dash-1")).thenReturn(flowOf(expenses))
        whenever(dashboardDao.getDashboardMembers("dash-1")).thenReturn(flowOf(people))
        whenever(expenseDao.getSplitsForExpense("e1")).thenReturn(splits)

        val csv = useCase.execute("dash-1")
        assertTrue(csv.contains("Alice: 100.00"))
        assertTrue(csv.contains("Bob: 100.00"))
    }

    @Test
    fun `execute escapes CSV special characters in description`() = runTest {
        val expenses = listOf(
            ExpenseEntity("e1", "dash-1", "Food, drinks, etc", 30.0, "u1", "Food", 1000L)
        )
        val people = listOf(PersonEntity("u1", "Alice", "blue", 1000L))

        whenever(expenseDao.getExpensesForDashboard("dash-1")).thenReturn(flowOf(expenses))
        whenever(dashboardDao.getDashboardMembers("dash-1")).thenReturn(flowOf(people))
        whenever(expenseDao.getSplitsForExpense("e1")).thenReturn(emptyList())

        val csv = useCase.execute("dash-1")
        // Commas in description should cause quoting
        assertTrue(csv.contains("\"Food, drinks, etc\""))
    }

    @Test
    fun `execute handles multiple expenses`() = runTest {
        val people = listOf(
            PersonEntity("u1", "Alice", "blue", 1000L)
        )
        val expenses = listOf(
            ExpenseEntity("e1", "dash-1", "Lunch", 100.0, "u1", "Food", 1000L),
            ExpenseEntity("e2", "dash-1", "Taxi", 50.0, "u1", "Transport", 2000L)
        )

        whenever(expenseDao.getExpensesForDashboard("dash-1")).thenReturn(flowOf(expenses))
        whenever(dashboardDao.getDashboardMembers("dash-1")).thenReturn(flowOf(people))
        whenever(expenseDao.getSplitsForExpense("e1")).thenReturn(emptyList())
        whenever(expenseDao.getSplitsForExpense("e2")).thenReturn(emptyList())

        val csv = useCase.execute("dash-1")
        val lines = csv.trimEnd('\n').lines()
        assertEquals(3, lines.size)
    }
}
