package com.mistymessenger.core.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mistymessenger.core.db.dao.StatusDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class StatusCleanupWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val statusDao: StatusDao
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        statusDao.deleteExpired()
        return Result.success()
    }
}
