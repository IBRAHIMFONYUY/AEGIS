package com.aegis.services.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.aegis.core.AnalysisContext
import com.aegis.core.SourceType
import com.aegis.agents.GuardianCore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class AegisAccessibilityService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    @Inject
    lateinit var guardianCore: GuardianCore
    
    @Inject
    lateinit var securityNotificationManager: com.aegis.services.notification.AegisSecurityNotificationManager

    @Inject
    lateinit var threatOverlayManager: com.aegis.services.overlay.ThreatOverlayManager

    override fun onServiceConnected() {
        super.onServiceConnected()
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_VIEW_CLICKED or
                    AccessibilityEvent.TYPE_VIEW_FOCUSED or
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            notificationTimeout = 500
        }
        serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString() ?: ""
        
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                // Keep only critical window state monitoring (like permission dialogs)
                if (packageName == "com.google.android.permissioncontroller" || 
                    packageName == "com.android.permissioncontroller") {
                    val root = rootInActiveWindow
                    if (root != null) {
                        handlePermissionDialog(root, packageName)
                        root.recycle()
                    }
                }
            }
        }
    }

    private fun handlePermissionDialog(root: AccessibilityNodeInfo, packageName: String) {
        val dialogText = extractAllText(root)
        scope.launch {
            val context = AnalysisContext(
                text = "Permission Request: $dialogText",
                sourceApp = packageName,
                sourceType = SourceType.SCREEN,
                metadata = mapOf(
                    "source" to "accessibility_service",
                    "is_permission_dialog" to "true",
                    "force_local_ai" to "true"
                )
            )
            guardianCore.analyze(context)
        }
    }

    private fun extractAllText(node: AccessibilityNodeInfo): String {
        val sb = StringBuilder()
        collectTextOptimized(node, sb)
        return sb.toString()
    }

    private fun collectTextOptimized(node: AccessibilityNodeInfo, sb: StringBuilder) {
        if (sb.length > 2000) return
        
        node.text?.toString()?.let { 
            if (it.isNotBlank()) {
                sb.append(it).append(" ")
            }
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            collectTextOptimized(child, sb)
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
