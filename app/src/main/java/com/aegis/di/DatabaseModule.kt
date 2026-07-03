package com.aegis.di

import android.content.Context
import com.aegis.data.db.AegisDatabase
import com.aegis.data.db.dao.*
import com.aegis.security.CryptoManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAegisDatabase(
        @ApplicationContext context: Context,
        cryptoManager: CryptoManager
    ): AegisDatabase {
        val passphrase = cryptoManager.generateDatabasePassphrase()
        return AegisDatabase.getInstance(context, passphrase)
    }

    @Provides
    fun provideThreatDao(database: AegisDatabase): ThreatDao = database.threatDao()

    @Provides
    fun provideSafetyScoreDao(database: AegisDatabase): SafetyScoreDao = database.safetyScoreDao()

    @Provides
    fun provideLearningDao(database: AegisDatabase): LearningDao = database.learningDao()

    @Provides
    fun provideSettingsDao(database: AegisDatabase): SettingsDao = database.settingsDao()

    @Provides
    fun provideMemoryDao(database: AegisDatabase): MemoryDao = database.memoryDao()
}
