package com.aegis.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "safety_scores")
data class SafetyScore(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val score: Float,
    val totalThreats: Int,
    val blockedThreats: Int,
    val userActions: Int,
    val periodStart: Long,
    val periodEnd: Long,
    val timestamp: Long = System.currentTimeMillis()
)
