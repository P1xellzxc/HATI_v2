package com.hati.v2.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_metadata")
data class SyncMetadataEntity(
    @PrimaryKey
    val groupId: String,
    val lastSyncTime: Long,
    val lastSyncStatus: String, // "success", "failed", "in_progress"
    val pendingChanges: Int = 0
)
