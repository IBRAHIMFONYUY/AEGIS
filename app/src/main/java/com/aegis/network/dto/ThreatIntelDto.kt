package com.aegis.network.dto

class ThreatIntelDto {

    data class ThreatReport(
        val threatType: String,
        val contentHash: String,
        val contentPreview: String,
        val sourceApp: String?,
        val deviceId: String?
    )

    data class ReportResponse(
        val success: Boolean,
        val threatId: String?,
        val message: String
    )

    data class BlocklistResponse(
        val domains: List<String>,
        val ips: List<String>,
        val hashes: List<String>,
        val updatedAt: Long
    )

    data class BlocklistData(
        val domains: List<String>,
        val ips: List<String>,
        val hashes: List<String>,
        val updatedAt: Long
    )

    data class UrlCheckResponse(
        val isMalicious: Boolean,
        val confidence: Float,
        val category: String?,
        val threatType: String?
    )

    data class UrlCheckResult(
        val isMalicious: Boolean,
        val confidence: Float,
        val category: String?,
        val threatType: String?
    )

    data class RecentThreatsResponse(
        val threats: List<ThreatSummary>,
        val total: Int
    )

    data class ThreatSummary(
        val id: String,
        val threatType: String,
        val severity: String,
        val timestamp: Long,
        val region: String?
    )

    data class AnalyticsEvent(
        val eventType: String,
        val value: String?,
        val timestamp: Long = System.currentTimeMillis()
    )

    data class AnalyticsResponse(
        val success: Boolean
    )

    data class ModelUpdateResponse(
        val hasUpdate: Boolean,
        val modelType: String,
        val newVersion: String,
        val downloadUrl: String,
        val fileSize: Long,
        val required: Boolean
    )

    data class ModelUpdate(
        val modelType: String,
        val newVersion: String,
        val downloadUrl: String,
        val fileSize: Long,
        val required: Boolean
    )
}
