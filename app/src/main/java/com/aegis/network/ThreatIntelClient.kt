package com.aegis.network

import android.content.Context
import com.aegis.network.dto.ThreatIntelDto
import kotlinx.coroutines.Dispatchers
import java.security.MessageDigest
import kotlinx.coroutines.withContext

class ThreatIntelClient(private val context: Context) {

    companion object {
        private const val DEFAULT_BASE_URL = "https://api.aegis-security.app/"
    }

    private var apiService: ApiService? = null
    private var isEnabled = false

    fun initialize(baseUrl: String = DEFAULT_BASE_URL) {
        apiService = ApiService.create(baseUrl)
        isEnabled = true
    }

    suspend fun checkUrl(url: String): ThreatIntelDto.UrlCheckResult? = withContext(Dispatchers.IO) {
        if (!isEnabled) return@withContext null
        try {
            val response = apiService?.checkPhishingUrl(url) ?: return@withContext null
            ThreatIntelDto.UrlCheckResult(
                isMalicious = response.isMalicious,
                confidence = response.confidence,
                category = response.category,
                threatType = response.threatType
            )
        } catch (e: Exception) {
            null
        }
    }

    suspend fun reportThreat(
        threatType: String,
        content: String,
        sourceApp: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        if (!isEnabled) return@withContext false
        try {
            val report = ThreatIntelDto.ThreatReport(
                threatType = threatType,
                contentHash = MessageDigest.getInstance("SHA-256")
                    .digest(content.toByteArray())
                    .joinToString("") { "%02x".format(it) },
                contentPreview = content.take(200),
                sourceApp = sourceApp,
                deviceId = null
            )
            val response = apiService?.reportThreat(report) ?: return@withContext false
            response.success
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getBlocklist(since: Long = 0): ThreatIntelDto.BlocklistData? = withContext(Dispatchers.IO) {
        if (!isEnabled) return@withContext null
        try {
            val response = apiService?.getBlocklist(since) ?: return@withContext null
            ThreatIntelDto.BlocklistData(
                domains = response.domains,
                ips = response.ips,
                hashes = response.hashes,
                updatedAt = response.updatedAt
            )
        } catch (e: Exception) {
            null
        }
    }

    suspend fun checkForModelUpdates(currentVersion: String): ThreatIntelDto.ModelUpdate? = withContext(Dispatchers.IO) {
        if (!isEnabled) return@withContext null
        try {
            val response = apiService?.checkModelUpdate(currentVersion) ?: return@withContext null
            if (response.hasUpdate) {
                ThreatIntelDto.ModelUpdate(
                    modelType = response.modelType,
                    newVersion = response.newVersion,
                    downloadUrl = response.downloadUrl,
                    fileSize = response.fileSize,
                    required = response.required
                )
            } else null
        } catch (e: Exception) {
            null
        }
    }

    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
    }

    fun isEnabled(): Boolean = isEnabled
}
