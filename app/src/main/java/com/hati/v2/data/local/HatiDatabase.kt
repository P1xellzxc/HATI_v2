package com.hati.v2.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        TransactionEntity::class, 
        UserEntity::class, 
        GroupEntity::class,
        SyncMetadataEntity::class
    ], 
    version = 2, // Increment version
    exportSchema = true
)
@TypeConverters(StringListConverter::class)
abstract class HatiDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun userDao(): UserDao
    abstract fun groupDao(): GroupDao
    abstract fun syncMetadataDao(): SyncMetadataDao
    
    companion object {
        const val DATABASE_NAME = "hati_database"
        
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add sync_metadata table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS sync_metadata (
                        groupId TEXT PRIMARY KEY NOT NULL,
                        lastSyncTime INTEGER NOT NULL,
                        lastSyncStatus TEXT NOT NULL,
                        pendingChanges INTEGER NOT NULL DEFAULT 0
                    )
                """)
                
                // Add groups table
                 database.execSQL("""
                    CREATE TABLE IF NOT EXISTS groups (
                        id TEXT PRIMARY KEY NOT NULL,
                        name TEXT NOT NULL,
                        description TEXT NOT NULL,
                        createdBy TEXT NOT NULL,
                        memberIds TEXT NOT NULL,
                        currency TEXT NOT NULL DEFAULT 'PHP',
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        isDeleted INTEGER NOT NULL DEFAULT 0
                    )
                """)
                
                // Add users table if it doesn't exist or re-create it (simplified for now as IF NOT EXISTS)
                // Note: In a real app we might need to handle column changes if UserEntity existed before with different schema
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS users (
                        id TEXT PRIMARY KEY NOT NULL,
                        email TEXT NOT NULL,
                        name TEXT NOT NULL,
                        avatarUrl TEXT,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """)
            }
        }
    }
}
