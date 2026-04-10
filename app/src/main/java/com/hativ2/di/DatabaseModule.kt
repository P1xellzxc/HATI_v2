package com.hativ2.di

import android.content.Context
import androidx.room.Room
import com.hativ2.data.AppDatabase
import com.hativ2.data.dao.DashboardDao
import com.hativ2.data.dao.ExpenseDao
import com.hativ2.data.dao.PersonDao
import com.hativ2.data.security.DatabaseKeyManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.sqlcipher.database.SupportFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        // Why SupportFactory(passphrase) instead of plain Room.databaseBuilder:
        // Plain Room stores all data in unencrypted SQLite. Anyone with root access
        // or physical access to the device can read every expense, person name, and
        // debt record. SupportFactory wraps Room's SQLiteOpenHelper with SQLCipher,
        // which encrypts the entire database file using AES-256.
        //
        // The passphrase is stored encrypted in SharedPreferences and unwrapped
        // using a hardware-backed Android Keystore key (see DatabaseKeyManager).
        val passphrase = DatabaseKeyManager.getPassphrase(context)
        val factory = SupportFactory(passphrase)

        return Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "hati_database"
        )
            .openHelperFactory(factory)
            // Why fallbackToDestructiveMigrationFrom(1) instead of
            // fallbackToDestructiveMigration():
            // The blanket fallback silently wipes ALL user data on ANY schema
            // change. By specifying version 1 only, we limit destructive
            // migration to the initial v1→v2 change. Future upgrades MUST
            // provide explicit Migration objects to preserve user data.
            .fallbackToDestructiveMigrationFrom(1)
            .build()
    }

    @Provides
    fun provideDashboardDao(database: AppDatabase): DashboardDao {
        return database.dashboardDao()
    }

    @Provides
    fun providePersonDao(database: AppDatabase): PersonDao {
        return database.personDao()
    }

    @Provides
    fun provideExpenseDao(database: AppDatabase): ExpenseDao {
        return database.expenseDao()
    }

    @Provides
    fun provideTransactionRepository(expenseDao: ExpenseDao): com.hativ2.domain.repository.TransactionRepository {
        return com.hativ2.data.repository.TransactionRepositoryImpl(expenseDao)
    }
}
