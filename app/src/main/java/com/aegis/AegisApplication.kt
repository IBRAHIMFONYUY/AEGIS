package com.aegis

import android.app.Application
import timber.log.Timber
import com.aegis.agents.GuardianCore
import com.aegis.core.ThreatLevel
import com.aegis.data.db.entity.SafetyScore
import com.aegis.data.repository.SafetyRepository
import com.aegis.data.repository.ThreatRepository
import com.aegis.services.overlay.ThreatOverlayManager
import com.aegis.services.workmanager.ScanWorker
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltAndroidApp
class AegisApplication : Application() {

    companion object {
        private const val TAG = "AegisApplication"

        @Volatile
        private var instance: AegisApplication? = null

        fun getInstance(): AegisApplication =
            instance ?: error("AegisApplication has not been initialized.")
    }

    @Inject
    lateinit var guardianCore: GuardianCore

    @Inject
    lateinit var threatRepository: ThreatRepository

    @Inject
    lateinit var safetyRepository: SafetyRepository

    @Inject
    lateinit var overlayManager: ThreatOverlayManager

    @Inject
    lateinit var gemmaEngine: com.aegis.ai.GemmaInferenceEngine

    @Inject
    lateinit var securityNotificationManager: com.aegis.services.notification.AegisSecurityNotificationManager

    private val applicationScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        try {
            System.loadLibrary("sqlcipher")
        } catch (e: UnsatisfiedLinkError) {
            Timber.tag(TAG).e(e, "Failed to load sqlcipher library")
        }

        super.onCreate()

        instance = this

        // Pre-load Gemma 3N model automatically
        applicationScope.launch(Dispatchers.IO) {
            gemmaEngine.loadModel()
        }

        observeAnalysisResults()
        ScanWorker.schedule(this)
    }

    private fun observeAnalysisResults() {
        applicationScope.launch(Dispatchers.IO) {

            guardianCore.analysisResults.collect { result ->

                runCatching {

                    val threatId = threatRepository.saveAnalysisResult(result)

                    // Trigger for ALL apps that send notifications (WhatsApp, FB, LinkedIn, etc.)
                    val isNotificationTrigger = result.context.sourceType != com.aegis.core.SourceType.SCREEN &&
                                              result.context.sourceType != com.aegis.core.SourceType.UNKNOWN

                    if (result.overallThreatLevel.value >= ThreatLevel.LIKELY_MALICIOUS.value && isNotificationTrigger) {
                        withContext(Dispatchers.Main.immediate) {
                            // Enriched metadata with the database ID for deep linking
                            val enrichedResult = if (threatId != null) {
                                val newMetadata = result.context.metadata.toMutableMap()
                                newMetadata["db_threat_id"] = threatId.toString()
                                result.copy(context = result.context.copy(metadata = newMetadata))
                            } else result
                            
                            // Centralized alert trigger: this shows both overlay and notification
                            securityNotificationManager.showSecurityAlert(enrichedResult)
                        }
                    }

                    val score = guardianCore.getDetailedGuardianScore()

                    safetyRepository.recordScore(
                        SafetyScore(
                            score = score.overall,
                            privacyScore = score.privacy,
                            scamScore = score.scamProtection,
                            deviceScore = score.deviceSecurity,
                            wellbeingScore = score.digitalWellbeing,
                            totalThreats = 0,
                            blockedThreats = 0,
                            userActions = 0,
                            periodStart = System.currentTimeMillis(),
                            periodEnd = System.currentTimeMillis()
                        )
                    )

                }.onFailure { exception ->
                    Timber.tag(TAG).e(
                        exception,
                        "Failed processing Guardian analysis result."
                    )
                }
            }
        }
    }

    override fun onTerminate() {
        applicationScope.cancel()
        super.onTerminate()
    }
}