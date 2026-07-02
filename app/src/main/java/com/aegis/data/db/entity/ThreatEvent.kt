package com.aegis.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "threat_events")
data class ThreatEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val agentName: String,
    val threatLevel: Int,
    val confidence: Float,
    val reason: String,
    val sourceText: String,
    val sourceApp: String?,
    val sourceType: String,
    val suggestedAction: String?,
    val details: String?,
    val timestamp: Long = System.currentTimeMillis(),
    val isResolved: Boolean = false,
    val userAction: String? = null
)
