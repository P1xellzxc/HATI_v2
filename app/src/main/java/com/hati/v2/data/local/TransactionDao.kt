package com.hati.v2.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Room DAO for Transaction operations.
 * Provides offline-first data access.
 */
@Dao
interface TransactionDao {
    
    @Query("SELECT * FROM transactions WHERE groupId = :groupId AND isDeleted = 0 ORDER BY createdAt DESC")
    fun getTransactionsByGroup(groupId: String): Flow<List<TransactionEntity>>
    
    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: String): TransactionEntity?
    
    @Query("SELECT * FROM transactions WHERE isSynced = 0")
    suspend fun getUnsyncedTransactions(): List<TransactionEntity>
    
    @Query("SELECT * FROM transactions WHERE isDeleted = 1 AND isSynced = 0")
    suspend fun getDeletedUnsyncedTransactions(): List<TransactionEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<TransactionEntity>)
    
    @Update
    suspend fun update(transaction: TransactionEntity)
    
    @Query("UPDATE transactions SET isSynced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: String)
    
    @Query("UPDATE transactions SET isDeleted = 1, isSynced = 0 WHERE id = :id")
    suspend fun markAsDeleted(id: String)
    
    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: String)
    
    @Query("DELETE FROM transactions WHERE groupId = :groupId")
    suspend fun deleteByGroup(groupId: String)
}

/**
 * Room DAO for User operations.
 */
@Dao
interface UserDao {
    
    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<UserEntity>>
    
    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getUserById(id: String): UserEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: UserEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(users: List<UserEntity>)
}

/**
 * Room DAO for Group operations.
 */
@Dao
interface GroupDao {
    
    @Query("SELECT * FROM groups ORDER BY createdAt DESC")
    fun getAllGroups(): Flow<List<GroupEntity>>
    
    @Query("SELECT * FROM groups WHERE id = :id")
    suspend fun getGroupById(id: String): GroupEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(group: GroupEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(groups: List<GroupEntity>)
    
    @Query("DELETE FROM groups WHERE id = :id")
    suspend fun deleteById(id: String)
}
