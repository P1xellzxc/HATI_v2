package com.hati.v2.di

import android.content.Context
import androidx.room.Room
import com.hati.v2.data.local.GroupDao
import com.hati.v2.data.local.HatiDatabase
import com.hati.v2.data.local.TransactionDao
import com.hati.v2.data.local.UserDao
import com.hati.v2.data.remote.SupabaseClientFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import javax.inject.Singleton

/**
 * Hilt AppModule - Provides application-level dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient {
        return SupabaseClientFactory.create()
    }
}

/**
 * Hilt DatabaseModule - Provides Room database dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideHatiDatabase(
        @ApplicationContext context: Context
    ): HatiDatabase {
        return Room.databaseBuilder(
            context,
            HatiDatabase::class.java,
            HatiDatabase.DATABASE_NAME
        ).build()
    }
    
    @Provides
    fun provideTransactionDao(database: HatiDatabase): TransactionDao {
        return database.transactionDao()
    }
    
    @Provides
    fun provideUserDao(database: HatiDatabase): UserDao {
        return database.userDao()
    }
    
    @Provides
    fun provideGroupDao(database: HatiDatabase): GroupDao {
        return database.groupDao()
    }
}
