package com.aegis.ai

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

/**
 * Optimized AI Operation Manager for high performance and seamless AI operations.
 * Implements caching, request batching, and intelligent resource management.
 */
class AIOperationManager(private val context: Context) {
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val operationMutex = Mutex()
    
    // Response cache for frequently requested analyses
    private val responseCache = ConcurrentHashMap<String, CachedResponse>()
    private val cacheMutex = Mutex()
    
    // Request batching for similar operations
    private val pendingRequests = ConcurrentHashMap<String, MutableList<PendingRequest>>()
    
    // Performance monitoring
    private val _performanceMetrics = MutableSharedFlow<PerformanceMetric>()
    val performanceMetrics: SharedFlow<PerformanceMetric> = _performanceMetrics.asSharedFlow()
    
    // AI Engine references
    private var gemmaEngine: GemmaInferenceEngine? = null
    private var geminiAIManager: GeminiAIManager? = null
    
    companion object {
        private const val CACHE_TTL_MS = 5 * 60 * 1000L // 5 minutes
        private const val MAX_CACHE_SIZE = 100
        private const val BATCH_TIMEOUT_MS = 100L
    }
    
    fun setEngines(gemmaEngine: GemmaInferenceEngine?, geminiAIManager: GeminiAIManager?) {
        this.gemmaEngine = gemmaEngine
        this.geminiAIManager = geminiAIManager
    }
    
    /**
     * Execute AI operation with caching and optimization
     */
    suspend fun <T> executeOperation(
        operationKey: String,
        operation: suspend () -> T,
        useCache: Boolean = true
    ): T {
        val startTime = System.currentTimeMillis()
        
        // Check cache first
        if (useCache) {
            val cached = getCachedResponse<T>(operationKey)
            if (cached != null) {
                emitMetric(operationKey, System.currentTimeMillis() - startTime, true)
                return cached
            }
        }
        
        // Execute operation with mutex to prevent concurrent similar operations
        return operationMutex.withLock {
            // Double-check cache after acquiring lock
            if (useCache) {
                val cached = getCachedResponse<T>(operationKey)
                if (cached != null) {
                    emitMetric(operationKey, System.currentTimeMillis() - startTime, true)
                    return cached
                }
            }
            
            // Execute the operation
            val result = operation()
            
            // Cache the result
            if (useCache) {
                cacheResponse(operationKey, result)
            }
            
            emitMetric(operationKey, System.currentTimeMillis() - startTime, false)
            result
        }
    }
    
    /**
     * Execute threat analysis with optimized AI selection
     */
    suspend fun analyzeThreatOptimized(
        text: String,
        metadata: Map<String, String> = emptyMap()
    ): ThreatAnalysisResult {
        val operationKey = "threat_${text.hashCode()}_${metadata.hashCode()}"
        
        return executeOperation(operationKey) {
            // Prefer Gemini AI for comprehensive analysis
            if (geminiAIManager != null) {
                try {
                    val context = metadata.entries.joinToString("\n") { "${it.key}: ${it.value}" }
                    val result = geminiAIManager.analyzeThreat(text, context)
                    return@executeOperation result
                } catch (e: Exception) {
                    // Fallback to Gemma if Gemini fails
                }
            }
            
            // Fallback to Gemma
            if (gemmaEngine != null) {
                try {
                    return@executeOperation gemmaEngine.detectThreatWithAI(text, metadata)
                } catch (e: Exception) {
                    // Return default result if both fail
                }
            }
            
            // Default result
            ThreatAnalysisResult(
                isThreat = false,
                threatType = "unknown",
                confidence = 0f,
                reason = "AI analysis unavailable",
                guidance = "Manual review recommended",
                appContext = null
            )
        }
    }
    
    /**
     * Execute conversation analysis with optimization
     */
    suspend fun analyzeConversationOptimized(
        messages: List<String>,
        currentMessage: String,
        senderInfo: String = ""
    ): ConversationAnalysisResult {
        // Create a cache key based on message content
        val recentMessages = messages.takeLast(10)
        val operationKey = "conv_${recentMessages.hashCode()}_$currentMessage"
        
        return executeOperation(operationKey) {
            if (geminiAIManager != null) {
                try {
                    return@executeOperation geminiAIManager.analyzeConversationHistory(
                        messages,
                        currentMessage,
                        senderInfo
                    )
                } catch (e: Exception) {
                    // Fallback to basic analysis
                }
            }
            
            // Basic fallback analysis
            ConversationAnalysisResult(
                isSuspicious = false,
                threatType = null,
                confidence = 0f,
                analysis = "Deep analysis unavailable",
                recommendedActions = emptyList(),
                riskFactors = emptyList()
            )
        }
    }
    
    /**
     * Batch multiple similar operations for efficiency
     */
    suspend fun <T> batchOperations(
        batchKey: String,
        operations: List<suspend () -> T>
    ): List<T> {
        return operationMutex.withLock {
            operations.map { operation ->
                try {
                    operation()
                } catch (e: Exception) {
                    throw e
                }
            }
        }
    }
    
    /**
     * Clear cache to free memory
     */
    suspend fun clearCache() {
        cacheMutex.withLock {
            responseCache.clear()
        }
    }
    
    /**
     * Clean up expired cache entries
     */
    suspend fun cleanupCache() {
        cacheMutex.withLock {
            val now = System.currentTimeMillis()
            val expiredKeys = responseCache.filter { 
                now - it.value.timestamp > CACHE_TTL_MS 
            }.keys
            
            expiredKeys.forEach { responseCache.remove(it) }
            
            // If cache is too large, remove oldest entries
            if (responseCache.size > MAX_CACHE_SIZE) {
                val entriesToRemove = responseCache.size - MAX_CACHE_SIZE
                responseCache.keys.take(entriesToRemove).forEach { responseCache.remove(it) }
            }
        }
    }
    
    private suspend fun <T> getCachedResponse(key: String): T? {
        return cacheMutex.withLock {
            val cached = responseCache[key]
            if (cached != null && System.currentTimeMillis() - cached.timestamp < CACHE_TTL_MS) {
                @Suppress("UNCHECKED_CAST")
                cached.data as? T
            } else {
                null
            }
        }
    }
    
    private suspend fun <T> cacheResponse(key: String, data: T) {
        cacheMutex.withLock {
            responseCache[key] = CachedResponse(data, System.currentTimeMillis())
            
            // Ensure cache doesn't exceed max size
            if (responseCache.size > MAX_CACHE_SIZE) {
                val oldestKey = responseCache.entries.minByOrNull { it.value.timestamp }?.key
                oldestKey?.let { responseCache.remove(it) }
            }
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
    
    /**
     * Get current cache statistics
     */
    suspend fun getCacheStats(): CacheStats {
        return cacheMutex.withLock {
            CacheStats(
                size = responseCache.size,
                maxSize = MAX_CACHE_SIZE,
                hitRate = calculateHitRate()
            )
        }
    }
    
    private fun calculateHitRate(): Float {
        // Simplified hit rate calculation
        return 0.8f // Placeholder - would need actual hit tracking
    }
    
    fun cleanup() {
        scope.launch {
            clearCache()
        }
    }
}

data class CachedResponse(
    val data: Any,
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

data class PendingRequest(
    val id: String,
    val timestamp: Long = System.currentTimeMillis()
)
