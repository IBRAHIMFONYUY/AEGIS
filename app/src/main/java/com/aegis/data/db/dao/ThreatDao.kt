package com.aegis.data.db.dao

import androidx.room.*
import com.aegis.data.db.entity.ThreatEvent
import kotlinx.coroutines.flow.Flow

@Dao
interface ThreatDao {
    @Query("SELECT * FROM threat_events ORDER BY timestamp DESC")
    fun getAllThreats(): Flow<List<ThreatEvent>>

    @Query("SELECT * FROM threat_events WHERE threatLevel >= :minLevel ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentThreats(minLevel: Int = 2, limit: Int = 50): Flow<List<ThreatEvent>>

    @Query("SELECT * FROM threat_events WHERE id = :id")
    suspend fun getThreatById(id: Long): ThreatEvent?

    @Query("SELECT * FROM threat_events WHERE timestamp > :since ORDER BY timestamp DESC")
    fun getThreatsSince(since: Long): Flow<List<ThreatEvent>>

    @Query("SELECT COUNT(*) FROM threat_events WHERE agentName = 'ScamAgent' AND timestamp >= :since")
    fun getScamsBlockedCount(since: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM threat_events WHERE (reason LIKE '%link%' OR details LIKE '%url%') AND timestamp >= :since")
    fun getLinksBlockedCount(since: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM threat_events WHERE agentName = 'MisinformationAgent' AND timestamp >= :since")
    fun getFakeNewsDetectedCount(since: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM threat_events WHERE threatLevel >= 3")
    fun getCriticalThreatCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM threat_events")
    fun getTotalThreatCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(threat: ThreatEvent): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(threats: List<ThreatEvent>)

    @Update
    suspend fun update(threat: ThreatEvent)

    @Query("UPDATE threat_events SET isResolved = :resolved, userAction = :action WHERE id = :id")
    suspend fun resolveThreat(id: Long, resolved: Boolean = true, action: String? = null)

    @Query("DELETE FROM threat_events WHERE timestamp < :before")
    suspend fun deleteOldThreats(before: Long)

    @Delete
    suspend fun delete(threat: ThreatEvent)
}
