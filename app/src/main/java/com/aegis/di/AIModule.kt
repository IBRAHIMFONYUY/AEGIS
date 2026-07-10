package com.aegis.di

import android.content.Context
import com.aegis.agents.*
import com.aegis.ai.*
import com.aegis.core.GuardianAgent
import com.aegis.data.repository.GuardianMemoryRepository
import com.aegis.network.ThreatIntelClient

import com.aegis.core.EmergencyGuardian
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AIModule {

    @Provides
    @Singleton
    fun provideGemmaInferenceEngine(@ApplicationContext context: Context): GemmaInferenceEngine =
        GemmaInferenceEngine(context)
    
    @Provides
    @Singleton
    fun provideGeminiAIManager(@ApplicationContext context: Context): GeminiAIManager =
        GeminiAIManager(context)
    
    @Provides
    @Singleton
    fun provideAIOperationManager(
        @ApplicationContext context: Context,
        gemmaEngine: GemmaInferenceEngine,
        geminiAIManager: GeminiAIManager
    ): AIOperationManager {
        val manager = AIOperationManager(context)
        manager.setEngines(gemmaEngine, geminiAIManager)
        return manager
    }

    @Provides
    @Singleton
    fun provideInferenceEngine(
        gemmaEngine: GemmaInferenceEngine
    ): InferenceEngine = gemmaEngine

    @Provides
    @Singleton
    fun provideReasoningEngine(
        gemmaEngine: GemmaInferenceEngine
    ): ReasoningEngine = gemmaEngine

    @Provides
    @Singleton
    fun provideModelManager(@ApplicationContext context: Context): ModelManager =
        ModelManager(context)

    @Provides
    @Singleton
    fun provideImageAnalyzer(@ApplicationContext context: Context): ImageAnalyzer =
        ImageAnalyzer(context)

    @Provides
    @Singleton
    fun provideGuardianAgents(
        inferenceEngine: InferenceEngine,
        reasoningEngine: ReasoningEngine,
        imageAnalyzer: ImageAnalyzer,
        threatIntelClient: ThreatIntelClient,
        gemmaEngine: GemmaInferenceEngine
    ): List<GuardianAgent> {
        val baseAgents = listOf(
            ScamAgent(inferenceEngine, threatIntelClient, gemmaEngine),
            PrivacyAgent(inferenceEngine),
            CyberbullyingAgent(inferenceEngine),
            MisinformationAgent(inferenceEngine),
            IntentAgent(inferenceEngine, reasoningEngine),
            PaymentGuardianAgent(),
            MalwareGuardianAgent(inferenceEngine, threatIntelClient),
            DeepfakeAgent(inferenceEngine),
            BehavioralAgent(inferenceEngine),
            DecisionAgent(reasoningEngine),
            PhishingAgent(inferenceEngine, threatIntelClient)
        )

        return baseAgents + listOf(
            ImageGuardianAgent(imageAnalyzer, baseAgents),
            GuardianCoachAgent(reasoningEngine)
        )
    }

    @Provides
    @Singleton
    fun provideGuardianCore(
        agents: @JvmSuppressWildcards List<GuardianAgent>,
        memoryRepository: GuardianMemoryRepository,
        gemmaEngine: GemmaInferenceEngine
    ): GuardianCore = GuardianCore(agents, memoryRepository, gemmaEngine)

    @Provides
    @Singleton
    fun provideEmergencyGuardian(@ApplicationContext context: Context): EmergencyGuardian =
        EmergencyGuardian(context)


}
