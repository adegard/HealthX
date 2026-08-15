package com.example.healthmonitor.data

import kotlin.math.roundToInt

object TargetsCalculator {

    fun maxHeartRate(age: Int): Int = (208 - 0.7 * age).roundToInt()

    fun autoDailySteps(profile: UserProfile): Int {
        val base = when {
            profile.age < 30 -> 10000
            profile.age < 45 -> 9000
            profile.age < 60 -> 8000
            profile.age < 70 -> 7000
            else -> 6000
        }
        val factor = when (profile.activityLevel) {
            0 -> 0.85
            1 -> 1.0
            2 -> 1.15
            else -> 1.3
        }
        return (base * factor).roundToInt()
    }

    fun dailyStepsGoal(profile: UserProfile): Int =
        if (profile.customGoal > 0) profile.customGoal else autoDailySteps(profile)

    fun strideMeters(profile: UserProfile): Double {
        val factor = if (profile.sex == "female") 0.413 else 0.415
        return profile.heightCm / 100.0 * factor
    }

    fun distanceKm(steps: Int, profile: UserProfile): Double =
        steps * strideMeters(profile) / 1000.0

    fun calories(steps: Int, profile: UserProfile): Double =
        steps * profile.weightKg * 0.0005

    fun activeMinutes(steps: Int): Int = steps / 100

    fun bmi(profile: UserProfile): Double {
        val meters = profile.heightCm / 100.0
        return profile.weightKg / (meters * meters)
    }

    fun heartRateZones(age: Int): List<Pair<String, IntRange>> {
        val max = maxHeartRate(age)
        return listOf(
            "Rest" to IntRange((max * 0.50).toInt(), (max * 0.59).toInt()),
            "Moderate" to IntRange((max * 0.60).toInt(), (max * 0.69).toInt()),
            "Vigorous" to IntRange((max * 0.70).toInt(), (max * 0.84).toInt()),
            "Maximum" to IntRange((max * 0.85).toInt(), max)
        )
    }
}
