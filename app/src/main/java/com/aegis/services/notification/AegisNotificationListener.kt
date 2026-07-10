package com.aegis.services.notification

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.os.Bundle
import com.aegis.AegisApplication
import com.aegis.core.AnalysisContext
import com.aegis.core.SourceType
import com.aegis.agents.GuardianCore
import com.aegis.ai.AIOperationManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class AegisNotificationListener : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    @Inject
    lateinit var guardianCore: GuardianCore
    
    @Inject
    lateinit var aiOperationManager: AIOperationManager

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        
        // --- CRITICAL: Ignore our own notifications to prevent AI loops ---
        if (packageName == applicationContext.packageName) return

        val extras = sbn.notification.extras

        val title = extras.getString(android.app.Notification.EXTRA_TITLE, "")
        val text = extras.getCharSequence(android.app.Notification.EXTRA_TEXT)?.toString() ?: ""
        val subText = extras.getCharSequence(android.app.Notification.EXTRA_SUB_TEXT)?.toString() ?: ""
        val infoText = extras.getCharSequence(android.app.Notification.EXTRA_INFO_TEXT)?.toString() ?: ""
        val summaryText = extras.getCharSequence(android.app.Notification.EXTRA_SUMMARY_TEXT)?.toString() ?: ""

        val combinedText = listOfNotNull(title, text, subText, infoText, summaryText)
            .filter { it.isNotEmpty() }
            .joinToString(" ")

        if (combinedText.length > 5) {
            val sourceType = when {
                packageName.contains("com.whatsapp") -> SourceType.WHATSAPP
                packageName.contains("org.telegram") -> SourceType.TELEGRAM
                packageName.contains("com.facebook.orca") -> SourceType.MESSENGER
                packageName.contains("com.google.android.apps.messaging") ||
                packageName.contains("com.android.mms") ||
                packageName.contains("com.samsung.android.messaging") -> SourceType.SMS
                packageName.contains("com.google.android.gm") ||
                packageName.contains("com.android.email") -> SourceType.EMAIL
                packageName.contains("com.android.chrome") ||
                packageName.contains("org.mozilla.firefox") -> SourceType.BROWSER
                else -> SourceType.NOTIFICATION
            }

            val chatId = "${packageName}_$title"
            
            scope.launch {
                // For chat apps, perform deep conversation analysis
                if (sourceType in listOf(SourceType.WHATSAPP, SourceType.TELEGRAM, SourceType.MESSENGER)) {
                    performDeepConversationAnalysis(
                        chatId = chatId,
                        sender = title,
                        currentMessage = combinedText,
                        packageName = packageName,
                        sourceType = sourceType
                    )
                } else {
                    // Standard analysis for other notifications
                    val context = AnalysisContext(
                        text = combinedText,
                        sourceApp = packageName,
                        sourceType = sourceType,
                        metadata = mapOf(
                            "notification_title" to title,
                            "notification_tag" to (sbn.tag ?: ""),
                            "notification_id" to sbn.id.toString(),
                            "sender" to title,
                            "chat_id" to chatId,
                            "source_type" to "NOTIFICATION"
                        )
                    )
                    guardianCore.analyze(context)
                }
            }
        }
    }
    
    private suspend fun performDeepConversationAnalysis(
        chatId: String,
        sender: String,
        currentMessage: String,
        packageName: String,
        sourceType: SourceType
    ) {
        try {
            // Get conversation history from memory
            val conversationHistory = guardianCore.memoryRepository?.getConversationHistory(chatId) ?: emptyList<String>()
            
            // Perform analysis via optimized manager
            // We use standard analyzeThreatOptimized first to see if local tier can handle it
            val context = AnalysisContext(
                text = currentMessage,
                sourceApp = packageName,
                sourceType = sourceType,
                conversationHistory = conversationHistory,
                metadata = mapOf(
                    "notification_title" to sender,
                    "sender" to sender,
                    "chat_id" to chatId,
                    "source_type" to "NOTIFICATION"
                )
            )

            val result = guardianCore.analyze(context)
            
            // For "Real & Legit" threats found by local/cache tiers, show alert immediately
            if (result.overallThreatLevel.value >= com.aegis.core.ThreatLevel.MALICIOUS.value) {
                showThreatOverlay(result, packageName)
            } else if (result.overallThreatLevel.value == com.aegis.core.ThreatLevel.SUSPICIOUS.value) {
                // If only SUSPICIOUS, we don't call Gemini yet.
                // We could show a subtle "Check with AEGIS" option if we had that UI, 
                // but for now, we follow the "only when requested" rule to save quota.
            }
        } catch (e: Exception) {
            // ... fallback logic
            // Fallback to basic analysis on error
            val context = AnalysisContext(
                text = currentMessage,
                sourceApp = packageName,
                sourceType = sourceType,
                metadata = mapOf(
                    "notification_title" to sender,
                    "sender" to sender,
                    "chat_id" to chatId,
                    "analysis_error" to e.localizedMessage
                )
            )
            guardianCore.analyze(context)
        }
    }
    
    private fun showThreatOverlay(result: com.aegis.core.AnalysisResult, packageName: String) {
        // This would typically trigger the overlay service
        // For now, we'll rely on the existing overlay system
        try {
            val overlayManager = com.aegis.services.overlay.ThreatOverlayManager(applicationContext)
            overlayManager.showThreatAlert(result)
        } catch (e: Exception) {
            // Log error but don't crash
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {}

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
