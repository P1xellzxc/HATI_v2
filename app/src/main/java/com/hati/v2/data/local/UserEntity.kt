package com.hati.v2.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String, // Supabase Auth ID
    val email: String,
    val fullName: String?,
    val avatarUrl: String?,
    val lastSyncedAt: Long = 0
)
