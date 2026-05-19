package com.hativ2.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.hativ2.data.dao.DashboardDao
import com.hativ2.data.dao.ExpenseDao
import com.hativ2.data.dao.PersonDao
import com.hativ2.data.entity.DashboardEntity
import com.hativ2.data.entity.DashboardMemberEntity
import com.hativ2.data.entity.ExpenseEntity
import com.hativ2.data.entity.PersonEntity
import com.hativ2.data.entity.SettlementEntity
import com.hativ2.data.entity.SplitEntity

@Database(
    entities = [
        PersonEntity::class,
        DashboardEntity::class,
        DashboardMemberEntity::class,
        ExpenseEntity::class,
        SplitEntity::class,
        SettlementEntity::class
    ],
    version = 3,
    // Why exportSchema = true:
    // Enables Room to export the database schema as JSON files to app/schemas/.
    // This gives us build-time migration validation, schema diffing in source
    // control, and the ability to use @AutoMigration for future upgrades.
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun personDao(): PersonDao
    abstract fun dashboardDao(): DashboardDao
    abstract fun expenseDao(): ExpenseDao

    // Why no companion object / singleton pattern here:
    // Database creation is fully managed by Hilt (see DatabaseModule).
    // Having a second creation path via a companion object risks creating
    // an unencrypted instance that bypasses the SQLCipher SupportFactory.

    companion object {
        // v2 → v3: add ExpenseEntity.note and ExpenseEntity.receiptPath
        // so the New Chapter form can attach an optional note and receipt
        // photo. Both columns are nullable so existing rows survive.
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE expenses ADD COLUMN note TEXT")
                db.execSQL("ALTER TABLE expenses ADD COLUMN receiptPath TEXT")
            }
        }
    }
}
