package com.hativ2.data.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hativ2.data.AppDatabase
import com.hativ2.data.entity.DashboardEntity
import com.hativ2.data.entity.ExpenseEntity
import com.hativ2.data.entity.PersonEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class ExpenseDaoTest {
    private lateinit var expenseDao: ExpenseDao
    private lateinit var dashboardDao: DashboardDao
    private lateinit var personDao: PersonDao
    private lateinit var db: AppDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context, AppDatabase::class.java
        ).build()
        expenseDao = db.expenseDao()
        dashboardDao = db.dashboardDao()
        personDao = db.personDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    private suspend fun setupDependencies(dashboardId: String, personId: String) {
        val dashboard = DashboardEntity(
            id = dashboardId,
            title = "Test Dashboard",
            coverImageUrl = null,
            currencySymbol = "$",
            themeColor = "#FFFFFF",
            dashboardType = "personal",
            createdAt = System.currentTimeMillis(),
            order = 0
        )
        dashboardDao.insertDashboard(dashboard)

        val person = PersonEntity(
            id = personId,
            name = "Test User",
            avatarColor = "#000000",
            createdAt = System.currentTimeMillis()
        )
        personDao.insertPerson(person)
    }

    @Test
    @Throws(Exception::class)
    fun writeUserAndReadInList() = runBlocking {
        val dashboardId = "dash-1"
        val personId = "user-1"
        setupDependencies(dashboardId, personId)

        val expense = ExpenseEntity(
            id = "expense-1",
            dashboardId = dashboardId,
            description = "Test Expense",
            amount = 100.0,
            paidBy = personId,
            category = "Food",
            createdAt = System.currentTimeMillis()
        )
        
        expenseDao.insertExpense(expense)
        
        val expenses = expenseDao.getExpensesForDashboard(dashboardId).first()
        assertEquals(expenses[0].description, "Test Expense")
    }

    @Test
    fun getExpensesByDateRangeReturnsCorrectExpenses() = runBlocking {
        val dashboardId = "dash-1"
        val personId = "u1"
        setupDependencies(dashboardId, personId)

        val expense1 = ExpenseEntity("1", dashboardId, "Exp 1", 10.0, personId, "Food", 1000)
        val expense2 = ExpenseEntity("2", dashboardId, "Exp 2", 20.0, personId, "Food", 2000)
        val expense3 = ExpenseEntity("3", dashboardId, "Exp 3", 30.0, personId, "Food", 3000)

        expenseDao.insertExpense(expense1)
        expenseDao.insertExpense(expense2)
        expenseDao.insertExpense(expense3)

        // Range covers expense 2 only
        val result = expenseDao.getExpensesByDateRange(dashboardId, 1500, 2500).first()
        
        assertEquals(1, result.size)
        assertEquals("Exp 2", result[0].description)
    }
}
