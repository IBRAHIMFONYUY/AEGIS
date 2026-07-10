package com.aegis.ai

import android.content.Context
import com.aegis.ai.safety.SafetyClassifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Optimized AI Operation Manager for high performance and quota management.
 * Implements a Multi-Tiered "Filter Funnel" to minimize expensive API calls:
 * Tier 0: Rule-Based (Local)
 * Tier 1: Semantic Cache (Local)
 * Tier 2: AI Flash Model (Cloud)
 */
class AIOperationManager(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val operationMutex = Mutex()
    private val safetyClassifier = SafetyClassifier()

    // Response cache for frequently requested analyses
    private val responseCache = ConcurrentHashMap<String, CachedResponse>()
    private val cacheMutex = Mutex()

    // Quota tracking
    private val apiCallCount = AtomicLong(0)
    private val lastApiResetTime = AtomicLong(System.currentTimeMillis())
    private val MAX_CALLS_PER_MINUTE = 15 // Protection against runaway loops

    // Cache metrics
    private val cacheHits = AtomicLong(0)
    private val cacheMisses = AtomicLong(0)

    // Performance monitoring
    private val _performanceMetrics = MutableSharedFlow<PerformanceMetric>()
    val performanceMetrics: SharedFlow<PerformanceMetric> = _performanceMetrics.asSharedFlow()

    // AI Engine references
    private var gemmaEngine: GemmaInferenceEngine? = null
    private var geminiAIManager: GeminiAIManager? = null

    companion object {
        private const val CACHE_TTL_MS = 30 * 60 * 1000L // 30 minutes for security results
        private const val MAX_CACHE_SIZE = 500
    }

    fun setEngines(gemmaEngine: GemmaInferenceEngine?, geminiAIManager: GeminiAIManager?) {
        this.gemmaEngine = gemmaEngine
        this.geminiAIManager = geminiAIManager
    }

    /**
     * The Funnel: This is the core of AEGIS's intelligence management.
     * It ensures we only use the Cloud AI when absolutely necessary.
     */
    suspend fun analyzeThreatOptimized(
        text: String,
        metadata: Map<String, String> = emptyMap()
    ): ThreatAnalysisResult {
        val startTime = System.currentTimeMillis()
        val textHash = text.trim().lowercase().hashCode().toString()
        val operationKey = "threat_$textHash"

        // --- TIER 0: LOCAL SAFETY CLASSIFIER (ZERO COST) ---
        val localSafety = safetyClassifier.classify(text)
        
        // Immediate detection for clear, dangerous threats (e.g. death threats)
        if (localSafety.isUnsafe && localSafety.confidence > 0.8f && 
            (localSafety.category == SafetyClassifier.SafetyCategory.HARASSMENT || 
             localSafety.category == SafetyClassifier.SafetyCategory.SCAM_FRAUD)) {
            
            Timber.tag("AIOperation").d("Tier 0: Local Critical Hit - ${localSafety.category}")
            return localSafety.toThreatResult("AEGIS Real-Time Guardian")
        }

        // --- TIER 1: CACHE CHECK (ZERO COST) ---
        val cached = getCachedResponse<ThreatAnalysisResult>(operationKey)
        if (cached != null) {
            Timber.tag("AIOperation").d("Tier 1: Cache Hit")
            emitMetric(operationKey, System.currentTimeMillis() - startTime, true)
            return cached
        }

        // --- TIER 2: CLOUD AI (ONLY IF TRIGGERED OR CHATBOT) ---
        val shouldCallAI = metadata["force_ai_analysis"] == "true" || 
                          metadata["source_type"] == "CHATBOT" ||
                          metadata["deep_scan"] == "true"

        if (shouldCallAI) {
            return executeOperation(
                operationKey = operationKey,
                operation = {
                    if (isQuotaExceeded()) {
                        Timber.tag("AIOperation").w("Quota Exceeded - Falling back")
                        return@executeOperation localSafety.toThreatResult("Cloud Quota Exceeded")
                    }

                    if (geminiAIManager != null) {
                        try {
                            apiCallCount.incrementAndGet()
                            val contextStr = metadata.entries.joinToString("\n") { "${it.key}: ${it.value}" }
                            val result = geminiAIManager!!.analyzeThreat(text, contextStr)
                            cacheResponse(operationKey, result)
                            return@executeOperation result
                        } catch (e: Exception) {
                            Timber.tag("AIOperation").e(e, "Gemini API failure")
                        }
                    }
                    localSafety.toThreatResult("Local Fallback")
                },
                useCache = false
            )
        }

        // Default: If not critical and not forced, remain local to save quota
        return localSafety.toThreatResult("AEGIS Baseline Protection")
    }

    /**
     * Execute AI operation with internal locking and performance tracking
     */
    private suspend fun <T> executeOperation(
        operationKey: String,
        operation: suspend () -> T,
        useCache: Boolean = true
    ): T {
        val startTime = System.currentTimeMillis()

        if (useCache) {
            val cached = getCachedResponse<T>(operationKey)
            if (cached != null) return cached
        }

        return operationMutex.withLock {
            if (useCache) {
                val cached = getCachedResponse<T>(operationKey)
                if (cached != null) return cached
            }

            val result = operation()
            if (useCache) cacheResponse(operationKey, result)

            emitMetric(operationKey, System.currentTimeMillis() - startTime, false)
            result
        }
    }

    suspend fun analyzeConversationOptimized(
        messages: List<String>,
        currentMessage: String,
        senderInfo: String = ""
    ): ConversationAnalysisResult {
        val recentMessages = messages.takeLast(10)
        val operationKey = "conv_${recentMessages.hashCode()}_$currentMessage"

        return executeOperation(
            operationKey = operationKey,
            operation = {
                if (geminiAIManager != null && !isQuotaExceeded()) {
                    try {
                        apiCallCount.incrementAndGet()
                        return@executeOperation geminiAIManager!!.analyzeConversationHistory(
                            messages,
                            currentMessage,
                            senderInfo
                        )
                    } catch (e: Exception) {
                        Timber.tag("AIOperation").e(e, "Conversation analysis failed")
                    }
                }

                ConversationAnalysisResult(
                    isSuspicious = false,
                    threatType = null,
                    confidence = 0f,
                    analysis = "Deep analysis unavailable (Quota or Offline)",
                    recommendedActions = emptyList(),
                    riskFactors = emptyList()
                )
            }
        )
    }

    private fun isQuotaExceeded(): Boolean {
        val now = System.currentTimeMillis()
        if (now - lastApiResetTime.get() > 60000) {
            apiCallCount.set(0)
            lastApiResetTime.set(now)
            return false
        }
        return apiCallCount.get() >= MAX_CALLS_PER_MINUTE
    }

    private fun SafetyClassifier.SafetyResult.toThreatResult(reasonPrefix: String): ThreatAnalysisResult {
        val guidance = when (this.category) {
            SafetyClassifier.SafetyCategory.HARASSMENT -> "Safety Alert: This message contains violent language or threats. AEGIS recommends blocking the sender immediately and documenting this for your safety."
            SafetyClassifier.SafetyCategory.SCAM_FRAUD -> "Fraud Alert: This message matches common scam patterns. Do not send money, provide gift cards, or share OTPs."
            SafetyClassifier.SafetyCategory.PHISHING -> "Phishing Alert: This app is asking for sensitive credentials in a suspicious way. Verify the source before typing any passwords."
            else -> "AEGIS Local Guardian detected suspicious patterns. Please exercise caution."
        }
        
        return ThreatAnalysisResult(
            isThreat = this.isUnsafe,
            threatType = this.category.name.lowercase(),
            confidence = this.confidence,
            reason = "$reasonPrefix: ${this.category}",
            guidance = guidance,
            appContext = null
        )
    }

    private suspend fun <T> getCachedResponse(key: String): T? {
        return cacheMutex.withLock {
            val cached = responseCache[key]
            if (cached != null && System.currentTimeMillis() - cached.timestamp < CACHE_TTL_MS) {
                cacheHits.incrementAndGet()
                @Suppress("UNCHECKED_CAST")
                cached.data as? T
            } else {
                cacheMisses.incrementAndGet()
                null
            }
        }
    }

    private suspend fun <T> cacheResponse(key: String, data: T) {
        cacheMutex.withLock {
            if (responseCache.size >= MAX_CACHE_SIZE) {
                val oldestKey = responseCache.entries.minByOrNull { it.value.timestamp }?.key
                oldestKey?.let { responseCache.remove(it) }
            }
            responseCache[key] = CachedResponse(data, System.currentTimeMillis())
        }
    }

    private fun emitMetric(operationKey: String, durationMs: Long, fromCache: Boolean) {
        scope.launch {
            _performanceMetrics.emit(
                PerformanceMetric(
                    operationKey = operationKey,
                    durationMs = durationMs,
                    fromCache = fromCache,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    fun cleanup() {
        scope.launch {
            cacheMutex.withLock {
                responseCache.clear()
            }
        }
    }
}

data class CachedResponse(
    val data: Any?,
    val timestamp: Long
)

data class PerformanceMetric(
    val operationKey: String,
    val durationMs: Long,
    val fromCache: Boolean,
    val timestamp: Long
)

data class CacheStats(
    val size: Int,
    val maxSize: Int,
    val hitRate: Float
)
