package com.example.healthmonitor.sync

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class DailySyncReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == SyncManager.ACTION_DAILY_SYNC && SyncManager.isEnabled(context)) {
            val result = goAsync()
            Thread {
                try {
                    SyncManager.syncNow(context)
                } finally {
                    result.finish()
                }
            }.start()
        }
    }
}
