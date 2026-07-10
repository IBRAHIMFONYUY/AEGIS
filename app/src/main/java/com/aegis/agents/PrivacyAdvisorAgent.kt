package com.aegis.agents

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.aegis.ai.ReasoningEngine
import com.aegis.core.*
import com.aegis.data.repository.GuardianMemoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class PrivacyAdvisorAgent(
    private val context: Context,
    private val reasoningEngine: ReasoningEngine? = null
) : GuardianAgent {

    override val name = "PrivacyAdvisor"
    override val version = "1.0.0"
    override val description = "Analyzes installed apps and their permissions to generate a Privacy Risk Report"

    override suspend fun analyze(
        context: AnalysisContext, 
        memory: GuardianMemoryRepository?,
        previousResults: List<AgentResult>
    ): AgentResult = withContext(Dispatchers.Default) {
        // If the context specifically requests a privacy report
        if (context.metadata["request_privacy_report"] == "true") {
            return@withContext generatePrivacyReport(context.metadata)
        }
        
        // Otherwise, it's a passive check during other analyses
        return@withContext safeResult
    }

    private suspend fun generatePrivacyReport(metadata: Map<String, String>): AgentResult {
        val pm = context.packageManager
        val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        
        val riskyApps = mutableListOf<String>()
        
        // Filter for non-system apps with high-risk permissions
        for (app in installedApps) {
            if (app.flags and ApplicationInfo.FLAG_SYSTEM == 0) {
                val packageInfo = try {
                    pm.getPackageInfo(app.packageName, PackageManager.GET_PERMISSIONS)
                } catch (e: Exception) {
                    null
                }
                
                val requestedPermissions = packageInfo?.requestedPermissions ?: emptyArray()
                
                val riskScore = calculateAppRisk(requestedPermissions)
                if (riskScore > 0.7f) {
                    riskyApps.add("${pm.getApplicationLabel(app)} (${app.packageName}) - Risk: High")
                } else if (riskScore > 0.4f) {
                    riskyApps.add("${pm.getApplicationLabel(app)} (${app.packageName}) - Risk: Medium")
                }
            }
        }

        val appSummary = riskyApps.take(15).joinToString("\n")
        
        val prompt = """
            You are the AEGIS Privacy Advisor. I have analyzed the installed apps on this device.
            Here is a list of non-system apps with their calculated privacy risk levels based on requested permissions:
            
            $appSummary
            
            Based on this information, generate a professional, concise Privacy Risk Report.
            Include:
            1. An overall privacy health score (0-100).
            2. Top 3 most concerning apps and WHY (e.g., "App X requests background location but it's just a calculator").
            3. Actionable advice on how to improve device privacy (e.g., "Review microphone permissions for social media apps").
            
            Keep it actionable and helpful.
        """.trimIndent()

        val report = reasoningEngine?.generateResponse(prompt, null, metadata)
            ?: "Analysis complete. Detected ${riskyApps.size} apps with potential privacy concerns. Please review your app permissions manually in Settings."

        return AgentResult(
            agentName = name,
            threatLevel = if (riskyApps.isNotEmpty()) ThreatLevel.SUSPICIOUS else ThreatLevel.SAFE,
            confidence = 0.9f,
            reason = "Privacy Risk Report Generated",
            details = mapOf("risky_apps_count" to riskyApps.size.toString()),
            suggestedAction = report
        )
    }

    private fun calculateAppRisk(permissions: Array<String>): Float {
        var score = 0f
        val highRiskPermissions = listOf(
            "android.permission.READ_SMS",
            "android.permission.RECEIVE_SMS",
            "android.permission.READ_CALL_LOG",
            "android.permission.READ_CONTACTS",
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.ACCESS_BACKGROUND_LOCATION",
            "android.permission.RECORD_AUDIO",
            "android.permission.CAMERA",
            "android.permission.PROCESS_OUTGOING_CALLS"
        )
        
        permissions.forEach { perm ->
            if (perm in highRiskPermissions) {
                score += 0.15f
            }
        }
        
        return score.coerceIn(0f, 1f)
    }

    private val safeResult get() = AgentResult(
        agentName = name,
        threatLevel = ThreatLevel.SAFE,
        confidence = 0f,
        reason = "No privacy report requested"
    )
}
