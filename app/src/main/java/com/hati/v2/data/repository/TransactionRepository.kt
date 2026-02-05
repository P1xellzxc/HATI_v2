package com.hati.v2.data.repository

import android.util.Log
import com.hati.v2.data.local.HatiDatabase
import com.hati.v2.data.local.TransactionEntity
import com.hati.v2.data.remote.ExpenseDto
import com.hati.v2.domain.model.Transaction
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * TransactionRepository - Offline-first data access.
 * 
 * Strategy:
 * 1. Always read from local Room database (fast, works offline)
 * 2. Sync with Supabase when online
 * 3. Queue changes when offline, sync when connection restored
 * 
 * SECURITY: Never log financial amounts to Logcat.
 */
@Singleton
class TransactionRepository @Inject constructor(
    private val database: HatiDatabase,
    private val supabaseClient: SupabaseClient
) {
    private val transactionDao = database.transactionDao()
    
    companion object {
        private const val TAG = "TransactionRepo"
        private const val TABLE_EXPENSES = "expenses"
    }
    
    /**
     * Get transactions for a group (offline-first).
     */
    fun getTransactionsByGroup(groupId: String): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByGroup(groupId)
            .map { entities -> entities.map { it.toDomain() } }
    }
    
    /**
     * Sync transactions from Supabase to local database.
     */
    suspend fun syncTransactions(groupId: String) = withContext(Dispatchers.IO) {
        try {
            val remoteExpenses = supabaseClient.from(TABLE_EXPENSES)
                .select {
                    filter {
                        eq("group_id", groupId)
                    }
                }
                .decodeList<ExpenseDto>()
            
            val entities = remoteExpenses.map { it.toEntity() }
            transactionDao.insertAll(entities)
            
            Log.d(TAG, "Synced ${entities.size} transactions") // Don't log amounts!
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed: ${e.message}")
        }
    }
    
    /**
     * Create a new transaction (saves locally, queues sync).
     */
    suspend fun createTransaction(transaction: Transaction) = withContext(Dispatchers.IO) {
        val entity = transaction.toEntity().copy(isSynced = false)
        transactionDao.insert(entity)
        
        // Attempt immediate sync
        syncNewTransaction(entity)
    }
    
    /**
     * Push unsynced transactions to Supabase.
     */
    suspend fun pushUnsyncedTransactions() = withContext(Dispatchers.IO) {
        val unsynced = transactionDao.getUnsyncedTransactions()
        
        unsynced.forEach { entity ->
            try {
                supabaseClient.from(TABLE_EXPENSES)
                    .insert(entity.toDto())
                
                transactionDao.markAsSynced(entity.id)
                Log.d(TAG, "Synced transaction ${entity.id}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync ${entity.id}: ${e.message}")
            }
        }
    }
    
    private suspend fun syncNewTransaction(entity: TransactionEntity) {
        try {
            supabaseClient.from(TABLE_EXPENSES)
                .insert(entity.toDto())
            
            transactionDao.markAsSynced(entity.id)
        } catch (e: Exception) {
            // Will be synced later
            Log.d(TAG, "Queued for later sync: ${entity.id}")
        }
    }
    
    // Extension functions for mapping
    private fun TransactionEntity.toDomain() = Transaction(
        id = id,
        groupId = groupId,
        description = description,
        amount = amount,
        paidBy = paidBy,
        category = category,
        createdAt = Instant.fromEpochMilliseconds(createdAt),
        updatedAt = Instant.fromEpochMilliseconds(updatedAt)
    )
    
    private fun Transaction.toEntity() = TransactionEntity(
        id = id,
        groupId = groupId,
        description = description,
        amount = amount,
        paidBy = paidBy,
        category = category,
        createdAt = createdAt.toEpochMilliseconds(),
        updatedAt = updatedAt.toEpochMilliseconds(),
        isSynced = true,
        isDeleted = false
    )
    
    private fun ExpenseDto.toEntity() = TransactionEntity(
        id = id,
        groupId = group_id,
        description = description,
        amount = amount,
        paidBy = paid_by,
        category = category,
        createdAt = Instant.parse(created_at).toEpochMilliseconds(),
        updatedAt = Instant.parse(updated_at).toEpochMilliseconds(),
        isSynced = true,
        isDeleted = false
    )
    
    private fun TransactionEntity.toDto() = ExpenseDto(
        id = id,
        group_id = groupId,
        description = description,
        amount = amount,
        paid_by = paidBy,
        category = category,
        created_at = Instant.fromEpochMilliseconds(createdAt).toString(),
        updated_at = Instant.fromEpochMilliseconds(updatedAt).toString()
    )
}
