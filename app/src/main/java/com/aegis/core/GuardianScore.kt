package com.aegis.core

data class GuardianScore(
    val overall: Float,
    val privacy: Float,
    val scamProtection: Float,
    val deviceSecurity: Float,
    val digitalWellbeing: Float
)
