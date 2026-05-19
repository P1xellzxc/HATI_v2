package com.hativ2.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "expenses",
    foreignKeys = [
        ForeignKey(
            entity = DashboardEntity::class,
            parentColumns = ["id"],
            childColumns = ["dashboardId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = PersonEntity::class,
            parentColumns = ["id"],
            childColumns = ["paidBy"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("dashboardId"),
        Index("paidBy")
    ]
)
data class ExpenseEntity(
    @PrimaryKey val id: String,
    val dashboardId: String,
    val description: String,
    val amount: Double,
    val paidBy: String?, // Person ID
    val category: String, // "food", "transport", etc.
    val createdAt: Long,

    // Optional user-provided note for the chapter/expense.
    val note: String? = null,

    // Absolute path under filesDir/receipts/ of a copied receipt image.
    // Stored on disk inside the app sandbox; the DB only holds the path.
    val receiptPath: String? = null,
)
