package com.aegis.services.workmanager

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import timber.log.Timber
import com.aegis.services.foreground.AegisForegroundService

class BootInitWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val intent = android.content.Intent(
            applicationContext,
            AegisForegroundService::class.java
        )
        return try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                applicationContext.startForegroundService(intent)
            } else {
                applicationContext.startService(intent)
            }
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "BootInitWorker failed to start service")
            Result.retry()
        }
    }
}
