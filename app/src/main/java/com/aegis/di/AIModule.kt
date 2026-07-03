package com.aegis.di

import android.content.Context
import com.aegis.agents.*
import com.aegis.ai.*
import com.aegis.core.GuardianAgent
import com.aegis.data.repository.GuardianMemoryRepository
import com.aegis.network.ThreatIntelClient
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
    fun provideInferenceEngine(@ApplicationContext context: Context): CompositeInferenceEngine =
        CompositeInferenceEngine(context)

    @Provides
    @Singleton
    fun provideReasoningEngine(
        @ApplicationContext context: Context,
        modelManager: ModelManager
    ): ReasoningEngine = OnnxReasoningEngine(context, modelManager)

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
        inferenceEngine: CompositeInferenceEngine,
        reasoningEngine: ReasoningEngine,
        imageAnalyzer: ImageAnalyzer,
        threatIntelClient: ThreatIntelClient
    ): List<GuardianAgent> {
        val baseAgents = listOf(
            ScamAgent(inferenceEngine, threatIntelClient),
            PrivacyAgent(inferenceEngine),
            CyberbullyingAgent(inferenceEngine),
            MisinformationAgent(inferenceEngine),
            IntentAgent(inferenceEngine, reasoningEngine),
            PaymentGuardianAgent(),
            MalwareGuardianAgent(inferenceEngine, threatIntelClient)
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
        memoryRepository: GuardianMemoryRepository
    ): GuardianCore = GuardianCore(agents, memoryRepository)
}
