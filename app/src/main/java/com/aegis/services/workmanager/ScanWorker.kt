package com.aegis.services.workmanager

import android.content.Context
import androidx.work.*
import com.aegis.agents.GuardianCore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class ScanWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.Default) {
        try {
            performBackgroundScan()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private suspend fun performBackgroundScan() {
        val packageManager = applicationContext.packageManager
        val installedApps = packageManager.getInstalledApplications(android.content.pm.PackageManager.GET_META_DATA)
        
        val guardianCore = (applicationContext as? com.aegis.AegisApplication)?.guardianCore ?: return

        installedApps.forEach { appInfo ->
            val context = com.aegis.core.AnalysisContext(
                text = "Scanning app: ${appInfo.loadLabel(packageManager)}",
                sourceApp = appInfo.packageName,
                sourceType = com.aegis.core.SourceType.FILE,
                appRiskScore = if (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM == 0) 0.5f else 0.1f
            )
            guardianCore.analyze(context)
        }
    }

    companion object {
        const val WORK_NAME = "aegis_periodic_scan"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

            val request = PeriodicWorkRequestBuilder<ScanWorker>(
                6, TimeUnit.HOURS,
                15, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    1, TimeUnit.MINUTES
                )
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
                )
        }
    }
}
