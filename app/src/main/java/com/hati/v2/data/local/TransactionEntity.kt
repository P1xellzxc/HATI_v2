package com.hati.v2.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.Instant

/**
 * Room Entity for offline storage of transactions.
 * Maps to the Transaction domain model.
 */
@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey
    val id: String,                    // UUID from Supabase
    val groupId: String,
    val description: String,
    val amount: Double,
    val paidBy: String,
    val category: String,
    val createdAt: Long,               // Stored as epoch millis
    val updatedAt: Long,
    val isSynced: Boolean = true,      // Track sync status
    val isDeleted: Boolean = false     // Soft delete for sync
)

/**
 * Room Entity for users cache.
 */
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val id: String,
    val email: String,
    val name: String,
    val avatarUrl: String?,
    val createdAt: Long
)

/**
 * Room Entity for groups cache.
 */
@Entity(tableName = "groups")
data class GroupEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String?,
    val createdBy: String?,
    val createdAt: Long
)
