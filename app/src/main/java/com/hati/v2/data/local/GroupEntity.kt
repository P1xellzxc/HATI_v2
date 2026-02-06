package com.hati.v2.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters

@Entity(tableName = "groups")
@TypeConverters(StringListConverter::class)
data class GroupEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String,
    val createdBy: String,
    val memberIds: List<String>, 
    val currency: String = "PHP",
    val createdAt: Long,
    val updatedAt: Long,
    val isDeleted: Boolean = false
)

// Type converter for List<String>
import androidx.room.TypeConverter

class StringListConverter {
    @TypeConverter
    fun fromString(value: String): List<String> {
        return if (value.isBlank()) emptyList() else value.split(",").filter { it.isNotBlank() }
    }
    
    @TypeConverter
    fun toString(list: List<String>): String {
        return list.joinToString(",")
    }
}
