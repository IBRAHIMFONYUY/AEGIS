package com.aegis.services.foreground

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.Build
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationCompat
import com.aegis.AegisApplication
import com.aegis.agents.GuardianCore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class AegisForegroundService : Service() {

    companion object {
        const val CHANNEL_ID = "aegis_protection"
        const val NOTIFICATION_ID = 1001
        const val ACTION_STOP = "com.aegis.STOP_FOREGROUND"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    @Inject
    lateinit var guardianCore: GuardianCore

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        try {
            val notification = createNotification()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            startContinuousScanning()
        } catch (e: Exception) {
            e.printStackTrace()
            stopSelf()
            return START_NOT_STICKY
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "AEGIS Protection",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "AEGIS is running in the background to protect you"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AEGIS Active")
            .setContentText("Protecting you from scams and threats")
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setPriority(NotificationManager.IMPORTANCE_LOW)
            .setOngoing(true)
            .build()
    }

    private fun startContinuousScanning() {
        scope.launch {
            while (isActive) {
                performBackgroundScan()
                delay(10_000)
            }
        }
    }

    private suspend fun performBackgroundScan() {
        // Continuous scan could check clipboard or other light signals
        // For now, just a heartbeat analysis to ensure engine is alive
        val context = com.aegis.core.AnalysisContext(
            text = "Active background protection heartbeat",
            sourceType = com.aegis.core.SourceType.UNKNOWN,
            metadata = mapOf("heartbeat" to "true")
        )
        guardianCore.analyze(context)
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
