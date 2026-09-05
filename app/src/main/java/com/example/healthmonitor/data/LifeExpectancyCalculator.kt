package com.example.healthmonitor.data

data class LifeExpectancyResult(
    val lifespanYears: Double,
    val remainingSeconds: Long,
    val avgDailySteps: Long,
    val bmi: Double,
    val stepsAdjustment: Double,
    val bmiAdjustment: Double,
    val hasStepsData: Boolean
)

object LifeExpectancyCalculator {

    const val SECONDS_PER_YEAR = 31_557_600L

    private const val BASELINE_MALE = 74.0
    private const val BASELINE_FEMALE = 80.0

    fun estimate(user: UserProfile, dailyStats: List<DailyStat>): LifeExpectancyResult {
        val avgSteps = if (dailyStats.isEmpty()) 0.0
        else dailyStats.map { it.steps.toDouble() }.average()
        val hasData = avgSteps > 0
        val stepsAdjust = if (hasData) stepsAdjustment(avgSteps) else 0.0
        val bmi = TargetsCalculator.bmi(user)
        val bmiAdjust = bmiAdjustment(bmi)
        val base = if (user.sex == "female") BASELINE_FEMALE else BASELINE_MALE
        val lifespan = (base + bmiAdjust + stepsAdjust).coerceIn(55.0, 100.0)
        val remainingSeconds = ((lifespan - user.age) * SECONDS_PER_YEAR)
            .toLong()
            .coerceAtLeast(0L)
        return LifeExpectancyResult(
            lifespanYears = lifespan,
            remainingSeconds = remainingSeconds,
            avgDailySteps = avgSteps.toLong(),
            bmi = bmi,
            stepsAdjustment = stepsAdjust,
            bmiAdjustment = bmiAdjust,
            hasStepsData = hasData
        )
    }

    private fun stepsAdjustment(avg: Double): Double = when {
        avg < 2000 -> -2.5
        avg < 4000 -> -1.5
        avg < 7000 -> -1.0
        avg < 10000 -> 0.0
        avg < 13000 -> 1.5
        else -> 2.5
    }

    private fun bmiAdjustment(bmi: Double): Double = when {
        bmi < 18.5 -> -3.0
        bmi < 25.0 -> 0.0
        bmi < 30.0 -> -0.5
        bmi < 35.0 -> -2.0
        else -> -4.0
    }
}