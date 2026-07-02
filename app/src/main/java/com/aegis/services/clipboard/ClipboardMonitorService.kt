package com.aegis.services.clipboard

import android.app.Service
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.IBinder
import com.aegis.agents.GuardianCore
import com.aegis.core.AnalysisContext
import com.aegis.core.SourceType
import kotlinx.coroutines.*

class ClipboardMonitorService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var guardianCore: GuardianCore? = null
    private var lastClipboardContent: String? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startMonitoring()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startMonitoring() {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.addPrimaryClipChangedListener(clipListener)
        checkExistingClip(clipboard)
    }

    private val clipListener = ClipboardManager.OnPrimaryClipChangedListener {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clip = clipboard.primaryClip ?: return@OnPrimaryClipChangedListener
        val text = clip.getItemAt(0)?.text?.toString() ?: return@OnPrimaryClipChangedListener

        if (text != lastClipboardContent && text.length > 5) {
            lastClipboardContent = text
            analyzeClipboardContent(text)
        }
    }

    private fun checkExistingClip(clipboard: ClipboardManager) {
        val clip = clipboard.primaryClip ?: return
        val text = clip.getItemAt(0)?.text?.toString() ?: return
        if (text.length > 5) {
            lastClipboardContent = text
        }
    }

    private fun analyzeClipboardContent(text: String) {
        scope.launch {
            val sensitivePatterns = listOf(
                Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"),
                Regex("""\+?\d{1,3}[-.\s]?\(?\d{1,4}\)?[-.\s]?\d{1,4}[-.\s]?\d{1,9}"""),
                Regex("""\b(?:\d[ -]*?){13,16}\b""")
            )
            val matches = sensitivePatterns.any { it.containsMatchIn(text) }
            if (matches) {
                val context = AnalysisContext(
                    text = text,
                    sourceType = SourceType.CLIPBOARD,
                    metadata = mapOf("source" to "clipboard_monitor")
                )
                guardianCore?.analyze(context)
            }
        }
    }

    fun setGuardianCore(core: GuardianCore) {
        guardianCore = core
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
