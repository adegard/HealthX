package com.example.healthmonitor.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.healthmonitor.HealthApp
import com.example.healthmonitor.R
import com.example.healthmonitor.data.TargetsCalculator
import org.json.JSONObject
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
    private const val ENTITY_PREFIX = "sensor.healthx_steps_"

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
        val pending = app.statsStore.countUnsynced()
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

    fun syncNow(context: Context): Boolean {
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

        for (date in app.statsStore.getUnsyncedDates(60)) {
            val steps = app.statsStore.getStepsFor(date)
            val entityId = ENTITY_PREFIX + date.replace("-", "_")
            val attributes = linkedMapOf<String, Any?>(
                "date" to date,
                "distance_km" to TargetsCalculator.distanceKm(steps.toInt(), profile),
                "calories" to TargetsCalculator.calories(steps.toInt(), profile),
                "friendly_name" to "HealthX steps $date"
            )
            allOk = try {
                HomeAssistantClient.sendState(
                    baseUrl,
                    authToken,
                    entityId,
                    steps.toString(),
                    attributes
                )
                app.statsStore.markSynced(date)
                true
            } catch (e: Exception) {
                setLastError(appContext, e.message ?: e.javaClass.simpleName)
                false
            }
            if (!allOk) break
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
