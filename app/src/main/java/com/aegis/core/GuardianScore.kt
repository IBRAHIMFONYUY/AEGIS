package com.aegis.core

data class GuardianScore(
    val overall: Float,
    val privacy: Float,
    val scamProtection: Float,
    val deviceSecurity: Float,
    val digitalWellbeing: Float,
    val trend: ScoreTrend = ScoreTrend.STABLE
)

enum class ScoreTrend {
    IMPROVING,
    STABLE,
    DECLINING
}

enum class ScoreCategory(val displayName: String, val icon: String) {
    PRIVACY("Privacy", "🛡️"),
    SCAM_PROTECTION("Scam Protection", "🚫"),
    DEVICE_SECURITY("Device Security", "📱"),
    DIGITAL_WELLBEING("Digital Wellbeing", "🧘")
}
