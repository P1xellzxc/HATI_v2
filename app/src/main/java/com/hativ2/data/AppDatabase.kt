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
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun personDao(): PersonDao
    abstract fun dashboardDao(): DashboardDao
    abstract fun expenseDao(): ExpenseDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "hati_database"
                )
                    // Why fallbackToDestructiveMigrationFrom(1) instead of
                    // fallbackToDestructiveMigration():
                    // The blanket fallbackToDestructiveMigration() silently wipes
                    // ALL user data on ANY schema change. This is dangerous for a
                    // finance app — users would lose their expense history without
                    // warning. By specifying version 1 only, we limit destructive
                    // migration to the initial v1→v2 schema change (which already
                    // happened). Future schema upgrades (v2→v3, etc.) MUST provide
                    // explicit Migration objects to preserve user data.
                    .fallbackToDestructiveMigrationFrom(1)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
