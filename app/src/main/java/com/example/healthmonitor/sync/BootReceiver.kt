package com.example.healthmonitor.sync

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.healthmonitor.advice.SedentaryReminder

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                SedentaryReminder.rescheduleIfEnabled(context)
                if (SyncManager.isEnabled(context)) {
                    SyncScheduler.reschedule(context)
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
    }
}
