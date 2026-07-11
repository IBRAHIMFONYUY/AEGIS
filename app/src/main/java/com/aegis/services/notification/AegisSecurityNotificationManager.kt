package com.aegis.services.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import timber.log.Timber
import androidx.core.app.NotificationCompat
import com.aegis.core.AnalysisResult
import com.aegis.core.ThreatLevel
import dagger.hilt.android.qualifiers.ApplicationContext
import com.aegis.services.overlay.ThreatOverlayManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AegisSecurityNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val overlayManager: ThreatOverlayManager
) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val CHANNEL_ID = "aegis_security_alerts"
    
    // Shared deduplication map: Key = normalized message hash, Value = timestamp of last alert
    private val recentAlerts = mutableMapOf<Int, Long>()
    private val ALERT_COOLDOWN_MS = 10_000L // Reduced for testing

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "AEGIS Security Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Urgent alerts for scams, fraud, and cyber threats"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showSecurityAlert(result: AnalysisResult) {
        val level = result.overallThreatLevel
        if (level == ThreatLevel.SAFE) return

        val rawText = result.context.text ?: return
        
        // Normalize text to ignore jitter (timestamps, spaces, case)
        val normalizedText = rawText.lowercase()
            .replace(Regex("\\d{1,2}:\\d{2}\\s?(?:am|pm)?"), "") // Remove timestamps
            .replace(Regex("\\s+"), " ") // Normalize spaces
            .trim()
        if (normalizedText.length < 5) return

        val msgHash = normalizedText.hashCode()
        
        // --- CONSOLIDATED DEDUPLICATION ---
        val now = System.currentTimeMillis()
        val lastAlert = recentAlerts[msgHash]
        if (lastAlert != null && (now - lastAlert) < ALERT_COOLDOWN_MS) {
            return // Skip duplicate within cooldown
        }
        recentAlerts[msgHash] = now

        val sender = result.context.metadata["sender"] ?: result.context.sourceApp ?: "Unknown App"
        val threatType = result.agentResults.maxByOrNull { it.threatLevel.value }?.agentName ?: "Cyber Threat"
        val confidence = (result.agentResults.maxByOrNull { it.threatLevel.value }?.confidence ?: 0.5f) * 100
        val guidance = result.agentResults.maxByOrNull { it.threatLevel.value }?.suggestedAction ?: "Exercise extreme caution."

        val title = "⚠ AEGIS: ${level.label.uppercase()} DETECTED"
        val messageContent = result.context.text ?: "No content"
        
        // --- LEGITIMACY FILTER ---
        val maxConfidence = result.agentResults.maxOfOrNull { it.confidence } ?: 0f
        
        // Show overlay if it's LIKELY_MALICIOUS or higher and we have at least 50% confidence
        val shouldShowOverlay = level.value >= ThreatLevel.LIKELY_MALICIOUS.value && maxConfidence >= 0.50f
        
        if (shouldShowOverlay) {
            // IMMEDIATE OVERLAY DISPLAY
            CoroutineScope(Dispatchers.Main.immediate).launch {
                overlayManager.showThreatAlert(result)
            }

            val bigText = """
            SENDER: $sender
            TIME: ${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(result.timestamp)}
            APP: ${result.context.sourceApp ?: "Unknown"}
            
            MESSAGE:
            "$messageContent"

            THREAT TYPE: $threatType
            CERTAINTY: ${confidence.toInt()}%
            
            EVALUATION:
            ${result.agentResults.firstOrNull { it.agentName == "GuardianCoach" }?.reason ?: "Real-time analysis detected suspicious manipulation patterns."}
            
            ACTIONS TO DO:
            - $guidance
            - Avoid sharing personal or financial info.
            - Block this sender if they are unknown.
        """.trimIndent()

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle(title)
                .setContentText("Suspicious activity in $sender")
                .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(msgHash, notification)
        }
        
        // Periodic cleanup
        if (recentAlerts.size > 200) {
            recentAlerts.entries.removeIf { (now - it.value) > (ALERT_COOLDOWN_MS * 10) }
        }
    }
}
