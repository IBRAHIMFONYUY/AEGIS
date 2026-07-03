package com.aegis.di

import android.content.Context
import com.aegis.agents.*
import com.aegis.ai.*
import com.aegis.core.GuardianAgent
import com.aegis.data.repository.GuardianMemoryRepository
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
    fun provideReasoningEngine(): ReasoningEngine = SimpleReasoningEngine()

    @Provides
    @Singleton
    fun provideImageAnalyzer(@ApplicationContext context: Context): ImageAnalyzer =
        ImageAnalyzer(context)

    @Provides
    @Singleton
    fun provideGuardianAgents(
        inferenceEngine: CompositeInferenceEngine,
        reasoningEngine: ReasoningEngine,
        imageAnalyzer: ImageAnalyzer
    ): List<GuardianAgent> {
        val baseAgents = listOf(
            ScamAgent(inferenceEngine),
            PrivacyAgent(inferenceEngine),
            CyberbullyingAgent(inferenceEngine),
            MisinformationAgent(inferenceEngine),
            IntentAgent(inferenceEngine, reasoningEngine),
            PaymentGuardianAgent(),
            MalwareGuardianAgent(inferenceEngine)
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
