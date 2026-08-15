package com.example.healthmonitor.advice

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

object SedentaryReminder {

    private const val PREFS = "advice"
    private const val KEY_ENABLED = "sedentary_enabled"
    private const val KEY_INTERVAL = "sedentary_interval_min"
    const val ACTION_REMIND = "com.example.healthmonitor.SEDENTARY_REMIND"
    private const val DEFAULT_INTERVAL_MIN = 60

    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_ENABLED, false)

    fun intervalMin(context: Context): Int =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt(KEY_INTERVAL, DEFAULT_INTERVAL_MIN)

    fun set(context: Context, enabled: Boolean, intervalMin: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ENABLED, enabled)
            .putInt(KEY_INTERVAL, intervalMin)
            .apply()
        if (enabled) schedule(context, intervalMin) else cancel(context)
    }

    fun rescheduleIfEnabled(context: Context) {
        if (isEnabled(context)) schedule(context, intervalMin(context))
    }

    private fun schedule(context: Context, intervalMin: Int) {
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intervalMs = intervalMin * 60_000L
        alarm.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + intervalMs,
            intervalMs,
            pendingIntent(context)
        )
    }

    fun cancel(context: Context) {
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarm.cancel(pendingIntent(context))
    }

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, SedentaryReminderReceiver::class.java)
            .setAction(ACTION_REMIND)
        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }
}
