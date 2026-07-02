package com.aegis.di

import android.content.Context
import com.aegis.AegisApplication
import com.aegis.agents.*
import com.aegis.core.GuardianAgent
import com.aegis.ai.CompositeInferenceEngine
import com.aegis.data.db.AegisDatabase
import com.aegis.data.repository.*
import com.aegis.security.CryptoManager
import com.aegis.security.KeyStoreManager

object AppModule {

    private lateinit var application: AegisApplication

    fun init(app: AegisApplication) {
        application = app
    }

    fun provideContext(): Context = application

    fun provideCryptoManager(): CryptoManager = application.cryptoManager

    fun provideKeyStoreManager(): KeyStoreManager = application.keyStoreManager

    fun provideDatabase(): AegisDatabase = application.database

    fun provideThreatRepository(): ThreatRepository = application.threatRepository

    fun provideSafetyRepository(): SafetyRepository = application.safetyRepository

    fun provideLearningRepository(): LearningRepository = application.learningRepository

    fun provideSettingsRepository(): SettingsRepository = application.settingsRepository

    fun provideInferenceEngine(): CompositeInferenceEngine = application.inferenceEngine

    fun provideGuardianCore(): GuardianCore = application.guardianCore

    fun createAgents(inferenceEngine: CompositeInferenceEngine): List<GuardianAgent> {
        return listOf(
            ScamAgent(inferenceEngine),
            PrivacyAgent(inferenceEngine),
            CyberbullyingAgent(inferenceEngine),
            MisinformationAgent(inferenceEngine)
        )
    }
}
