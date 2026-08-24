package com.example.healthmonitor.sync

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

object SyncScheduler {

    private const val ACTION_SYNC = SyncManager.ACTION_DAILY_SYNC
    private const val REQUEST_DAILY = 1001
    private const val REQUEST_RETRY = 1002

    fun reschedule(context: Context) {
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarm.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            nextMidnightOffset(),
            AlarmManager.INTERVAL_DAY,
            pendingIntent(context, REQUEST_DAILY)
        )
    }

    fun scheduleRetry(context: Context, delayMs: Long = 30 * 60_000L) {
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarm.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + delayMs,
            pendingIntent(context, REQUEST_RETRY)
        )
    }

    fun cancel(context: Context) {
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarm.cancel(pendingIntent(context, REQUEST_DAILY))
        alarm.cancel(pendingIntent(context, REQUEST_RETRY))
    }

    private fun nextMidnightOffset(): Long {
        val calendar = java.util.Calendar.getInstance().apply {
            add(java.util.Calendar.DAY_OF_YEAR, 1)
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 15)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    private fun pendingIntent(context: Context, requestCode: Int): PendingIntent {
        val intent = Intent(context, DailySyncReceiver::class.java).setAction(ACTION_SYNC)
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }
}
