package com.aegis

import android.app.Application
import android.util.Log
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

    private val applicationScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        try {
            System.loadLibrary("sqlcipher")
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "Failed to load sqlcipher library", e)
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

                    threatRepository.saveAnalysisResult(result)

                    val isHighRisk = result.overallThreatLevel.value >= ThreatLevel.MALICIOUS.value
                    val isNotification = result.context.sourceType == com.aegis.core.SourceType.NOTIFICATION ||
                            result.context.sourceType == com.aegis.core.SourceType.WHATSAPP ||
                            result.context.sourceType == com.aegis.core.SourceType.TELEGRAM ||
                            result.context.sourceType == com.aegis.core.SourceType.SMS

                    if (isHighRisk && isNotification) {
                        withContext(Dispatchers.Main.immediate) {
                            overlayManager.showThreatAlert(result)
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
                    Log.e(
                        TAG,
                        "Failed processing Guardian analysis result.",
                        exception
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