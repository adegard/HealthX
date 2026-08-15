package com.example.healthmonitor.data

import java.io.File
import java.time.LocalDate

class StatsStore(private val db: HealthDatabase) {

    fun todayKey(): String = LocalDate.now().toString()

    fun getStepsFor(dateKey: String): Long = db.getSteps(dateKey)

    fun saveSteps(dateKey: String, steps: Long) = db.upsertSteps(dateKey, steps)

    fun getHistory(limit: Int): List<DailyStat> = db.getHistory(limit)

    fun lastHeartRate(): Int = db.getLastHeartRate()

    fun saveHeartRate(hr: Int) = db.recordHeartRate(todayKey(), hr)

    fun addHeartRateSession() {}

    fun heartRateSessions(): Int = db.getHeartRateSessions(todayKey())

    fun getEarnedAchievements(): Set<String> = db.getAchievements()

    fun earnAchievement(id: String) = db.earnAchievement(id)

    fun getDaysActive(): Set<String> = db.getDaysActive()

    fun markActive(dateKey: String) {}

    fun backupTo(file: File) = db.backupTo(file)

    fun restoreFrom(file: File) = db.restoreFrom(file)
}
