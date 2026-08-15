package com.example.healthmonitor.data

data class DailyStat(
    val date: String,
    val steps: Long,
    val avgHr: Int,
    val hrSessions: Int
)
