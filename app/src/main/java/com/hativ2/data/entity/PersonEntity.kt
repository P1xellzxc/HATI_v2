package com.hativ2.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "people")
data class PersonEntity(
    @PrimaryKey val id: String,
    val name: String,
    val avatarColor: String,
    val createdAt: Long
)
