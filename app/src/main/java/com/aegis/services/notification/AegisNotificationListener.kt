package com.aegis.services.notification

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.os.Bundle
import com.aegis.AegisApplication
import com.aegis.core.AnalysisContext
import com.aegis.core.SourceType
import com.aegis.agents.GuardianCore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class AegisNotificationListener : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    @Inject
    lateinit var guardianCore: GuardianCore

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
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

            scope.launch {
                val context = AnalysisContext(
                    text = combinedText,
                    sourceApp = packageName,
                    sourceType = sourceType,
                    metadata = mapOf(
                        "notification_title" to title,
                        "notification_tag" to (sbn.tag ?: ""),
                        "notification_id" to sbn.id.toString(),
                        "sender" to title,
                        "chat_id" to "${packageName}_$title"
                    )
                )
                guardianCore.analyze(context)
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {}

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
