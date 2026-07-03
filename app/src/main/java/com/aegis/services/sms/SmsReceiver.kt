package com.aegis.services.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.aegis.agents.GuardianCore
import com.aegis.core.AnalysisContext
import com.aegis.core.SourceType
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SmsReceiver : BroadcastReceiver() {

    @Inject
    lateinit var guardianCore: GuardianCore

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            for (sms in messages) {
                val sender = sms.displayOriginatingAddress
                val body = sms.displayMessageBody
                
                scope.launch {
                    val analysisContext = AnalysisContext(
                        text = body,
                        sourceApp = "com.android.sms",
                        sourceType = SourceType.SMS,
                        isUnknownSender = true, // Simplified for now
                        metadata = mapOf("sender" to (sender ?: "Unknown"))
                    )
                    guardianCore.analyze(analysisContext)
                }
            }
        }
    }
}
