package com.mistymessenger.core.worker

import android.content.Context
import androidx.work.*
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkerScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun scheduleContactSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // Run immediately once, then every 12 hours
        val oneTimeRequest = OneTimeWorkRequestBuilder<ContactSyncWorker>()
            .setConstraints(constraints)
            .build()

        val periodicRequest = PeriodicWorkRequestBuilder<ContactSyncWorker>(12, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        val workManager = WorkManager.getInstance(context)
        workManager.enqueueUniqueWork("contact_sync_once", ExistingWorkPolicy.REPLACE, oneTimeRequest)
        workManager.enqueueUniquePeriodicWork("contact_sync", ExistingPeriodicWorkPolicy.KEEP, periodicRequest)
    }

    fun scheduleStatusCleanup() {
        val request = PeriodicWorkRequestBuilder<StatusCleanupWorker>(1, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.NOT_REQUIRED).build())
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork("status_cleanup", ExistingPeriodicWorkPolicy.KEEP, request)
    }
}
