package com.example.healthmonitor

import android.app.Application
import com.example.healthmonitor.data.HealthDatabase
import com.example.healthmonitor.data.ProfileStore
import com.example.healthmonitor.data.StatsStore
import com.example.healthmonitor.data.UserProfile
import com.example.healthmonitor.sensor.StepTracker
import java.time.LocalDate

class HealthApp : Application() {

    val healthDatabase: HealthDatabase by lazy { HealthDatabase(this) }
    val profileStore: ProfileStore by lazy { ProfileStore(healthDatabase) }
    val statsStore: StatsStore by lazy { StatsStore(healthDatabase) }
    val stepTracker: StepTracker by lazy { StepTracker(this, statsStore) }

    override fun onCreate() {
        super.onCreate()
        migrateLegacyPrefs()
    }

    private fun migrateLegacyPrefs() {
        val statsPrefs = getSharedPreferences("stats", MODE_PRIVATE)
        if (!statsPrefs.getBoolean("legacy_migrated", false)) {
            statsPrefs.all.forEach { (key, value) ->
                if (key.startsWith("steps_") && value is Long) {
                    val date = key.removePrefix("steps_")
                    if (healthDatabase.getSteps(date) == 0L) {
                        healthDatabase.upsertSteps(date, value)
                    }
                }
            }
            statsPrefs.getStringSet("achievements", emptySet())?.forEach {
                healthDatabase.earnAchievement(it)
            }
            val lastHr = statsPrefs.getInt("last_hr", 0)
            if (lastHr > 0) {
                healthDatabase.recordHeartRate(LocalDate.now().toString(), lastHr)
            }
            statsPrefs.edit().putBoolean("legacy_migrated", true).apply()
        }

        val profilePrefs = getSharedPreferences("profile", MODE_PRIVATE)
        if (profilePrefs.contains("age")) {
            healthDatabase.saveProfile(
                UserProfile(
                    name = profilePrefs.getString("name", "").orEmpty(),
                    age = profilePrefs.getInt("age", 30),
                    sex = profilePrefs.getString("sex", "male") ?: "male",
                    heightCm = profilePrefs.getFloat("height_cm", 170f).toDouble(),
                    weightKg = profilePrefs.getFloat("weight_kg", 70f).toDouble(),
                    activityLevel = profilePrefs.getInt("activity_level", 1),
                    customGoal = profilePrefs.getInt("custom_goal", 0)
                )
            )
            profilePrefs.edit().clear().apply()
        }
    }
}
