package com.hati.v2.data.repository

import com.hati.v2.data.local.HatiDatabase
import com.hati.v2.data.local.TransactionEntity
import com.hati.v2.domain.model.Transaction
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing transaction data with offline-first architecture
 * Implements bidirectional sync between local Room database and Supabase
 */
@Singleton
class TransactionRepository @Inject constructor(
    private val database: HatiDatabase,
    private val supabaseClient: SupabaseClient,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val networkMonitor: NetworkMonitor
) {
    private val transactionDao = database.transactionDao()
    private val repositoryScope = CoroutineScope(SupervisorJob() + ioDispatcher)
    
    companion object {
        private const val SUPABASE_TABLE = "transactions"
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val RETRY_DELAY_MS = 1000L
    }
    
    /**
     * Get transactions for a specific group with real-time updates
     */
    fun getTransactionsByGroup(groupId: String): Flow<Result<List<Transaction>>> {
        return transactionDao.getTransactionsByGroup(groupId)
            .map { entities -> 
                Result.success(entities.map { it.toDomain() })
            }
            .catch { error ->
                Timber.e(error, "Error fetching transactions for group: $groupId")
                emit(Result.failure(error))
            }
    }
    
    /**
     * Add a new transaction (offline-first)
     */
    suspend fun addTransaction(transaction: Transaction): Result<Transaction> = withContext(ioDispatcher) {
        try {
            // 1. Save to local database immediately
            val entity = transaction.toEntity()
            transactionDao.insert(entity.copy(isSynced = false))
            Timber.d("Transaction saved locally: ${transaction.id}")
            
            // 2. Attempt sync if online
            if (networkMonitor.isOnline()) {
                syncTransaction(entity)
            } else {
                Timber.d("Offline - transaction queued for sync")
            }
            
            Result.success(transaction)
        } catch (e: Exception) {
            Timber.e(e, "Error adding transaction")
            Result.failure(e)
        }
    }
    
    /**
     * Update an existing transaction
     */
    suspend fun updateTransaction(transaction: Transaction): Result<Transaction> = withContext(ioDispatcher) {
        try {
            val entity = transaction.toEntity().copy(
                updatedAt = Clock.System.now().toEpochMilliseconds(),
                isSynced = false
            )
            
            transactionDao.insert(entity)
            
            if (networkMonitor.isOnline()) {
                syncTransaction(entity)
            }
            
            Result.success(transaction)
        } catch (e: Exception) {
            Timber.e(e, "Error updating transaction")
            Result.failure(e)
        }
    }
    
    /**
     * Delete a transaction (soft delete)
     */
    suspend fun deleteTransaction(transactionId: String): Result<Unit> = withContext(ioDispatcher) {
        try {
            transactionDao.markAsDeleted(transactionId)
            
            if (networkMonitor.isOnline()) {
                supabaseClient.from(SUPABASE_TABLE)
                    .delete {
                        filter { eq("id", transactionId) }
                    }
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error deleting transaction")
            Result.failure(e)
        }
    }
    
    /**
     * Sync local changes to remote
     */
    suspend fun syncLocalChanges(): Result<Int> = withContext(ioDispatcher) {
        if (!networkMonitor.isOnline()) {
            return@withContext Result.failure(NetworkUnavailableException("No network connection"))
        }
        
        try {
            val unsyncedTransactions = transactionDao.getUnsyncedTransactions()
            var syncedCount = 0
            
            unsyncedTransactions.forEach { entity ->
                val result = syncTransaction(entity)
                if (result.isSuccess) {
                    syncedCount++
                }
            }
            
            Timber.i("Synced $syncedCount/${unsyncedTransactions.size} transactions")
            Result.success(syncedCount)
        } catch (e: Exception) {
            Timber.e(e, "Error syncing local changes")
            Result.failure(e)
        }
    }
    
    /**
     * Fetch remote changes and update local database
     */
    suspend fun fetchRemoteChanges(groupId: String): Result<List<Transaction>> = withContext(ioDispatcher) {
        if (!networkMonitor.isOnline()) {
            return@withContext Result.failure(NetworkUnavailableException("No network connection"))
        }
        
        try {
            // Get last sync timestamp
            val lastSyncTime = database.syncMetadataDao().getLastSyncTime(groupId) ?: 0L
            
            // Fetch transactions updated after last sync
            val remoteTransactions = supabaseClient.from(SUPABASE_TABLE)
                .select {
                    filter {
                        eq("groupId", groupId)
                        gte("updatedAt", lastSyncTime)
                    }
                }
                .decodeList<SupabaseTransaction>()
            
            // Resolve conflicts and update local database
            remoteTransactions.forEach { remote ->
                resolveAndSaveTransaction(remote)
            }
            
            // Update sync metadata
            database.syncMetadataDao().updateLastSyncTime(
                groupId = groupId,
                timestamp = Clock.System.now().toEpochMilliseconds()
            )
            
            val transactions = remoteTransactions.map { it.toDomain() }
            Timber.i("Fetched ${transactions.size} remote changes")
            Result.success(transactions)
        } catch (e: Exception) {
            Timber.e(e, "Error fetching remote changes")
            Result.failure(e)
        }
    }
    
    /**
     * Subscribe to real-time updates for a group
     */
    fun subscribeToRealtimeUpdates(groupId: String) {
        repositoryScope.launch {
            try {
                val channel = supabaseClient.channel("transactions:$groupId")
                
                channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = SUPABASE_TABLE
                    filter = "groupId=eq.$groupId"
                }.collect { action ->
                    when (action) {
                        is PostgresAction.Insert -> {
                            val transaction = action.record.decodeAs<SupabaseTransaction>()
                            handleRealtimeInsert(transaction)
                        }
                        is PostgresAction.Update -> {
                            val transaction = action.record.decodeAs<SupabaseTransaction>()
                            handleRealtimeUpdate(transaction)
                        }
                        is PostgresAction.Delete -> {
                            val id = action.oldRecord["id"] as? String
                            id?.let { handleRealtimeDelete(it) }
                        }
                        else -> {}
                    }
                }
                
                channel.subscribe()
            } catch (e: Exception) {
                Timber.e(e, "Error subscribing to realtime updates")
            }
        }
    }
    
    // ============================
    // Private Helper Methods
    // ============================
    
    private suspend fun syncTransaction(entity: TransactionEntity): Result<Unit> {
        var attempt = 0
        var lastException: Exception? = null
        
        while (attempt < MAX_RETRY_ATTEMPTS) {
            try {
                val supabaseTransaction = entity.toSupabaseModel()
                
                supabaseClient.from(SUPABASE_TABLE)
                    .upsert(supabaseTransaction)
                
                transactionDao.markAsSynced(entity.id)
                Timber.d("Transaction synced: ${entity.id}")
                return Result.success(Unit)
            } catch (e: Exception) {
                lastException = e
                attempt++
                
                if (attempt < MAX_RETRY_ATTEMPTS) {
                    kotlinx.coroutines.delay(RETRY_DELAY_MS * attempt)
                    Timber.w("Sync attempt $attempt failed, retrying...")
                }
            }
        }
        
        Timber.e(lastException, "Failed to sync transaction after $MAX_RETRY_ATTEMPTS attempts")
        return Result.failure(lastException ?: Exception("Unknown sync error"))
    }
    
    private suspend fun resolveAndSaveTransaction(remote: SupabaseTransaction) {
        val local = transactionDao.getTransactionById(remote.id)
        
        if (local == null) {
            // New transaction from remote
            transactionDao.insert(remote.toEntity().copy(isSynced = true))
        } else {
            // Conflict resolution: Last-Write-Wins (LWW)
            if (remote.updatedAt > local.updatedAt) {
                transactionDao.insert(remote.toEntity().copy(isSynced = true))
                Timber.d("Resolved conflict: remote wins for ${remote.id}")
            } else if (remote.updatedAt < local.updatedAt && !local.isSynced) {
                // Local has newer changes, will be synced later
                Timber.d("Resolved conflict: local wins for ${remote.id}")
            } else {
                // Same timestamp or already synced
                transactionDao.insert(remote.toEntity().copy(isSynced = true))
            }
        }
    }
    
    private suspend fun handleRealtimeInsert(transaction: SupabaseTransaction) {
        withContext(ioDispatcher) {
            try {
                transactionDao.insert(transaction.toEntity().copy(isSynced = true))
                Timber.d("Realtime insert: ${transaction.id}")
            } catch (e: Exception) {
                Timber.e(e, "Error handling realtime insert")
            }
        }
    }
    
    private suspend fun handleRealtimeUpdate(transaction: SupabaseTransaction) {
        withContext(ioDispatcher) {
            try {
                resolveAndSaveTransaction(transaction)
                Timber.d("Realtime update: ${transaction.id}")
            } catch (e: Exception) {
                Timber.e(e, "Error handling realtime update")
            }
        }
    }
    
    private suspend fun handleRealtimeDelete(transactionId: String) {
        withContext(ioDispatcher) {
            try {
                transactionDao.markAsDeleted(transactionId)
                Timber.d("Realtime delete: $transactionId")
            } catch (e: Exception) {
                Timber.e(e, "Error handling realtime delete")
            }
        }
    }
}

// ============================
// Extension Functions
// ============================

private fun TransactionEntity.toDomain(): Transaction {
    return Transaction(
        id = id,
        groupId = groupId,
        description = description,
        amount = amount,
        paidBy = paidBy,
        category = category,
        createdAt = Instant.fromEpochMilliseconds(createdAt),
        updatedAt = Instant.fromEpochMilliseconds(updatedAt)
    )
}

private fun Transaction.toEntity(): TransactionEntity {
    return TransactionEntity(
        id = id,
        groupId = groupId,
        description = description,
        amount = amount,
        paidBy = paidBy,
        category = category,
        createdAt = createdAt.toEpochMilliseconds(),
        updatedAt = updatedAt.toEpochMilliseconds(),
        isSynced = false,
        isDeleted = false
    )
}

private fun TransactionEntity.toSupabaseModel(): SupabaseTransaction {
    return SupabaseTransaction(
        id = id,
        groupId = groupId,
        description = description,
        amount = amount,
        paidBy = paidBy,
        category = category,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

private fun SupabaseTransaction.toEntity(): TransactionEntity {
    return TransactionEntity(
        id = id,
        groupId = groupId,
        description = description,
        amount = amount,
        paidBy = paidBy,
        category = category,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isSynced = true,
        isDeleted = false
    )
}

private fun SupabaseTransaction.toDomain(): Transaction {
    return Transaction(
        id = id,
        groupId = groupId,
        description = description,
        amount = amount,
        paidBy = paidBy,
        category = category,
        createdAt = Instant.fromEpochMilliseconds(createdAt),
        updatedAt = Instant.fromEpochMilliseconds(updatedAt)
    )
}

// ============================
// Data Models
// ============================

@Serializable
data class SupabaseTransaction(
    val id: String,
    val groupId: String,
    val description: String,
    val amount: Double,
    val paidBy: String,
    val category: String,
    val createdAt: Long,
    val updatedAt: Long
)

// ============================
// Custom Exceptions
// ============================

class NetworkUnavailableException(message: String) : Exception(message)
class SyncConflictException(message: String) : Exception(message)
