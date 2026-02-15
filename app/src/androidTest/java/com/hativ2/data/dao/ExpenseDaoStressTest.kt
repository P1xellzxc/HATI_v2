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
import java.util.UUID
import kotlin.system.measureTimeMillis

@RunWith(AndroidJUnit4::class)
class ExpenseDaoStressTest {

    private lateinit var db: AppDatabase
    private lateinit var expenseDao: ExpenseDao
    private lateinit var dashboardDao: DashboardDao
    private lateinit var personDao: PersonDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        expenseDao = db.expenseDao()
        dashboardDao = db.dashboardDao()
        personDao = db.personDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun insert1000TransactionsPerformance() = runBlocking {
        // Setup Dependencies
        val dashboardId = "dash_stress"
        val personId = "person_stress"
        
        dashboardDao.insertDashboard(
            DashboardEntity(
                id = dashboardId, 
                title = "Stress Test", 
                coverImageUrl = null,
                currencySymbol = "$",
                themeColor = "#FF0000",
                dashboardType = "other",
                createdAt = System.currentTimeMillis(),
                order = 0
            )
        )
        
        personDao.insertPerson(
            PersonEntity(
                id = personId, 
                name = "Tester", 
                avatarColor = "#00FF00",
                createdAt = System.currentTimeMillis()
            )
        )

        // Stress Test
        val count = 1000
        val time = measureTimeMillis {
            for (i in 1..count) {
                expenseDao.insertExpense(
                    ExpenseEntity(
                        id = UUID.randomUUID().toString(),
                        dashboardId = dashboardId,
                        description = "Expense $i",
                        amount = 10.0 + i,
                        paidBy = personId,
                        category = "Misc",
                        createdAt = System.currentTimeMillis()
                    )
                )
            }
        }

        println("Inserted $count expenses in $time ms")

        // Verify
        val expenses = expenseDao.getExpensesForDashboard(dashboardId).first()
        assertEquals(count, expenses.size)
    }
}
