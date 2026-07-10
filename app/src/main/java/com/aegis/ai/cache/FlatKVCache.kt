package com.aegis.ai.cache

class KVCache(
    private val numLayers: Int,
    private val numHeads: Int,
    private val headDim: Int,
    private val maxSequenceLength: Int
) {
    // Key and value caches for each layer
    private val keyCache: Array<Array<FloatArray>> = Array(numLayers) {
        Array(numHeads) { FloatArray(maxSequenceLength * headDim) }
    }
    private val valueCache: Array<Array<FloatArray>> = Array(numLayers) {
        Array(numHeads) { FloatArray(maxSequenceLength * headDim) }
    }
    
    // Track current sequence length
    private var currentLength = 0
    
    // Statistics
    private var cacheHits = 0
    private var cacheMisses = 0
    
    fun update(layer: Int, head: Int, position: Int, key: FloatArray, value: FloatArray) {
        if (layer < 0 || layer >= numLayers) return
        if (head < 0 || head >= numHeads) return
        if (position < 0 || position >= maxSequenceLength) return
        if (key.size != headDim || value.size != headDim) return
        
        val offset = position * headDim
        System.arraycopy(key, 0, keyCache[layer][head], offset, headDim)
        System.arraycopy(value, 0, valueCache[layer][head], offset, headDim)
        
        currentLength = maxOf(currentLength, position + 1)
        cacheMisses++
    }
    
    fun getKeys(layer: Int, head: Int): FloatArray? {
        if (layer < 0 || layer >= numLayers) return null
        if (head < 0 || head >= numHeads) return null
        
        cacheHits++
        return keyCache[layer][head]
    }
    
    fun getValues(layer: Int, head: Int): FloatArray? {
        if (layer < 0 || layer >= numLayers) return null
        if (head < 0 || head >= numHeads) return null
        
        cacheHits++
        return valueCache[layer][head]
    }
    
    fun getKey(layer: Int, head: Int, position: Int): FloatArray? {
        if (layer < 0 || layer >= numLayers) return null
        if (head < 0 || head >= numHeads) return null
        if (position < 0 || position >= currentLength) return null
        
        val offset = position * headDim
        val result = FloatArray(headDim)
        System.arraycopy(keyCache[layer][head], offset, result, 0, headDim)
        
        cacheHits++
        return result
    }
    
    fun getValue(layer: Int, head: Int, position: Int): FloatArray? {
        if (layer < 0 || layer >= numLayers) return null
        if (head < 0 || head >= numHeads) return null
        if (position < 0 || position >= currentLength) return null
        
        val offset = position * headDim
        val result = FloatArray(headDim)
        System.arraycopy(valueCache[layer][head], offset, result, 0, headDim)
        
        cacheHits++
        return result
    }
    
    fun reset() {
        for (layer in 0 until numLayers) {
            for (head in 0 until numHeads) {
                keyCache[layer][head].fill(0f)
                valueCache[layer][head].fill(0f)
            }
        }
        currentLength = 0
    }
    
    fun getCurrentLength(): Int = currentLength
    
    fun getCacheStats(): CacheStats {
        val total = cacheHits + cacheMisses
        val hitRate = if (total > 0) cacheHits.toFloat() / total else 0f
        
        return CacheStats(
            cacheHits = cacheHits,
            cacheMisses = cacheMisses,
            hitRate = hitRate,
            currentLength = currentLength,
            maxSequenceLength = maxSequenceLength
        )
    }
    
    fun getMemoryUsageBytes(): Long {
        // Calculate memory usage: 2 caches * numLayers * numHeads * maxSeqLen * headDim * 4 bytes (float)
        return 2L * numLayers * numHeads * maxSequenceLength * headDim * 4
    }
}

data class CacheStats(
    val cacheHits: Int,
    val cacheMisses: Int,
    val hitRate: Float,
    val currentLength: Int,
    val maxSequenceLength: Int
)
