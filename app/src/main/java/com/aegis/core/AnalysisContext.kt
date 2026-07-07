package com.aegis.core

data class AnalysisContext(
    val text: String? = null,
    val sourceApp: String? = null,
    val sourceType: SourceType = SourceType.UNKNOWN,
    val url: String? = null,
    val imagePath: String? = null,
    val audioPath: String? = null,
    val appRiskScore: Float = 0f, // 0 to 1, where 1 is highly risky app
    val isUnknownSender: Boolean = false,
    val conversationHistory: List<String> = emptyList(),
    val metadata: Map<String, String> = emptyMap(),
    val timestamp: Long = System.currentTimeMillis()
)

enum class SourceType {
    SMS,
    NOTIFICATION,
    CLIPBOARD,
    BROWSER,
    EMAIL,
    WHATSAPP,
    TELEGRAM,
    MESSENGER,
    SCREEN,
    FILE,
    IMAGE,
    AUDIO,
    UNKNOWN
}
