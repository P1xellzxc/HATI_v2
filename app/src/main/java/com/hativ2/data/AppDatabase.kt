package com.hativ2.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
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
    version = 2,
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
}
