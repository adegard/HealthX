package com.example.healthmonitor.sync

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

class SyncWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        SyncManager.syncNow(applicationContext)
        return Result.success()
    }
}
