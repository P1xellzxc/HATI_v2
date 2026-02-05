package com.hati.v2.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Transaction domain model matching Supabase schema.
 * 
 * Maps to the 'expenses' table in Supabase.
 * Note: We call it Transaction in the domain layer for clarity,
 * but it corresponds to the expenses table in the database.
 */
@Serializable
data class Transaction(
    val id: String,                    // UUID
    val groupId: String,               // FK to groups table
    val description: String,
    val amount: Double,                // Decimal(10,2) - stored as Double
    val paidBy: String,                // FK to users table
    val category: String = "other",
    val createdAt: Instant,
    val updatedAt: Instant
) {
    companion object {
        // Category constants
        const val CATEGORY_FOOD = "food"
        const val CATEGORY_TRANSPORT = "transport"
        const val CATEGORY_ENTERTAINMENT = "entertainment"
        const val CATEGORY_UTILITIES = "utilities"
        const val CATEGORY_SHOPPING = "shopping"
        const val CATEGORY_OTHER = "other"
        
        val CATEGORIES = listOf(
            CATEGORY_FOOD,
            CATEGORY_TRANSPORT,
            CATEGORY_ENTERTAINMENT,
            CATEGORY_UTILITIES,
            CATEGORY_SHOPPING,
            CATEGORY_OTHER
        )
    }
}

/**
 * User domain model.
 */
@Serializable
data class User(
    val id: String,
    val email: String,
    val name: String,
    val avatarUrl: String? = null,
    val createdAt: Instant
)

/**
 * Group domain model.
 */
@Serializable
data class Group(
    val id: String,
    val name: String,
    val description: String? = null,
    val createdBy: String?,
    val createdAt: Instant
)
