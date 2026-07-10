package com.aegis.data.db.dao

import androidx.room.*
import com.aegis.data.db.entity.MemoryEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryDao {
    @Query("SELECT * FROM guardian_memory WHERE `key` = :key AND (agentName = :agentName OR agentName IS NULL) ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatest(key: String, agentName: String?): MemoryEntry?

    @Query("SELECT * FROM guardian_memory WHERE category = :category ORDER BY timestamp DESC")
    fun getByCategory(category: String): Flow<List<MemoryEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: MemoryEntry)

    @Query("DELETE FROM guardian_memory WHERE expiresAt IS NOT NULL AND expiresAt < :currentTime")
    suspend fun deleteExpired(currentTime: Long = System.currentTimeMillis())

    @Query("SELECT * FROM guardian_memory WHERE agentName = :agentName")
    suspend fun getForAgent(agentName: String): List<MemoryEntry>

    @Query("SELECT * FROM guardian_memory WHERE category = :category AND agentName = :agentName ORDER BY timestamp DESC")
    fun getByCategoryAndAgent(category: String, agentName: String): Flow<List<MemoryEntry>>
}
