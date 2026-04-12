package com.hativ2.data.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hativ2.data.AppDatabase
import com.hativ2.data.entity.DashboardEntity
import com.hativ2.data.entity.DashboardMemberEntity
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
    private lateinit var dashboardDao: DashboardDao
    private lateinit var personDao: PersonDao
    private lateinit var db: AppDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context, AppDatabase::class.java
        ).build()
        dashboardDao = db.dashboardDao()
        personDao = db.personDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    private fun makeDashboard(
        id: String,
        title: String = "Dashboard $id",
        order: Int = 0
    ) = DashboardEntity(
        id = id,
        title = title,
        coverImageUrl = null,
        currencySymbol = "$",
        themeColor = "#FFFFFF",
        dashboardType = "travel",
        createdAt = System.currentTimeMillis(),
        order = order
    )

    private fun makePerson(id: String, name: String = "Person $id") = PersonEntity(
        id = id,
        name = name,
        avatarColor = "#000000",
        createdAt = System.currentTimeMillis()
    )

    // ── insertDashboard + getAllDashboards ───────────────────────

    @Test
    fun insertAndGetAllDashboards() = runBlocking {
        dashboardDao.insertDashboard(makeDashboard("d1", order = 1))
        dashboardDao.insertDashboard(makeDashboard("d2", order = 0))

        val result = dashboardDao.getAllDashboards().first()
        assertEquals(2, result.size)
        // Ordered by `order` ASC
        assertEquals("d2", result[0].id)
        assertEquals("d1", result[1].id)
    }

    @Test
    fun getAllDashboardsReturnsEmptyListWhenNone() = runBlocking {
        val result = dashboardDao.getAllDashboards().first()
        assertTrue(result.isEmpty())
    }

    // ── getDashboardById ────────────────────────────────────────

    @Test
    fun getDashboardByIdReturnsCorrectDashboard() = runBlocking {
        dashboardDao.insertDashboard(makeDashboard("d1", title = "My Trip"))
        val result = dashboardDao.getDashboardById("d1")
        assertNotNull(result)
        assertEquals("My Trip", result!!.title)
    }

    @Test
    fun getDashboardByIdReturnsNullWhenNotFound() = runBlocking {
        val result = dashboardDao.getDashboardById("nonexistent")
        assertNull(result)
    }

    // ── updateDashboard ─────────────────────────────────────────

    @Test
    fun updateDashboardModifiesExistingRecord() = runBlocking {
        val original = makeDashboard("d1", title = "Old Title")
        dashboardDao.insertDashboard(original)

        val updated = original.copy(title = "New Title")
        dashboardDao.updateDashboard(updated)

        val result = dashboardDao.getDashboardById("d1")
        assertEquals("New Title", result!!.title)
    }

    // ── deleteDashboard ─────────────────────────────────────────

    @Test
    fun deleteDashboardRemovesRecord() = runBlocking {
        dashboardDao.insertDashboard(makeDashboard("d1"))
        dashboardDao.deleteDashboard("d1")
        val result = dashboardDao.getDashboardById("d1")
        assertNull(result)
    }

    @Test
    fun deleteDashboardDoesNothingWhenNotFound() = runBlocking {
        dashboardDao.insertDashboard(makeDashboard("d1"))
        dashboardDao.deleteDashboard("nonexistent")
        val result = dashboardDao.getAllDashboards().first()
        assertEquals(1, result.size)
    }

    // ── insertDashboard REPLACE strategy ────────────────────────

    @Test
    fun insertDashboardWithSameIdReplacesExisting() = runBlocking {
        dashboardDao.insertDashboard(makeDashboard("d1", title = "V1"))
        dashboardDao.insertDashboard(makeDashboard("d1", title = "V2"))

        val result = dashboardDao.getDashboardById("d1")
        assertEquals("V2", result!!.title)

        val all = dashboardDao.getAllDashboards().first()
        assertEquals(1, all.size)
    }

    // ── addMember + getDashboardMembers ─────────────────────────

    @Test
    fun addMemberAndGetDashboardMembers() = runBlocking {
        dashboardDao.insertDashboard(makeDashboard("d1"))
        personDao.insertPerson(makePerson("p1", "Alice"))
        personDao.insertPerson(makePerson("p2", "Bob"))

        dashboardDao.addMember(DashboardMemberEntity("d1", "p1", System.currentTimeMillis()))
        dashboardDao.addMember(DashboardMemberEntity("d1", "p2", System.currentTimeMillis()))

        val members = dashboardDao.getDashboardMembers("d1").first()
        assertEquals(2, members.size)
        val names = members.map { it.name }.toSet()
        assertTrue(names.contains("Alice"))
        assertTrue(names.contains("Bob"))
    }

    @Test
    fun getDashboardMembersReturnsEmptyWhenNoneAdded() = runBlocking {
        dashboardDao.insertDashboard(makeDashboard("d1"))
        val members = dashboardDao.getDashboardMembers("d1").first()
        assertTrue(members.isEmpty())
    }

    // ── removeMember ────────────────────────────────────────────

    @Test
    fun removeMemberRemovesOnlySpecifiedMember() = runBlocking {
        dashboardDao.insertDashboard(makeDashboard("d1"))
        personDao.insertPerson(makePerson("p1", "Alice"))
        personDao.insertPerson(makePerson("p2", "Bob"))

        dashboardDao.addMember(DashboardMemberEntity("d1", "p1", System.currentTimeMillis()))
        dashboardDao.addMember(DashboardMemberEntity("d1", "p2", System.currentTimeMillis()))

        dashboardDao.removeMember("d1", "p1")

        val members = dashboardDao.getDashboardMembers("d1").first()
        assertEquals(1, members.size)
        assertEquals("Bob", members[0].name)
    }

    // ── CASCADE delete ──────────────────────────────────────────

    @Test
    fun deleteDashboardCascadeDeletesMembers() = runBlocking {
        dashboardDao.insertDashboard(makeDashboard("d1"))
        personDao.insertPerson(makePerson("p1"))
        dashboardDao.addMember(DashboardMemberEntity("d1", "p1", System.currentTimeMillis()))

        dashboardDao.deleteDashboard("d1")

        // Person still exists but membership should be gone
        assertNotNull(personDao.getPersonById("p1"))
        // Re-insert dashboard to query members (no members should remain)
        dashboardDao.insertDashboard(makeDashboard("d1"))
        val members = dashboardDao.getDashboardMembers("d1").first()
        assertTrue(members.isEmpty())
    }
}
