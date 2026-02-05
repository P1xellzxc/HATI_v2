package com.hati.v2.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * HATI_v2 Room Database.
 * Provides offline storage for transactions, users, and groups.
 */
@Database(
    entities = [
        TransactionEntity::class,
        UserEntity::class,
        GroupEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class HatiDatabase : RoomDatabase() {
    
    abstract fun transactionDao(): TransactionDao
    abstract fun userDao(): UserDao
    abstract fun groupDao(): GroupDao
    
    companion object {
        const val DATABASE_NAME = "hati_database"
    }
}
