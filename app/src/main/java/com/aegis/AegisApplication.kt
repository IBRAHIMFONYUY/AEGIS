package com.aegis

import android.app.Application
import com.aegis.agents.GuardianCore
import com.aegis.core.ThreatLevel
import com.aegis.data.repository.SafetyRepository
import com.aegis.data.repository.ThreatRepository
import com.aegis.services.overlay.ThreatOverlayManager
import com.aegis.services.workmanager.ScanWorker
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.*
import javax.inject.Inject

@HiltAndroidApp
class AegisApplication : Application() {

    @Inject
    lateinit var guardianCore: GuardianCore
    
    @Inject
    lateinit var threatRepository: ThreatRepository
    
    @Inject
    lateinit var safetyRepository: SafetyRepository
    
    @Inject
    lateinit var overlayManager: ThreatOverlayManager

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        instance = this
        
        observeAnalysisResults()
        ScanWorker.schedule(this)
    }

    private fun observeAnalysisResults() {
        applicationScope.launch(Dispatchers.IO) {
            guardianCore.analysisResults.collect { result ->
                threatRepository.saveAnalysisResult(result)

                if (result.overallThreatLevel.value >= ThreatLevel.SUSPICIOUS.value) {
                    withContext(Dispatchers.Main) {
                        overlayManager.showThreatAlert(result)
                    }
                }

                // Update safety score history
                val detailedScore = guardianCore.getDetailedGuardianScore()
                safetyRepository.recordScore(
                    com.aegis.data.db.entity.SafetyScore(
                        score = detailedScore.overall,
                        privacyScore = detailedScore.privacy,
                        scamScore = detailedScore.scamProtection,
                        deviceScore = detailedScore.deviceSecurity,
                        wellbeingScore = detailedScore.digitalWellbeing,
                        totalThreats = 0,
                        blockedThreats = 0,
                        userActions = 0,
                        periodStart = System.currentTimeMillis(),
                        periodEnd = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    companion object {
        @Volatile
        private var instance: AegisApplication? = null

        fun getInstance(): AegisApplication =
            instance ?: throw IllegalStateException("AegisApplication not initialized")
    }
}
