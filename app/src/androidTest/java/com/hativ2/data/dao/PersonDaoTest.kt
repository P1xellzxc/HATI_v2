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
class PersonDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var personDao: PersonDao
    private lateinit var dashboardDao: DashboardDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        personDao = db.personDao()
        dashboardDao = db.dashboardDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    // -------- Insert + query --------

    @Test
    fun insertPerson_thenGetById_returnsCorrectPerson() = runBlocking {
        val person = makePerson("p1", "Alice", "#FF0000")
        personDao.insertPerson(person)

        val loaded = personDao.getPersonById("p1")

        assertNotNull(loaded)
        assertEquals("Alice", loaded!!.name)
        assertEquals("#FF0000", loaded.avatarColor)
    }

    @Test
    fun getPersonById_missingId_returnsNull() = runBlocking {
        val result = personDao.getPersonById("nonexistent")
        assertNull(result)
    }

    @Test
    fun getAllPeople_orderedAlphabetically() = runBlocking {
        personDao.insertPerson(makePerson("p3", "Charlie", "#0000FF"))
        personDao.insertPerson(makePerson("p1", "Alice", "#FF0000"))
        personDao.insertPerson(makePerson("p2", "Bob", "#00FF00"))

        val all = personDao.getAllPeople().first()

        assertEquals(3, all.size)
        assertEquals("Alice", all[0].name)
        assertEquals("Bob", all[1].name)
        assertEquals("Charlie", all[2].name)
    }

    // -------- Update --------

    @Test
    fun updatePerson_changesNameAndAvatarColor() = runBlocking {
        val person = makePerson("p1", "Alice", "#FF0000")
        personDao.insertPerson(person)

        personDao.updatePerson(person.copy(name = "Alicia", avatarColor = "#ABCDEF"))

        val loaded = personDao.getPersonById("p1")
        assertEquals("Alicia", loaded!!.name)
        assertEquals("#ABCDEF", loaded.avatarColor)
    }

    // -------- Delete --------

    @Test
    fun deletePerson_removesFromAllPeople() = runBlocking {
        personDao.insertPerson(makePerson("p1", "Alice", "#FF0000"))
        personDao.insertPerson(makePerson("p2", "Bob", "#00FF00"))

        personDao.deletePerson("p1")

        val all = personDao.getAllPeople().first()
        assertEquals(1, all.size)
        assertEquals("Bob", all[0].name)
    }

    @Test
    fun deletePerson_thenGetById_returnsNull() = runBlocking {
        personDao.insertPerson(makePerson("p1", "Alice", "#FF0000"))
        personDao.deletePerson("p1")

        val result = personDao.getPersonById("p1")
        assertNull(result)
    }

    // -------- Dashboard membership --------

    @Test
    fun getDashboardMembers_onlyReturnsPersonsInThatDashboard() = runBlocking {
        val dash1 = "dash-1"
        val dash2 = "dash-2"
        dashboardDao.insertDashboard(makeDashboard(dash1))
        dashboardDao.insertDashboard(makeDashboard(dash2))

        personDao.insertPerson(makePerson("p1", "Alice", "#FF0000"))
        personDao.insertPerson(makePerson("p2", "Bob", "#00FF00"))
        personDao.insertPerson(makePerson("p3", "Charlie", "#0000FF"))

        dashboardDao.addMember(DashboardMemberEntity(dash1, "p1", 0L))
        dashboardDao.addMember(DashboardMemberEntity(dash1, "p2", 0L))
        dashboardDao.addMember(DashboardMemberEntity(dash2, "p3", 0L))

        val dash1Members = dashboardDao.getDashboardMembers(dash1).first()
        val dash2Members = dashboardDao.getDashboardMembers(dash2).first()

        assertEquals(2, dash1Members.size)
        assertTrue(dash1Members.map { it.name }.containsAll(listOf("Alice", "Bob")))
        assertEquals(1, dash2Members.size)
        assertEquals("Charlie", dash2Members[0].name)
    }

    // -------- Replace on duplicate insert --------

    @Test
    fun insertPerson_withDuplicateId_replacesExisting() = runBlocking {
        personDao.insertPerson(makePerson("p1", "Alice", "#FF0000"))
        personDao.insertPerson(makePerson("p1", "Alice Updated", "#ABCDEF"))

        val loaded = personDao.getPersonById("p1")
        assertEquals("Alice Updated", loaded!!.name)

        val all = personDao.getAllPeople().first()
        assertEquals("Should only have 1 person after replace", 1, all.size)
    }

    // -------- Helpers --------

    private fun makePerson(id: String, name: String, avatarColor: String) = PersonEntity(
        id = id,
        name = name,
        avatarColor = avatarColor,
        createdAt = System.currentTimeMillis()
    )

    private fun makeDashboard(id: String) = DashboardEntity(
        id = id,
        title = "Test Dashboard",
        coverImageUrl = null,
        currencySymbol = "$",
        themeColor = "#000000",
        dashboardType = "other",
        createdAt = System.currentTimeMillis(),
        order = 0
    )
}
