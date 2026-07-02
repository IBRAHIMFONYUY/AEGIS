package com.aegis.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "guardian_memory")
data class MemoryEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val key: String,
    val value: String,
    val agentName: String? = null,
    val category: String,
    val timestamp: Long = System.currentTimeMillis(),
    val expiresAt: Long? = null
)
