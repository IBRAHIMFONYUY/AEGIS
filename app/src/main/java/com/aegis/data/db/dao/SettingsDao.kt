package com.aegis.data.db.dao

import androidx.room.*
import com.aegis.data.db.entity.AppSettings
import kotlinx.coroutines.flow.Flow

@Dao
interface SettingsDao {
    @Query("SELECT * FROM app_settings WHERE `key` = :key")
    suspend fun getSetting(key: String): AppSettings?

    @Query("SELECT * FROM app_settings WHERE `key` = :key")
    fun observeSetting(key: String): Flow<AppSettings?>

    @Query("SELECT * FROM app_settings")
    fun getAllSettings(): Flow<List<AppSettings>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setSetting(setting: AppSettings)

    @Query("DELETE FROM app_settings WHERE `key` = :key")
    suspend fun deleteSetting(key: String)
}
