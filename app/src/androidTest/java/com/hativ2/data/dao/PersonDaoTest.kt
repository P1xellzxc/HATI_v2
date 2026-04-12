package com.hativ2.data.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hativ2.data.AppDatabase
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
    private lateinit var personDao: PersonDao
    private lateinit var db: AppDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context, AppDatabase::class.java
        ).build()
        personDao = db.personDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    private fun makePerson(
        id: String,
        name: String = "Person $id",
        avatarColor: String = "#000000"
    ) = PersonEntity(
        id = id,
        name = name,
        avatarColor = avatarColor,
        createdAt = System.currentTimeMillis()
    )

    // ── insertPerson + getAllPeople ──────────────────────────────

    @Test
    fun insertAndGetAllPeople() = runBlocking {
        personDao.insertPerson(makePerson("p1", name = "Charlie"))
        personDao.insertPerson(makePerson("p2", name = "Alice"))
        personDao.insertPerson(makePerson("p3", name = "Bob"))

        val result = personDao.getAllPeople().first()
        assertEquals(3, result.size)
        // Ordered by name ASC
        assertEquals("Alice", result[0].name)
        assertEquals("Bob", result[1].name)
        assertEquals("Charlie", result[2].name)
    }

    @Test
    fun getAllPeopleReturnsEmptyListWhenNone() = runBlocking {
        val result = personDao.getAllPeople().first()
        assertTrue(result.isEmpty())
    }

    // ── getPersonById ───────────────────────────────────────────

    @Test
    fun getPersonByIdReturnsCorrectPerson() = runBlocking {
        personDao.insertPerson(makePerson("p1", name = "Alice"))
        val result = personDao.getPersonById("p1")
        assertNotNull(result)
        assertEquals("Alice", result!!.name)
    }

    @Test
    fun getPersonByIdReturnsNullWhenNotFound() = runBlocking {
        val result = personDao.getPersonById("nonexistent")
        assertNull(result)
    }

    // ── updatePerson ────────────────────────────────────────────

    @Test
    fun updatePersonModifiesExistingRecord() = runBlocking {
        personDao.insertPerson(makePerson("p1", name = "Old Name"))
        val original = personDao.getPersonById("p1")!!
        personDao.updatePerson(original.copy(name = "New Name"))

        val result = personDao.getPersonById("p1")
        assertEquals("New Name", result!!.name)
    }

    @Test
    fun updatePersonPreservesOtherFields() = runBlocking {
        val person = makePerson("p1", name = "Alice", avatarColor = "#FF0000")
        personDao.insertPerson(person)
        personDao.updatePerson(person.copy(name = "Alicia"))

        val result = personDao.getPersonById("p1")!!
        assertEquals("Alicia", result.name)
        assertEquals("#FF0000", result.avatarColor)
        assertEquals(person.createdAt, result.createdAt)
    }

    // ── deletePerson ────────────────────────────────────────────

    @Test
    fun deletePersonRemovesRecord() = runBlocking {
        personDao.insertPerson(makePerson("p1"))
        personDao.deletePerson("p1")
        val result = personDao.getPersonById("p1")
        assertNull(result)
    }

    @Test
    fun deletePersonDoesNothingWhenNotFound() = runBlocking {
        personDao.insertPerson(makePerson("p1"))
        personDao.deletePerson("nonexistent")
        val result = personDao.getAllPeople().first()
        assertEquals(1, result.size)
    }

    // ── insertPerson REPLACE strategy ───────────────────────────

    @Test
    fun insertPersonWithSameIdReplacesExisting() = runBlocking {
        personDao.insertPerson(makePerson("p1", name = "V1"))
        personDao.insertPerson(makePerson("p1", name = "V2"))

        val result = personDao.getPersonById("p1")
        assertEquals("V2", result!!.name)

        val all = personDao.getAllPeople().first()
        assertEquals(1, all.size)
    }

    // ── Edge cases ──────────────────────────────────────────────

    @Test
    fun insertPersonWithSpecialCharactersInName() = runBlocking {
        personDao.insertPerson(makePerson("p1", name = "O'Brien \"The\" Test"))
        val result = personDao.getPersonById("p1")
        assertEquals("O'Brien \"The\" Test", result!!.name)
    }

    @Test
    fun insertPersonWithEmptyName() = runBlocking {
        personDao.insertPerson(makePerson("p1", name = ""))
        val result = personDao.getPersonById("p1")
        assertEquals("", result!!.name)
    }
}
