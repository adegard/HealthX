package com.example.healthmonitor.data

import java.time.LocalDate

data class Achievement(
    val id: String,
    val title: String,
    val description: String
)

object Achievements {

    fun all(): List<Achievement> = listOf(
        Achievement("first_steps", "First Steps", "Take your first 100 steps in a day"),
        Achievement("warm_up", "Warming Up", "Take 1,000 steps in a day"),
        Achievement("half_way", "Half Day", "Take 5,000 steps in a day"),
        Achievement("ten_k", "10K Steps", "Take 10,000 steps in a day"),
        Achievement("twenty_k", "20K Steps", "Take 20,000 steps in a day"),
        Achievement("distance_5k", "5 km Traveller", "Walk 5 km in a day"),
        Achievement("distance_10k", "10 km Explorer", "Walk 10 km in a day"),
        Achievement("calories_500", "Calorie Burner", "Burn 500 kcal in a day"),
        Achievement("bmi_normal", "Healthy Range", "Have a BMI between 18.5 and 25"),
        Achievement("streak_3", "3-Day Streak", "Be active 3 days in a row"),
        Achievement("streak_7", "7-Day Streak", "Be active 7 days in a row")
    )

    fun currentStreak(daysActive: Set<String>): Int {
        var streak = 0
        var date = LocalDate.now()
        while (daysActive.contains(date.toString())) {
            streak++
            date = date.minusDays(1)
        }
        return streak
    }

    fun checkAndEarn(
        statsStore: StatsStore,
        todaySteps: Long,
        distanceKm: Double,
        calories: Double,
        bmi: Double
    ) {
        val earned = statsStore.getEarnedAchievements()
        val streak = currentStreak(statsStore.getDaysActive())

        fun earn(id: String) {
            if (id !in earned) statsStore.earnAchievement(id)
        }

        if (todaySteps >= 100) earn("first_steps")
        if (todaySteps >= 1000) earn("warm_up")
        if (todaySteps >= 5000) earn("half_way")
        if (todaySteps >= 10000) earn("ten_k")
        if (todaySteps >= 20000) earn("twenty_k")
        if (distanceKm >= 5.0) earn("distance_5k")
        if (distanceKm >= 10.0) earn("distance_10k")
        if (calories >= 500.0) earn("calories_500")
        if (bmi in 18.5..25.0) earn("bmi_normal")
        if (streak >= 3) earn("streak_3")
        if (streak >= 7) earn("streak_7")
    }
}
