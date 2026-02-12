package com.hativ2.di

import android.content.Context
import androidx.room.Room
import com.hativ2.data.AppDatabase
import com.hativ2.data.dao.DashboardDao
import com.hativ2.data.dao.ExpenseDao
import com.hativ2.data.dao.PersonDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
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
