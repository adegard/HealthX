package com.example.healthmonitor.data

data class UserProfile(
    val name: String = "",
    val age: Int = 30,
    val sex: String = "male",
    val heightCm: Double = 170.0,
    val weightKg: Double = 70.0,
    val activityLevel: Int = 1,
    val customGoal: Int = 0
)
