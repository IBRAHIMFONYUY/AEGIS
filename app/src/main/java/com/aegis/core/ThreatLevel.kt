package com.aegis.core

enum class ThreatLevel(val value: Int) {
    SAFE(0),
    SUSPICIOUS(1),
    LIKELY_MALICIOUS(2),
    MALICIOUS(3),
    CRITICAL(4);

    val label: String
        get() = when (this) {
            SAFE -> "Safe"
            SUSPICIOUS -> "Suspicious"
            LIKELY_MALICIOUS -> "Likely Malicious"
            MALICIOUS -> "Malicious"
            CRITICAL -> "Critical"
        }
}

fun ThreatLevel.toComposeColor(): Long = when (this) {
    ThreatLevel.SAFE -> 0xFF4CAF50
    ThreatLevel.SUSPICIOUS -> 0xFFFFC107
    ThreatLevel.LIKELY_MALICIOUS -> 0xFFFF9800
    ThreatLevel.MALICIOUS -> 0xFFF44336
    ThreatLevel.CRITICAL -> 0xFFD32F2F
}
