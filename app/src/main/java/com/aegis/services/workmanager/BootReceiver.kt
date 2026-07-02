package com.aegis.services.workmanager

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val bootWork = OneTimeWorkRequestBuilder<BootInitWorker>()
                .addTag("boot_init")
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork("boot_init", ExistingWorkPolicy.REPLACE, bootWork)
            ScanWorker.schedule(context)
        }
    }
}
