package com.aegis

import android.app.Application
import com.aegis.agents.*
import com.aegis.ai.CompositeInferenceEngine
import com.aegis.data.db.AegisDatabase
import com.aegis.data.repository.*
import com.aegis.security.CryptoManager
import com.aegis.security.KeyStoreManager
import com.aegis.services.workmanager.ScanWorker
import com.aegis.services.overlay.ThreatOverlayManager
import com.aegis.ai.SimpleReasoningEngine
import kotlinx.coroutines.*
import net.zetetic.database.sqlcipher.SQLiteDatabase
import java.io.File

class AegisApplication : Application() {

    lateinit var guardianCore: GuardianCore
        private set
    lateinit var cryptoManager: CryptoManager
        private set
    lateinit var keyStoreManager: KeyStoreManager
        private set
    lateinit var database: AegisDatabase
        private set
    lateinit var threatRepository: ThreatRepository
        private set
    lateinit var safetyRepository: SafetyRepository
        private set
    lateinit var learningRepository: LearningRepository
        private set
    lateinit var settingsRepository: SettingsRepository
        private set
    lateinit var memoryRepository: GuardianMemoryRepository
        private set
    lateinit var inferenceEngine: CompositeInferenceEngine
        private set
    lateinit var overlayManager: ThreatOverlayManager
        private set

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        instance = this

        overlayManager = ThreatOverlayManager(this)
        initializeSecurity()
        initializeDatabase()
        initializeAI()
        initializeAgents()
        observeAnalysisResults()
        ScanWorker.schedule(this)
    }

    private fun initializeSecurity() {
        cryptoManager = CryptoManager()
        cryptoManager.generateAESKey()
        cryptoManager.generateRSAKeyPair()

        keyStoreManager = KeyStoreManager(this)
        keyStoreManager.initializeMasterKey()
    }

    private fun initializeDatabase() {
        try {
            val passphrase = cryptoManager.generateDatabasePassphrase()
            database = AegisDatabase.getInstance(this, passphrase)
            // Trigger a database operation to verify it's working
            database.openHelper.writableDatabase
            
            threatRepository = ThreatRepository(database.threatDao())
            safetyRepository = SafetyRepository(database.safetyScoreDao())
            learningRepository = LearningRepository(database.learningDao())
            settingsRepository = SettingsRepository(database.settingsDao())
            memoryRepository = GuardianMemoryRepository(database.memoryDao())
        } catch (_: Exception) {
            // Close the instance if it exists to release file locks
            AegisDatabase.closeDatabase()
            
            // Delete the database file and its journals/wal
            val dbFile = getDatabasePath("aegis_secure.db")
            if (dbFile.exists()) {
                dbFile.delete()
                File(dbFile.path + "-journal").delete()
                File(dbFile.path + "-wal").delete()
                File(dbFile.path + "-shm").delete()
            }
            
            val passphrase = cryptoManager.generateDatabasePassphrase()
            database = AegisDatabase.getInstance(this, passphrase)
            threatRepository = ThreatRepository(database.threatDao())
            safetyRepository = SafetyRepository(database.safetyScoreDao())
            learningRepository = LearningRepository(database.learningDao())
            settingsRepository = SettingsRepository(database.settingsDao())
            memoryRepository = GuardianMemoryRepository(database.memoryDao())
        }
    }

    private fun observeAnalysisResults() {
        applicationScope.launch(Dispatchers.IO) {
            guardianCore.analysisResults.collect { result ->
                threatRepository.saveAnalysisResult(result)

                if (result.overallThreatLevel.value >= com.aegis.core.ThreatLevel.SUSPICIOUS.value) {
                    withContext(Dispatchers.Main) {
                        overlayManager.showThreatAlert(result)
                    }
                }

                // Update safety score history
                val currentScore = guardianCore.getOverallSafetyScore()
                safetyRepository.recordScore(
                    com.aegis.data.db.entity.SafetyScore(
                        score = currentScore,
                        totalThreats = 0, // Simplified for now
                        blockedThreats = 0,
                        userActions = 0,
                        periodStart = System.currentTimeMillis(),
                        periodEnd = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    private fun initializeAI() {
        inferenceEngine = CompositeInferenceEngine(this)
    }

    private fun initializeAgents() {
        val reasoningEngine = SimpleReasoningEngine()
        val agents = listOf(
            ScamAgent(inferenceEngine),
            PrivacyAgent(inferenceEngine),
            CyberbullyingAgent(inferenceEngine),
            MisinformationAgent(inferenceEngine),
            IntentAgent(inferenceEngine),
            GuardianCoachAgent(reasoningEngine)
        )
        guardianCore = GuardianCore(agents, memoryRepository)
    }

    companion object {
        @Volatile
        private var instance: AegisApplication? = null

        fun getInstance(): AegisApplication =
            instance ?: throw IllegalStateException("AegisApplication not initialized")
            
        val guardianCore: GuardianCore
            get() = getInstance().guardianCore
    }
}
