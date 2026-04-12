package com.hativ2.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hativ2.data.dao.DashboardDao
import com.hativ2.data.dao.ExpenseDao
import com.hativ2.data.dao.PersonDao
import com.hativ2.data.entity.DashboardEntity
import com.hativ2.data.entity.DashboardMemberEntity
import com.hativ2.data.entity.ExpenseEntity
import com.hativ2.data.entity.PersonEntity
import com.hativ2.data.entity.SettlementEntity
import com.hativ2.data.entity.SplitEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * Validates the v2 database schema: all 6 tables are created, foreign key
 * constraints are enforced, and cascade deletes work correctly.
 *
 * Note: Proper Room MigrationTestHelper tests require exported schema JSON
 * files. These tests verify the *current* schema is correct and that
 * referential integrity is enforced.
 */
@RunWith(AndroidJUnit4::class)
class DatabaseSchemaTest {
    private lateinit var db: AppDatabase
    private lateinit var dashboardDao: DashboardDao
    private lateinit var personDao: PersonDao
    private lateinit var expenseDao: ExpenseDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .build()
        dashboardDao = db.dashboardDao()
        personDao = db.personDao()
        expenseDao = db.expenseDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    private fun makeDashboard(id: String = "d1") = DashboardEntity(
        id = id, title = "Test", coverImageUrl = null,
        currencySymbol = "$", themeColor = "#FFF",
        dashboardType = "travel", createdAt = 1000L, order = 0
    )

    private fun makePerson(id: String = "p1", name: String = "User") = PersonEntity(
        id = id, name = name, avatarColor = "#000", createdAt = 1000L
    )

    // ── All 6 tables exist and accept inserts ───────────────────

    @Test
    fun allTablesAcceptInserts() = runBlocking {
        val dashboard = makeDashboard()
        val person = makePerson()
        dashboardDao.insertDashboard(dashboard)
        personDao.insertPerson(person)

        dashboardDao.addMember(DashboardMemberEntity("d1", "p1", 1000L))

        expenseDao.insertExpense(
            ExpenseEntity("e1", "d1", "Test", 50.0, "p1", "Food", 1000L)
        )

        expenseDao.insertSplits(listOf(
            SplitEntity("s1", "e1", "p1", 25.0)
        ))

        expenseDao.insertSettlement(
            SettlementEntity("st1", "d1", "p1", "p1", 10.0, 1000L)
        )

        // Verify reads
        assertNotNull(dashboardDao.getDashboardById("d1"))
        assertNotNull(personDao.getPersonById("p1"))
        val expenses = expenseDao.getExpensesForDashboard("d1").first()
        assertEquals(1, expenses.size)
        val splits = expenseDao.getSplitsForExpense("e1")
        assertEquals(1, splits.size)
        val settlements = expenseDao.getSettlementsForDashboard("d1").first()
        assertEquals(1, settlements.size)
    }

    // ── CASCADE: Deleting dashboard cascades to members, expenses, settlements

    @Test
    fun deleteDashboardCascadesToExpenses() = runBlocking {
        dashboardDao.insertDashboard(makeDashboard())
        personDao.insertPerson(makePerson())
        expenseDao.insertExpense(
            ExpenseEntity("e1", "d1", "Test", 50.0, "p1", "Food", 1000L)
        )

        dashboardDao.deleteDashboard("d1")

        // Re-insert dashboard to be able to query expenses
        dashboardDao.insertDashboard(makeDashboard())
        val expenses = expenseDao.getExpensesForDashboard("d1").first()
        assertTrue(expenses.isEmpty())
    }

    @Test
    fun deleteDashboardCascadesToSettlements() = runBlocking {
        dashboardDao.insertDashboard(makeDashboard())
        personDao.insertPerson(makePerson())
        personDao.insertPerson(makePerson("p2", "Bob"))
        expenseDao.insertSettlement(
            SettlementEntity("st1", "d1", "p1", "p2", 10.0, 1000L)
        )

        dashboardDao.deleteDashboard("d1")

        dashboardDao.insertDashboard(makeDashboard())
        val settlements = expenseDao.getSettlementsForDashboard("d1").first()
        assertTrue(settlements.isEmpty())
    }

    // ── CASCADE: Deleting expense cascades to splits ────────────

    @Test
    fun deleteExpenseCascadesToSplits() = runBlocking {
        dashboardDao.insertDashboard(makeDashboard())
        personDao.insertPerson(makePerson())
        expenseDao.insertExpense(
            ExpenseEntity("e1", "d1", "Test", 50.0, "p1", "Food", 1000L)
        )
        expenseDao.insertSplits(listOf(
            SplitEntity("s1", "e1", "p1", 25.0),
            SplitEntity("s2", "e1", "p1", 25.0)
        ))

        expenseDao.deleteExpense("e1")

        // Splits should be gone because of CASCADE
        val splits = expenseDao.getSplitsForExpense("e1")
        assertTrue(splits.isEmpty())
    }

    // ── CASCADE: Deleting person cascades to members and splits ─

    @Test
    fun deletePersonCascadesToDashboardMembers() = runBlocking {
        dashboardDao.insertDashboard(makeDashboard())
        personDao.insertPerson(makePerson())
        dashboardDao.addMember(DashboardMemberEntity("d1", "p1", 1000L))

        personDao.deletePerson("p1")

        val members = dashboardDao.getDashboardMembers("d1").first()
        assertTrue(members.isEmpty())
    }

    // ── SET NULL: Deleting person sets expense.paidBy to null ───

    @Test
    fun deletePersonSetsExpensePaidByToNull() = runBlocking {
        dashboardDao.insertDashboard(makeDashboard())
        personDao.insertPerson(makePerson())
        expenseDao.insertExpense(
            ExpenseEntity("e1", "d1", "Test", 50.0, "p1", "Food", 1000L)
        )

        personDao.deletePerson("p1")

        val expense = expenseDao.getExpenseById("e1")
        assertNotNull(expense)
        assertNull(expense!!.paidBy)
    }

    // ── saveExpenseWithSplits transactional method ──────────────

    @Test
    fun saveExpenseWithSplitsIsAtomic() = runBlocking {
        dashboardDao.insertDashboard(makeDashboard())
        personDao.insertPerson(makePerson())

        val expense = ExpenseEntity("e1", "d1", "Dinner", 100.0, "p1", "Food", 1000L)
        val splits = listOf(
            SplitEntity("s1", "e1", "p1", 50.0),
            SplitEntity("s2", "e1", "p1", 50.0)
        )

        expenseDao.saveExpenseWithSplits(expense, splits)

        val savedExpense = expenseDao.getExpenseById("e1")
        assertNotNull(savedExpense)
        val savedSplits = expenseDao.getSplitsForExpense("e1")
        assertEquals(2, savedSplits.size)
    }

    @Test
    fun saveExpenseWithSplitsReplacesOldSplits() = runBlocking {
        dashboardDao.insertDashboard(makeDashboard())
        personDao.insertPerson(makePerson())

        val expense = ExpenseEntity("e1", "d1", "Dinner", 100.0, "p1", "Food", 1000L)
        expenseDao.saveExpenseWithSplits(expense, listOf(
            SplitEntity("s1", "e1", "p1", 100.0)
        ))

        // Re-save with different splits
        expenseDao.saveExpenseWithSplits(expense, listOf(
            SplitEntity("s2", "e1", "p1", 60.0),
            SplitEntity("s3", "e1", "p1", 40.0)
        ))

        val splits = expenseDao.getSplitsForExpense("e1")
        assertEquals(2, splits.size)
    }

    // ── Database version ────────────────────────────────────────

    @Test
    fun databaseVersionIs2() {
        val version = db.openHelper.readableDatabase.version
        assertEquals(2, version)
    }
}
