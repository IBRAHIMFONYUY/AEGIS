package com.aegis.services.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GppBad
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.aegis.core.AnalysisResult
import com.aegis.core.ThreatLevel
import com.aegis.ui.theme.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ThreatOverlayManager(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var currentView: View? = null

    fun showThreatAlert(result: AnalysisResult) {
        if (result.overallThreatLevel == ThreatLevel.SAFE) return

        // Check for overlay permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !android.provider.Settings.canDrawOverlays(context)) {
            Log.w("ThreatOverlayManager", "Cannot show overlay: SYSTEM_ALERT_WINDOW permission not granted")
            return
        }

        CoroutineScope(Dispatchers.Main).launch {
            hideOverlay()

            val composeView = ComposeView(context).apply {
                setContent {
                    AegisTheme(darkTheme = true) {
                        ThreatAlertUI(
                            result = result,
                            onDismiss = { hideOverlay() }
                        )
                    }
                }
            }

            val lifecycleOwner = MyLifecycleOwner()
            lifecycleOwner.performRestore(null)
            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)
            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
            
            composeView.setViewTreeLifecycleOwner(lifecycleOwner)
            composeView.setViewTreeViewModelStoreOwner(lifecycleOwner)
            composeView.setViewTreeSavedStateRegistryOwner(lifecycleOwner)

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) 
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY 
                else WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP
                y = 50 
            }

            try {
                windowManager.addView(composeView, params)
                currentView = composeView
                
                // Critical threats stay longer, suspicious ones fade after 8s
                val duration = if (result.overallThreatLevel.value >= ThreatLevel.MALICIOUS.value) 20000L else 8000L
                delay(duration)
                hideOverlay()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun hideOverlay() {
        currentView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                // ignore
            }
            currentView = null
        }
    }

    private class MyLifecycleOwner : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {
        private val lifecycleRegistry = LifecycleRegistry(this)
        private val savedStateRegistryController = SavedStateRegistryController.create(this)
        private val store = ViewModelStore()

        override val lifecycle: Lifecycle get() = lifecycleRegistry
        override val viewModelStore: ViewModelStore get() = store
        override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

        fun handleLifecycleEvent(event: Lifecycle.Event) {
            lifecycleRegistry.handleLifecycleEvent(event)
        }
        
        fun performRestore(savedState: android.os.Bundle?) {
            savedStateRegistryController.performRestore(savedState)
        }
    }
}

@Composable
fun ThreatAlertUI(
    result: AnalysisResult,
    onDismiss: () -> Unit
) {
    val level = result.overallThreatLevel
    val color = when (level) {
        ThreatLevel.CRITICAL -> DangerRed
        ThreatLevel.MALICIOUS -> DangerRed
        ThreatLevel.LIKELY_MALICIOUS -> WarningOrange
        else -> WarningYellow
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1C1E)),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(color.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (level.value >= ThreatLevel.MALICIOUS.value) Icons.Filled.GppBad else Icons.Filled.Security,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "AEGIS GUARDIAN: ${level.label.uppercase()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = color,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Suspicious activity in ${result.context.sourceApp?.split('.')?.lastOrNull() ?: "Current App"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, null, tint = Color.White.copy(alpha = 0.4f))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val coachExplanation = result.agentResults.find { it.agentName == "GuardianCoach" }?.reason
            val primaryReason = result.agentResults.maxByOrNull { it.threatLevel.value }?.reason 
                ?: "Potential manipulation detected."

            Text(
                text = coachExplanation ?: primaryReason,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                fontWeight = FontWeight.Medium,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) {
                    Text("Ignore", color = Color.White.copy(alpha = 0.6f))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = color),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        if (level.value >= ThreatLevel.MALICIOUS.value) "Secure My Data" else "Understood",
                        fontWeight = FontWeight.Bold,
                        color = if (color == WarningYellow) Color.Black else Color.White
                    )
                }
            }
        }
    }
}
