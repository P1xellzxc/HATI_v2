package com.hativ2.data.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hativ2.data.AppDatabase
import com.hativ2.data.entity.DashboardEntity
import com.hativ2.data.entity.DashboardMemberEntity
import com.hativ2.data.entity.ExpenseEntity
import com.hativ2.data.entity.PersonEntity
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

@RunWith(AndroidJUnit4::class)
class DashboardDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dashboardDao: DashboardDao
    private lateinit var personDao: PersonDao
    private lateinit var expenseDao: ExpenseDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        dashboardDao = db.dashboardDao()
        personDao = db.personDao()
        expenseDao = db.expenseDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    // -------- Insert + query --------

    @Test
    fun insertDashboard_thenGetById_returnsCorrectDashboard() = runBlocking {
        val dashboard = makeDashboard("dash-1", "Family Trip", "$")
        dashboardDao.insertDashboard(dashboard)

        val loaded = dashboardDao.getDashboardById("dash-1")

        assertNotNull(loaded)
        assertEquals("Family Trip", loaded!!.title)
        assertEquals("$", loaded.currencySymbol)
    }

    @Test
    fun getDashboardById_missingId_returnsNull() = runBlocking {
        val result = dashboardDao.getDashboardById("nonexistent")
        assertNull(result)
    }

    @Test
    fun getAllDashboards_orderedByOrderField() = runBlocking {
        dashboardDao.insertDashboard(makeDashboard("d2", "Second", "$", order = 1))
        dashboardDao.insertDashboard(makeDashboard("d1", "First", "$", order = 0))
        dashboardDao.insertDashboard(makeDashboard("d3", "Third", "$", order = 2))

        val all = dashboardDao.getAllDashboards().first()

        assertEquals(3, all.size)
        assertEquals("First", all[0].title)
        assertEquals("Second", all[1].title)
        assertEquals("Third", all[2].title)
    }

    // -------- Update --------

    @Test
    fun updateDashboard_changesTitle() = runBlocking {
        val dashboard = makeDashboard("dash-1", "Old Title", "€")
        dashboardDao.insertDashboard(dashboard)

        dashboardDao.updateDashboard(dashboard.copy(title = "New Title"))

        val loaded = dashboardDao.getDashboardById("dash-1")
        assertEquals("New Title", loaded!!.title)
    }

    // -------- Cascade delete --------

    @Test
    fun deleteDashboard_cascadesExpenses() = runBlocking {
        val dashId = "dash-cascade"
        val personId = "person-1"

        dashboardDao.insertDashboard(makeDashboard(dashId, "Cascade Test", "$"))
        personDao.insertPerson(makePerson(personId, "Alice"))
        expenseDao.insertExpense(makeExpense("exp-1", dashId, personId))

        // Verify expense exists before deletion
        val before = expenseDao.getExpensesForDashboard(dashId).first()
        assertEquals(1, before.size)

        dashboardDao.deleteDashboard(dashId)

        val after = expenseDao.getExpensesForDashboard(dashId).first()
        assertTrue("Expenses must be deleted when dashboard is deleted", after.isEmpty())
    }

    @Test
    fun deleteDashboard_cascadesMemberships() = runBlocking {
        val dashId = "dash-member"
        val personId = "person-1"

        dashboardDao.insertDashboard(makeDashboard(dashId, "Member Test", "$"))
        personDao.insertPerson(makePerson(personId, "Bob"))
        dashboardDao.addMember(DashboardMemberEntity(dashId, personId, System.currentTimeMillis()))

        // Verify member exists
        val membersBefore = dashboardDao.getDashboardMembers(dashId).first()
        assertEquals(1, membersBefore.size)

        dashboardDao.deleteDashboard(dashId)

        val membersAfter = dashboardDao.getDashboardMembers(dashId).first()
        assertTrue("Members must be removed when dashboard is deleted", membersAfter.isEmpty())
    }

    // -------- Member management --------

    @Test
    fun addMember_thenGetDashboardMembers_includesMember() = runBlocking {
        val dashId = "dash-1"
        val personId = "person-alice"

        dashboardDao.insertDashboard(makeDashboard(dashId, "Shared Trip", "$"))
        personDao.insertPerson(makePerson(personId, "Alice"))
        dashboardDao.addMember(DashboardMemberEntity(dashId, personId, System.currentTimeMillis()))

        val members = dashboardDao.getDashboardMembers(dashId).first()

        assertEquals(1, members.size)
        assertEquals("Alice", members[0].name)
    }

    @Test
    fun removeMember_removesOnlyTargetPerson() = runBlocking {
        val dashId = "dash-1"
        val aliceId = "person-alice"
        val bobId = "person-bob"

        dashboardDao.insertDashboard(makeDashboard(dashId, "Trip", "$"))
        personDao.insertPerson(makePerson(aliceId, "Alice"))
        personDao.insertPerson(makePerson(bobId, "Bob"))
        dashboardDao.addMember(DashboardMemberEntity(dashId, aliceId, 0L))
        dashboardDao.addMember(DashboardMemberEntity(dashId, bobId, 0L))

        dashboardDao.removeMember(dashId, aliceId)

        val remaining = dashboardDao.getDashboardMembers(dashId).first()
        assertEquals(1, remaining.size)
        assertEquals("Bob", remaining[0].name)
    }

    @Test
    fun addMember_withDuplicateKey_replacesExisting() = runBlocking {
        val dashId = "dash-1"
        val personId = "person-1"

        dashboardDao.insertDashboard(makeDashboard(dashId, "Trip", "$"))
        personDao.insertPerson(makePerson(personId, "Alice"))
        dashboardDao.addMember(DashboardMemberEntity(dashId, personId, 100L))
        dashboardDao.addMember(DashboardMemberEntity(dashId, personId, 200L)) // same key, new timestamp

        val members = dashboardDao.getDashboardMembers(dashId).first()
        assertEquals("Should still be 1 member after duplicate insert", 1, members.size)
    }

    // -------- Helpers --------

    private fun makeDashboard(id: String, title: String, currency: String, order: Int = 0) = DashboardEntity(
        id = id,
        title = title,
        coverImageUrl = null,
        currencySymbol = currency,
        themeColor = "#FF0000",
        dashboardType = "other",
        createdAt = System.currentTimeMillis(),
        order = order
    )

    private fun makePerson(id: String, name: String) = PersonEntity(
        id = id,
        name = name,
        avatarColor = "#AABBCC",
        createdAt = System.currentTimeMillis()
    )

    private fun makeExpense(id: String, dashboardId: String, personId: String) = ExpenseEntity(
        id = id,
        dashboardId = dashboardId,
        description = "Test Expense",
        amount = 50.0,
        paidBy = personId,
        category = "Food",
        createdAt = System.currentTimeMillis()
    )
}
