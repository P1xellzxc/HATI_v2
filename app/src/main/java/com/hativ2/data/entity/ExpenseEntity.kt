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
    
    // Split details (simplified for now as JSON string or we can create a separate SplitEntity)
    // For a robust implementation, a SplitEntity is better, but to match the rapid porting, 
    // we might store splits as a JSON string or simplified structure if they are complex.
    // However, the original schema had a `splits` table. Let's stick to the relational model.
)
