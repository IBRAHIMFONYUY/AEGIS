package com.aegis.agents

import com.aegis.ai.InferenceEngine
import com.aegis.core.*
import com.aegis.data.repository.GuardianMemoryRepository
import com.aegis.network.ThreatIntelClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PhishingAgent(
    private val inferenceEngine: InferenceEngine? = null,
    private val threatIntelClient: ThreatIntelClient? = null
) : GuardianAgent {

    override val name = "PhishingAgent"
    override val version = "1.0.0"
    override val description = "Specialized detection for phishing links, deceptive domains, and credential harvesting"

    private val suspiciousDomainPatterns = listOf(
        Regex("""\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}"""), // IP address instead of domain
        Regex("""-login\."""),
        Regex("""-secure\."""),
        Regex("""-verify\."""),
        Regex("""update-"""),
        Regex("""confirm-"""),
        Regex("""account-"""),
        Regex("""support-""")
    )

    private val brandImpersonationPatterns = mapOf(
        "paypal" to listOf("paypa1", "pay-pal", "paypal-auth", "secure-paypal"),
        "google" to listOf("g00gle", "google-security", "gmail-verify"),
        "microsoft" to listOf("msoft", "microsoft-online", "outlook-confirm"),
        "apple" to listOf("apple-id", "icloud-verify", "apple-support"),
        "facebook" to listOf("face-book", "fb-login", "meta-verify"),
        "amazon" to listOf("amzn", "amazon-orders", "prime-verify"),
        "netflix" to listOf("nflx", "netflix-billing")
    )

    override suspend fun analyze(
        context: AnalysisContext,
        memory: GuardianMemoryRepository?,
        previousResults: List<AgentResult>
    ): AgentResult = withContext(Dispatchers.Default) {
        val url = context.url ?: extractUrl(context.text) ?: return@withContext safeResult
        
        var phishingScore = 0f
        val details = mutableMapOf<String, String>()
        details["analyzed_url"] = url

        // 1. Basic URL Heuristics
        if (!url.startsWith("https://")) {
            phishingScore += 0.3f
            details["insecure_protocol"] = "true"
        }

        val domain = getDomain(url)
        if (domain.count { it == '.' } > 3) {
            phishingScore += 0.2f
            details["excessive_subdomains"] = "true"
        }

        if (suspiciousDomainPatterns.any { it.containsMatchIn(domain) }) {
            phishingScore += 0.4f
            details["suspicious_domain_pattern"] = "true"
        }

        // 2. Brand Impersonation Check
        brandImpersonationPatterns.forEach { (brand, patterns) ->
            if (patterns.any { domain.contains(it) }) {
                phishingScore += 0.6f
                details["impersonating_brand"] = brand
            }
        }

        // 3. Threat Intelligence Check
        val intelResult = threatIntelClient?.checkUrl(url)
        if (intelResult?.isMalicious == true) {
            phishingScore += intelResult.confidence
            details["threat_intel_flagged"] = "true"
            details["intel_category"] = intelResult.category ?: "malicious"
        }

        // 4. ML Analysis
        val mlScore = if (inferenceEngine is com.aegis.ai.GemmaInferenceEngine) {
            inferenceEngine.detectScam(url, context.metadata)
        } else {
            inferenceEngine?.classify(url, "phishing_detection") ?: 0f
        }
        phishingScore = (phishingScore * 0.4f + mlScore * 0.6f).coerceIn(0f, 1f)

        val threatLevel = scoreToThreatLevel(phishingScore)

        AgentResult(
            agentName = name,
            threatLevel = threatLevel,
            confidence = phishingScore,
            reason = buildReason(threatLevel, details),
            details = details,
            suggestedAction = getAdvice(threatLevel, details),
            requiresUserAttention = threatLevel.value >= ThreatLevel.MALICIOUS.value
        )
    }

    private fun extractUrl(text: String?): String? {
        if (text == null) return null
        val regex = Regex("""https?://[^\s<>"]+|www\.[^\s<>"]+""")
        return regex.find(text)?.value
    }

    private fun getDomain(url: String): String {
        return url.removePrefix("https://")
            .removePrefix("http://")
            .removePrefix("www.")
            .split('/')
            .firstOrNull() ?: ""
    }

    private fun scoreToThreatLevel(score: Float): ThreatLevel = when {
        score >= 0.8f -> ThreatLevel.CRITICAL
        score >= 0.6f -> ThreatLevel.MALICIOUS
        score >= 0.4f -> ThreatLevel.SUSPICIOUS
        else -> ThreatLevel.SAFE
    }

    private fun buildReason(level: ThreatLevel, details: Map<String, String>): String {
        return when {
            details.containsKey("impersonating_brand") -> "Potential phishing attempt impersonating ${details["impersonating_brand"]?.replaceFirstChar { it.uppercase() }}."
            details.containsKey("threat_intel_flagged") -> "URL confirmed as malicious by threat intelligence databases."
            details.containsKey("suspicious_domain_pattern") -> "Domain uses patterns commonly associated with credential harvesting."
            level == ThreatLevel.CRITICAL -> "Critical phishing threat detected. This URL is highly likely to be a scam."
            level == ThreatLevel.MALICIOUS -> "Malicious link detected. Do not enter any credentials."
            else -> "No phishing indicators found in link."
        }
    }

    private fun getAdvice(level: ThreatLevel, details: Map<String, String>): String? {
        if (level == ThreatLevel.SAFE) return null
        return when {
            details.containsKey("impersonating_brand") -> "This site looks like ${details["impersonating_brand"]}, but the URL is incorrect. Navigate to the official website manually."
            else -> "Do not provide your password, payment info, or personal data to this website. Close the page immediately."
        }
    }

    private val safeResult get() = AgentResult(
        agentName = name,
        threatLevel = ThreatLevel.SAFE,
        confidence = 0f,
        reason = "No link to analyze"
    )
}
