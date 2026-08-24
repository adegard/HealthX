package com.example.healthmonitor.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.healthmonitor.HealthApp
import com.example.healthmonitor.R
import com.example.healthmonitor.data.TargetsCalculator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object SyncManager {

    const val ACTION_DAILY_SYNC = "com.example.healthmonitor.DAILY_SYNC"

    private const val PREFS = "home_assistant"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_URL = "url"
    private const val KEY_TOKEN = "token"
    private const val KEY_LAST_SYNC_AT = "last_sync_at"
    private const val KEY_LAST_ERROR = "last_error"
    private const val KEY_LEGACY_CLEANED = "legacy_per_day_cleaned"

    const val ENTITY_ID = "sensor.healthx_steps"
    private const val LEGACY_PREFIX = "sensor.healthx_steps_"
    private const val HISTORY_DAYS = 30

    fun isEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ENABLED, false)

    fun url(context: Context): String =
        prefs(context).getString(KEY_URL, "")?.trim()?.trimEnd('/') ?: ""

    fun token(context: Context): String =
        prefs(context).getString(KEY_TOKEN, "")?.trim() ?: ""

    fun saveSettings(context: Context, enabled: Boolean, url: String, token: String) {
        prefs(context).edit()
            .putBoolean(KEY_ENABLED, enabled)
            .putString(KEY_URL, url.trim().trimEnd('/'))
            .putString(KEY_TOKEN, token.trim())
            .apply()
        if (enabled) SyncScheduler.reschedule(context) else SyncScheduler.cancel(context)
    }

    fun statusText(context: Context): String {
        val app = context.applicationContext as HealthApp
        val today = todayKey()
        val pending = app.statsStore.getUnsyncedDates(60).count { it != today }
        val prefs = prefs(context)
        val lastAt = prefs.getLong(KEY_LAST_SYNC_AT, 0L)
        val lastError = prefs.getString(KEY_LAST_ERROR, null)

        val parts = mutableListOf(context.getString(R.string.ha_pending, pending))
        if (lastAt > 0L) {
            val formatted = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
                .format(Date(lastAt))
            parts.add(context.getString(R.string.ha_last_sync, formatted))
        }
        lastError?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
        return parts.joinToString("\n")
    }

    fun syncNow(context: Context, force: Boolean = false): Boolean {
        val appContext = context.applicationContext
        if (!isEnabled(appContext)) return false
        val baseUrl = url(appContext)
        val authToken = token(appContext)
        if (baseUrl.isBlank() || authToken.isBlank()) return false

        if (!isOnWifi(appContext)) {
            setLastError(appContext, appContext.getString(R.string.ha_waiting_wifi))
            SyncScheduler.scheduleRetry(appContext)
            return false
        }

        val app = appContext as HealthApp
        val profile = app.profileStore.load()

        var allOk = true
        try {
            val rows = app.statsStore.getHistory(HISTORY_DAYS).reversed()
            if (rows.isEmpty()) return true

            val latest = rows.last()
            val latestSteps = latest.steps.toInt()
            val history = linkedMapOf<String, Any>()
            rows.forEach { row -> history[row.date] = row.steps }

            val attributes = linkedMapOf<String, Any?>(
                "friendly_name" to "HealthX steps",
                "icon" to "mdi:foot-print",
                "unit_of_measurement" to "steps",
                "date" to latest.date,
                "distance_km" to TargetsCalculator.distanceKm(latestSteps, profile),
                "calories" to TargetsCalculator.calories(latestSteps, profile),
                "history" to history
            )
            HomeAssistantClient.sendState(
                baseUrl,
                authToken,
                ENTITY_ID,
                latestSteps.toString(),
                attributes
            )

            val today = todayKey()
            app.statsStore.getUnsyncedDates(60).forEach { date ->
                if (date != today) app.statsStore.markSynced(date)
            }

            cleanupLegacySensors(app, baseUrl, authToken)
        } catch (e: Exception) {
            setLastError(appContext, e.message ?: e.javaClass.simpleName)
            allOk = false
        }

        if (allOk) {
            prefs(appContext).edit()
                .putLong(KEY_LAST_SYNC_AT, System.currentTimeMillis())
                .putString(KEY_LAST_ERROR, null)
                .apply()
        } else {
            SyncScheduler.scheduleRetry(appContext)
        }
        return allOk
    }

    private fun cleanupLegacySensors(app: HealthApp, baseUrl: String, authToken: String) {
        val prefs = prefs(app)
        if (prefs.getBoolean(KEY_LEGACY_CLEANED, false)) return
        try {
            app.statsStore.getHistory(730).forEach { row ->
                val legacyId = LEGACY_PREFIX + row.date.replace("-", "_")
                try {
                    HomeAssistantClient.deleteState(baseUrl, authToken, legacyId)
                } catch (e: Exception) {
                    if (e is java.io.IOException && e.message?.contains("404") == true) {
                        // entity never existed or already gone
                    } else {
                        throw e
                    }
                }
            }
            prefs.edit().putBoolean(KEY_LEGACY_CLEANED, true).apply()
        } catch (e: Exception) {
            // retry on next successful sync
        }
    }

    private fun todayKey(): String = java.time.LocalDate.now().toString()

    private fun setLastError(context: Context, message: String) {
        prefs(context).edit().putString(KEY_LAST_ERROR, message).apply()
    }

    private fun isOnWifi(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
