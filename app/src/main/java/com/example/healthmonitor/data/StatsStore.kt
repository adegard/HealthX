package com.example.healthmonitor.data

import android.content.Context
import java.time.LocalDate

class StatsStore(context: Context) {

    private val prefs = context.getSharedPreferences("stats", Context.MODE_PRIVATE)

    fun todayKey(): String = LocalDate.now().toString()

    fun getStepsFor(dateKey: String): Long = prefs.getLong("steps_$dateKey", 0)

    fun saveSteps(dateKey: String, steps: Long) {
        prefs.edit().putLong("steps_$dateKey", steps).apply()
    }

    fun getEarnedAchievements(): Set<String> =
        prefs.getStringSet("achievements", emptySet()) ?: emptySet()

    fun earnAchievement(id: String) {
        val current = getEarnedAchievements().toMutableSet()
        if (current.add(id)) {
            prefs.edit().putStringSet("achievements", current).apply()
        }
    }

    fun getDaysActive(): Set<String> =
        prefs.getStringSet("days_active", emptySet()) ?: emptySet()

    fun markActive(dateKey: String) {
        val current = getDaysActive().toMutableSet()
        if (current.add(dateKey)) {
            prefs.edit().putStringSet("days_active", current).apply()
        }
    }

    fun lastHeartRate(): Int = prefs.getInt("last_hr", 0)

    fun saveHeartRate(hr: Int) {
        prefs.edit().putInt("last_hr", hr).apply()
    }

    fun heartRateSessions(): Int = prefs.getInt("hr_sessions", 0)

    fun addHeartRateSession() {
        prefs.edit().putInt("hr_sessions", prefs.getInt("hr_sessions", 0) + 1).apply()
    }
}
