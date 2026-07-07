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

enum class ScoreCategory {
    PRIVACY,
    SCAM_PROTECTION,
    DEVICE_SECURITY,
    DIGITAL_WELLBEING
}
