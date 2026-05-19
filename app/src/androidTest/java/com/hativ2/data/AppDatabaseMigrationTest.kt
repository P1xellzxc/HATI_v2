package com.hativ2.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hativ2.data.entity.ExpenseEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Verifies that the v3 schema has the `note` and `receiptPath` columns added by MIGRATION_2_3,
 * and that those fields survive a round-trip through Room.
 *
 * A full MigrationTestHelper-based test (createDatabase(name, 2) → runMigrationsAndValidate)
 * requires pre-generated schema JSON files in app/schemas/. Those files are produced when the
 * project is first built with `./gradlew build`. Run `./gradlew build` before running this test
 * if you want to add that stricter variant.
 */
@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {

    private lateinit var db: AppDatabase

    @Before
    fun openDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .addMigrations(AppDatabase.MIGRATION_2_3)
            .build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    // -------- Schema column existence --------

    @Test
    fun expenses_table_has_note_column() {
        val sqLiteDb = db.openHelper.writableDatabase
        val cursor = sqLiteDb.query("PRAGMA table_info(expenses)")
        val columnNames = mutableListOf<String>()
        while (cursor.moveToNext()) {
            columnNames.add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
        }
        cursor.close()
        assertTrue("'note' column must exist on expenses table", "note" in columnNames)
    }

    @Test
    fun expenses_table_has_receiptPath_column() {
        val sqLiteDb = db.openHelper.writableDatabase
        val cursor = sqLiteDb.query("PRAGMA table_info(expenses)")
        val columnNames = mutableListOf<String>()
        while (cursor.moveToNext()) {
            columnNames.add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
        }
        cursor.close()
        assertTrue("'receiptPath' column must exist on expenses table", "receiptPath" in columnNames)
    }

    // -------- Round-trip: note field persists --------

    @Test
    fun expenseWithNote_survivesRoundTrip() = runBlocking {
        seedDashboardAndPerson()

        db.expenseDao().insertExpense(
            ExpenseEntity(
                id = "exp-note-1",
                dashboardId = TEST_DASH_ID,
                description = "Coffee with note",
                amount = 5.0,
                paidBy = TEST_PERSON_ID,
                category = "Food",
                createdAt = 0L,
                note = "Meeting notes here",
                receiptPath = null
            )
        )

        val loaded = db.expenseDao().getExpenseById("exp-note-1")
        assertEquals("Meeting notes here", loaded?.note)
        assertNull(loaded?.receiptPath)
    }

    @Test
    fun expenseWithReceiptPath_survivesRoundTrip() = runBlocking {
        seedDashboardAndPerson()

        db.expenseDao().insertExpense(
            ExpenseEntity(
                id = "exp-receipt-1",
                dashboardId = TEST_DASH_ID,
                description = "Taxi receipt",
                amount = 25.0,
                paidBy = TEST_PERSON_ID,
                category = "Transport",
                createdAt = 0L,
                note = null,
                receiptPath = "/data/user/0/com.hativ2/files/receipts/taxi.jpg"
            )
        )

        val loaded = db.expenseDao().getExpenseById("exp-receipt-1")
        assertNull(loaded?.note)
        assertEquals("/data/user/0/com.hativ2/files/receipts/taxi.jpg", loaded?.receiptPath)
    }

    @Test
    fun expenseWithoutNoteOrReceipt_defaultsToNull() = runBlocking {
        seedDashboardAndPerson()

        db.expenseDao().insertExpense(
            ExpenseEntity(
                id = "exp-plain-1",
                dashboardId = TEST_DASH_ID,
                description = "Plain expense",
                amount = 10.0,
                paidBy = TEST_PERSON_ID,
                category = "Other",
                createdAt = 0L
            )
        )

        val loaded = db.expenseDao().getExpenseById("exp-plain-1")
        assertNull("note should default to null", loaded?.note)
        assertNull("receiptPath should default to null", loaded?.receiptPath)
    }

    // -------- Migration SQL correctness --------

    @Test
    fun migration2To3_sql_runs_without_error_on_minimal_v2_table() {
        val sqLiteDb = db.openHelper.writableDatabase

        // Create a minimal v2-style table without note/receiptPath
        sqLiteDb.execSQL("CREATE TABLE IF NOT EXISTS expenses_v2_copy (id TEXT PRIMARY KEY, amount REAL)")
        sqLiteDb.execSQL("INSERT INTO expenses_v2_copy (id, amount) VALUES ('test', 99.0)")

        // Run the same SQL as MIGRATION_2_3 on our copy table to verify it works
        sqLiteDb.execSQL("ALTER TABLE expenses_v2_copy ADD COLUMN note TEXT")
        sqLiteDb.execSQL("ALTER TABLE expenses_v2_copy ADD COLUMN receiptPath TEXT")

        // Verify the old row survived with nulls in new columns
        val cursor = sqLiteDb.query("SELECT note, receiptPath FROM expenses_v2_copy WHERE id = 'test'")
        assertTrue(cursor.moveToFirst())
        assertTrue("note should be null after migration", cursor.isNull(cursor.getColumnIndexOrThrow("note")))
        assertTrue("receiptPath should be null after migration", cursor.isNull(cursor.getColumnIndexOrThrow("receiptPath")))
        cursor.close()
    }

    // -------- Helpers --------

    private suspend fun seedDashboardAndPerson() {
        db.dashboardDao().insertDashboard(
            com.hativ2.data.entity.DashboardEntity(
                id = TEST_DASH_ID, title = "Migration Test", coverImageUrl = null,
                currencySymbol = "$", themeColor = "#000", dashboardType = "other",
                createdAt = 0L, order = 0
            )
        )
        db.personDao().insertPerson(
            com.hativ2.data.entity.PersonEntity(
                id = TEST_PERSON_ID, name = "Tester", avatarColor = "#FFFFFF", createdAt = 0L
            )
        )
    }

    companion object {
        private const val TEST_DASH_ID = "dash-migration"
        private const val TEST_PERSON_ID = "person-migration"
    }
}
