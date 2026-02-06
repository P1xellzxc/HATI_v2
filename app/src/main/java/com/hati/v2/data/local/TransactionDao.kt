package com.hati.v2.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    
    @Query("SELECT * FROM transactions WHERE groupId = :groupId AND isDeleted = 0 ORDER BY createdAt DESC")
    fun getTransactionsByGroup(groupId: String): Flow<List<TransactionEntity>>
    
    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: String): TransactionEntity?
    
    @Query("SELECT * FROM transactions WHERE groupId = :groupId AND isDeleted = 0")
    suspend fun getAllTransactions(groupId: String): List<TransactionEntity>
    
    @Query("SELECT * FROM transactions WHERE isSynced = 0 AND isDeleted = 0")
    suspend fun getUnsyncedTransactions(): List<TransactionEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<TransactionEntity>)
    
    @Query("UPDATE transactions SET isSynced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: String)
    
    @Query("UPDATE transactions SET isDeleted = 1 WHERE id = :id")
    suspend fun markAsDeleted(id: String)
    
    @Query("DELETE FROM transactions WHERE isDeleted = 1 AND updatedAt < :timestamp")
    suspend fun deleteOldSoftDeletedTransactions(timestamp: Long)
}
