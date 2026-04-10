package com.hativ2.domain.usecase

import com.hativ2.data.dao.DashboardDao
import com.hativ2.data.dao.ExpenseDao
import com.hativ2.data.entity.DashboardEntity
import com.hativ2.data.entity.ExpenseEntity
import com.hativ2.data.entity.PersonEntity
import com.hativ2.data.entity.SettlementEntity
import com.hativ2.data.entity.SplitEntity
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever

class ExportDashboardJsonUseCaseTest {

    @Mock
    private lateinit var expenseDao: ExpenseDao

    @Mock
    private lateinit var dashboardDao: DashboardDao

    private lateinit var useCase: ExportDashboardJsonUseCase

    private val testDashboard = DashboardEntity(
        id = "dash-1",
        title = "Trip",
        coverImageUrl = null,
        coverImageOffsetY = 50,
        coverImageZoom = 1.0f,
        currencySymbol = "$",
        themeColor = "#FF0000",
        dashboardType = "travel",
        createdAt = 1609459200000L,
        order = 0
    )

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        useCase = ExportDashboardJsonUseCase(expenseDao, dashboardDao)
    }

    private suspend fun setupEmptyDashboard() {
        whenever(dashboardDao.getDashboardById("dash-1")).thenReturn(testDashboard)
        whenever(expenseDao.getExpensesForDashboard("dash-1")).thenReturn(flowOf(emptyList()))
        whenever(expenseDao.getSettlementsForDashboard("dash-1")).thenReturn(flowOf(emptyList()))
        whenever(dashboardDao.getDashboardMembers("dash-1")).thenReturn(flowOf(emptyList()))
        whenever(expenseDao.getSplitsForDashboard("dash-1")).thenReturn(flowOf(emptyList()))
    }

    @Test
    fun `execute returns valid JSON with export metadata`() = runTest {
        setupEmptyDashboard()

        val json = useCase.execute("dash-1")
        val root = JSONObject(json)

        assertEquals(1, root.getInt("exportVersion"))
        assertTrue(root.has("exportedAt"))
    }

    @Test
    fun `execute includes dashboard metadata`() = runTest {
        setupEmptyDashboard()

        val json = useCase.execute("dash-1")
        val root = JSONObject(json)
        val dashboard = root.getJSONObject("dashboard")

        assertEquals("dash-1", dashboard.getString("id"))
        assertEquals("Trip", dashboard.getString("title"))
        assertEquals("travel", dashboard.getString("type"))
        assertEquals("$", dashboard.getString("currencySymbol"))
        assertEquals("#FF0000", dashboard.getString("themeColor"))
    }

    @Test
    fun `execute includes members array`() = runTest {
        val people = listOf(
            PersonEntity("u1", "Alice", "blue", 1000L),
            PersonEntity("u2", "Bob", "red", 1000L)
        )

        whenever(dashboardDao.getDashboardById("dash-1")).thenReturn(testDashboard)
        whenever(expenseDao.getExpensesForDashboard("dash-1")).thenReturn(flowOf(emptyList()))
        whenever(expenseDao.getSettlementsForDashboard("dash-1")).thenReturn(flowOf(emptyList()))
        whenever(dashboardDao.getDashboardMembers("dash-1")).thenReturn(flowOf(people))
        whenever(expenseDao.getSplitsForDashboard("dash-1")).thenReturn(flowOf(emptyList()))

        val json = useCase.execute("dash-1")
        val root = JSONObject(json)
        val members = root.getJSONArray("members")

        assertEquals(2, members.length())
        assertEquals("Alice", members.getJSONObject(0).getString("name"))
        assertEquals("Bob", members.getJSONObject(1).getString("name"))
    }

    @Test
    fun `execute includes expenses with splits`() = runTest {
        val people = listOf(
            PersonEntity("u1", "Alice", "blue", 1000L),
            PersonEntity("u2", "Bob", "red", 1000L)
        )
        val expenses = listOf(
            ExpenseEntity("e1", "dash-1", "Dinner", 200.0, "u1", "Food", 1609459200000L)
        )
        val splits = listOf(
            SplitEntity("s1", "e1", "u1", 100.0),
            SplitEntity("s2", "e1", "u2", 100.0)
        )

        whenever(dashboardDao.getDashboardById("dash-1")).thenReturn(testDashboard)
        whenever(expenseDao.getExpensesForDashboard("dash-1")).thenReturn(flowOf(expenses))
        whenever(expenseDao.getSettlementsForDashboard("dash-1")).thenReturn(flowOf(emptyList()))
        whenever(dashboardDao.getDashboardMembers("dash-1")).thenReturn(flowOf(people))
        whenever(expenseDao.getSplitsForDashboard("dash-1")).thenReturn(flowOf(splits))

        val json = useCase.execute("dash-1")
        val root = JSONObject(json)
        val expensesArray = root.getJSONArray("expenses")

        assertEquals(1, expensesArray.length())

        val expense = expensesArray.getJSONObject(0)
        assertEquals("e1", expense.getString("id"))
        assertEquals("Dinner", expense.getString("description"))
        assertEquals(200.0, expense.getDouble("amount"), 0.0)
        assertEquals("Alice", expense.getString("paidByName"))

        val splitsArray = expense.getJSONArray("splits")
        assertEquals(2, splitsArray.length())
    }

    @Test
    fun `execute includes settlements`() = runTest {
        val people = listOf(
            PersonEntity("u1", "Alice", "blue", 1000L),
            PersonEntity("u2", "Bob", "red", 1000L)
        )
        val settlements = listOf(
            SettlementEntity("set-1", "dash-1", "u2", "u1", 50.0, 1609459200000L)
        )

        whenever(dashboardDao.getDashboardById("dash-1")).thenReturn(testDashboard)
        whenever(expenseDao.getExpensesForDashboard("dash-1")).thenReturn(flowOf(emptyList()))
        whenever(expenseDao.getSettlementsForDashboard("dash-1")).thenReturn(flowOf(settlements))
        whenever(dashboardDao.getDashboardMembers("dash-1")).thenReturn(flowOf(people))
        whenever(expenseDao.getSplitsForDashboard("dash-1")).thenReturn(flowOf(emptyList()))

        val json = useCase.execute("dash-1")
        val root = JSONObject(json)
        val settlementsArray = root.getJSONArray("settlements")

        assertEquals(1, settlementsArray.length())
        val settlement = settlementsArray.getJSONObject(0)
        assertEquals("set-1", settlement.getString("id"))
        assertEquals("Bob", settlement.getString("fromName"))
        assertEquals("Alice", settlement.getString("toName"))
        assertEquals(50.0, settlement.getDouble("amount"), 0.0)
    }

    @Test
    fun `execute includes summary stats`() = runTest {
        val people = listOf(PersonEntity("u1", "Alice", "blue", 1000L))
        val expenses = listOf(
            ExpenseEntity("e1", "dash-1", "Lunch", 100.0, "u1", "Food", 1000L),
            ExpenseEntity("e2", "dash-1", "Dinner", 200.0, "u1", "Food", 2000L)
        )
        val settlements = listOf(
            SettlementEntity("set-1", "dash-1", "u1", "u1", 50.0, 3000L)
        )

        whenever(dashboardDao.getDashboardById("dash-1")).thenReturn(testDashboard)
        whenever(expenseDao.getExpensesForDashboard("dash-1")).thenReturn(flowOf(expenses))
        whenever(expenseDao.getSettlementsForDashboard("dash-1")).thenReturn(flowOf(settlements))
        whenever(dashboardDao.getDashboardMembers("dash-1")).thenReturn(flowOf(people))
        whenever(expenseDao.getSplitsForDashboard("dash-1")).thenReturn(flowOf(emptyList()))

        val json = useCase.execute("dash-1")
        val root = JSONObject(json)
        val summary = root.getJSONObject("summary")

        assertEquals(2, summary.getInt("totalExpenses"))
        assertEquals(1, summary.getInt("totalSettlements"))
        assertEquals(300.0, summary.getDouble("totalSpending"), 0.0)
        assertEquals(1, summary.getInt("memberCount"))
    }

    @Test
    fun `execute resolves current user to You`() = runTest {
        val people = listOf(
            PersonEntity("user-current", "Current User", "default", 1000L)
        )
        val expenses = listOf(
            ExpenseEntity("e1", "dash-1", "Coffee", 5.0, "user-current", "Drinks", 1000L)
        )
        val splits = listOf(
            SplitEntity("s1", "e1", "user-current", 5.0)
        )

        whenever(dashboardDao.getDashboardById("dash-1")).thenReturn(testDashboard)
        whenever(expenseDao.getExpensesForDashboard("dash-1")).thenReturn(flowOf(expenses))
        whenever(expenseDao.getSettlementsForDashboard("dash-1")).thenReturn(flowOf(emptyList()))
        whenever(dashboardDao.getDashboardMembers("dash-1")).thenReturn(flowOf(people))
        whenever(expenseDao.getSplitsForDashboard("dash-1")).thenReturn(flowOf(splits))

        val json = useCase.execute("dash-1")
        val root = JSONObject(json)
        val expense = root.getJSONArray("expenses").getJSONObject(0)
        assertEquals("You", expense.getString("paidByName"))

        val split = expense.getJSONArray("splits").getJSONObject(0)
        assertEquals("You", split.getString("personName"))
    }

    @Test
    fun `execute handles null dashboard gracefully`() = runTest {
        whenever(dashboardDao.getDashboardById("nonexistent")).thenReturn(null)
        whenever(expenseDao.getExpensesForDashboard("nonexistent")).thenReturn(flowOf(emptyList()))
        whenever(expenseDao.getSettlementsForDashboard("nonexistent")).thenReturn(flowOf(emptyList()))
        whenever(dashboardDao.getDashboardMembers("nonexistent")).thenReturn(flowOf(emptyList()))
        whenever(expenseDao.getSplitsForDashboard("nonexistent")).thenReturn(flowOf(emptyList()))

        val json = useCase.execute("nonexistent")
        val root = JSONObject(json)
        // Should still produce valid JSON with empty dashboard object
        assertTrue(root.has("dashboard"))
        assertEquals(0, root.getJSONArray("expenses").length())
    }
}
