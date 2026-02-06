package com.hati.v2.di

import android.content.Context
import androidx.room.Room
import com.hati.v2.data.local.DashboardDao
import com.hati.v2.data.local.GroupDao
import com.hati.v2.data.local.HatiDatabase
import com.hati.v2.data.local.MemberDao
import com.hati.v2.data.local.SyncMetadataDao
import com.hati.v2.data.local.TransactionDao
import com.hati.v2.data.local.UserDao
import com.hati.v2.data.network.NetworkMonitor
import com.hati.v2.data.network.NetworkMonitorImpl
import com.hati.v2.data.remote.SupabaseClientFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient {
        return SupabaseClientFactory.create()
    }

    @Provides
    @Singleton
    fun provideNetworkMonitor(@ApplicationContext context: Context): NetworkMonitor {
        return NetworkMonitorImpl(context)
    }
}

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
        )
        .addMigrations(HatiDatabase.MIGRATION_1_2)
        .build()
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
    fun provideDashboardDao(database: HatiDatabase): DashboardDao {
        return database.dashboardDao()
    }
    
    @Provides
    fun provideGroupDao(database: HatiDatabase): GroupDao {
        return database.groupDao()
    }

    @Provides
    fun provideMemberDao(database: HatiDatabase): MemberDao {
        return database.memberDao()
    }

    @Provides
    fun provideSyncMetadataDao(database: HatiDatabase): SyncMetadataDao {
        return database.syncMetadataDao()
    }
}
