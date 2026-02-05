package com.hati.v2.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.Instant

/**
 * Room Entity for offline storage of transactions.
 * Maps to the Transaction domain model.
 */
@Entity(
    tableName = "transactions",
    foreignKeys = [
        androidx.room.ForeignKey(
            entity = DashboardEntity::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = androidx.room.ForeignKey.CASCADE
        )
    ],
    indices = [androidx.room.Index("groupId")]
)
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

