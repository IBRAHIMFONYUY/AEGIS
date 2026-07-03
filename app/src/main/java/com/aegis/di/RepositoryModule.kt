package com.aegis.di

import com.aegis.data.db.dao.*
import com.aegis.data.repository.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideThreatRepository(threatDao: ThreatDao): ThreatRepository = 
        ThreatRepository(threatDao)

    @Provides
    @Singleton
    fun provideSafetyRepository(safetyScoreDao: SafetyScoreDao): SafetyRepository = 
        SafetyRepository(safetyScoreDao)

    @Provides
    @Singleton
    fun provideLearningRepository(learningDao: LearningDao): LearningRepository = 
        LearningRepository(learningDao)

    @Provides
    @Singleton
    fun provideSettingsRepository(settingsDao: SettingsDao): SettingsRepository = 
        SettingsRepository(settingsDao)

    @Provides
    @Singleton
    fun provideMemoryRepository(memoryDao: MemoryDao): GuardianMemoryRepository = 
        GuardianMemoryRepository(memoryDao)
}
