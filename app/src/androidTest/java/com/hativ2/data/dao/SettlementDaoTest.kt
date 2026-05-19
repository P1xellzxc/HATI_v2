package com.hativ2.data.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hativ2.data.AppDatabase
import com.hativ2.data.entity.DashboardEntity
import com.hativ2.data.entity.ExpenseEntity
import com.hativ2.data.entity.PersonEntity
import com.hativ2.data.entity.SettlementEntity
import com.hativ2.data.entity.SplitEntity
import com.hativ2.domain.usecase.CalculateDebtsUseCase
import com.hativ2.domain.usecase.DebtStrategy
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class SettlementDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var expenseDao: ExpenseDao
    private lateinit var dashboardDao: DashboardDao
    private lateinit var personDao: PersonDao
    private val calculateDebts = CalculateDebtsUseCase()

    private val dashId = "dash-1"
    private val aliceId = "person-alice"
    private val bobId = "person-bob"

    @Before
    fun createDb() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        expenseDao = db.expenseDao()
        dashboardDao = db.dashboardDao()
        personDao = db.personDao()

        // Seed required FK records
        dashboardDao.insertDashboard(
            DashboardEntity(dashId, "Test", null, 50, 1.0f, "$", "#000", "other", 0L, 0)
        )
        personDao.insertPerson(PersonEntity(aliceId, "Alice", "#FF0000", 0L))
        personDao.insertPerson(PersonEntity(bobId, "Bob", "#00FF00", 0L))
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    // -------- Insert + query --------

    @Test
    fun insertSettlement_thenQueryByDashboard_returnsSettlement() = runBlocking {
        val settlement = SettlementEntity("s1", dashId, bobId, aliceId, 50.0, 1000L)
        expenseDao.insertSettlement(settlement)

        val loaded = expenseDao.getSettlementsForDashboard(dashId).first()

        assertEquals(1, loaded.size)
        assertEquals(50.0, loaded[0].amount, 0.001)
        assertEquals(bobId, loaded[0].fromId)
        assertEquals(aliceId, loaded[0].toId)
    }

    @Test
    fun insertMultipleSettlements_orderedByCreatedAtDesc() = runBlocking {
        expenseDao.insertSettlement(SettlementEntity("s1", dashId, bobId, aliceId, 10.0, 1000L))
        expenseDao.insertSettlement(SettlementEntity("s2", dashId, bobId, aliceId, 20.0, 2000L))
        expenseDao.insertSettlement(SettlementEntity("s3", dashId, bobId, aliceId, 30.0, 3000L))

        val loaded = expenseDao.getSettlementsForDashboard(dashId).first()

        assertEquals(3, loaded.size)
        assertEquals(30.0, loaded[0].amount, 0.001) // Most recent first
        assertEquals(20.0, loaded[1].amount, 0.001)
        assertEquals(10.0, loaded[2].amount, 0.001)
    }

    // -------- Delete --------

    @Test
    fun deleteSettlement_removesFromList() = runBlocking {
        expenseDao.insertSettlement(SettlementEntity("s1", dashId, bobId, aliceId, 100.0, 0L))
        expenseDao.deleteSettlement("s1")

        val loaded = expenseDao.getSettlementsForDashboard(dashId).first()
        assertTrue(loaded.isEmpty())
    }

    // -------- Settlement affects debt calculation --------

    @Test
    fun settlement_reducesDebtInCalculation() = runBlocking {
        // Alice pays for Bob: 100
        val expId = "exp1"
        expenseDao.saveExpenseWithSplits(
            ExpenseEntity(expId, dashId, "Dinner", 100.0, aliceId, "Food", 0L),
            listOf(SplitEntity("sp1", expId, bobId, 100.0))
        )

        // Before settlement: Bob owes Alice 100
        val allExpenses = expenseDao.getExpensesForDashboard(dashId).first()
        val allSplits = expenseDao.getSplitsForDashboard(dashId).first()
        val before = calculateDebts.execute(
            expenses = allExpenses,
            splits = allSplits,
            userIds = listOf(aliceId, bobId),
            currentUserId = aliceId
        )
        assertEquals(100.0, before.totalOwedToYou, 0.01)

        // Bob partially settles 60
        expenseDao.insertSettlement(SettlementEntity("s1", dashId, bobId, aliceId, 60.0, 1000L))
        val allSettlements = expenseDao.getSettlementsForDashboard(dashId).first()

        val after = calculateDebts.execute(
            expenses = allExpenses,
            splits = allSplits,
            settlements = allSettlements,
            userIds = listOf(aliceId, bobId),
            currentUserId = aliceId
        )

        assertEquals(40.0, after.totalOwedToYou, 0.01)
    }

    @Test
    fun full_settlement_zeroes_out_all_transactions() = runBlocking {
        val expId = "exp1"
        expenseDao.saveExpenseWithSplits(
            ExpenseEntity(expId, dashId, "Groceries", 80.0, aliceId, "Food", 0L),
            listOf(SplitEntity("sp1", expId, bobId, 80.0))
        )
        expenseDao.insertSettlement(SettlementEntity("s1", dashId, bobId, aliceId, 80.0, 0L))

        val expenses = expenseDao.getExpensesForDashboard(dashId).first()
        val splits = expenseDao.getSplitsForDashboard(dashId).first()
        val settlements = expenseDao.getSettlementsForDashboard(dashId).first()

        val result = calculateDebts.execute(
            expenses = expenses,
            splits = splits,
            settlements = settlements,
            userIds = listOf(aliceId, bobId),
            currentUserId = aliceId
        )

        assertTrue("No transactions expected after full settlement", result.transactions.isEmpty())
        assertEquals(0.0, result.totalOwedToYou, 0.01)
        assertEquals(0.0, result.totalYouOwe, 0.01)
    }

    // -------- Cascade: settlement deleted when person deleted --------

    @Test
    fun deleteFromPerson_cascadesSettlementsReferencing() = runBlocking {
        expenseDao.insertSettlement(SettlementEntity("s1", dashId, bobId, aliceId, 50.0, 0L))

        // Delete Bob (fromId of the settlement)
        personDao.deletePerson(bobId)

        val remaining = expenseDao.getSettlementsForDashboard(dashId).first()
        assertTrue("Settlement must be deleted when the paying person is deleted", remaining.isEmpty())
    }
}
