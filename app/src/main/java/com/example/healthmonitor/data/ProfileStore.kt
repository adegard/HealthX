package com.example.healthmonitor.data

import android.content.Context

data class UserProfile(
    val name: String = "",
    val age: Int = 30,
    val sex: String = "male",
    val heightCm: Double = 170.0,
    val weightKg: Double = 70.0,
    val activityLevel: Int = 1,
    val customGoal: Int = 0
)

class ProfileStore(context: Context) {

    private val prefs = context.getSharedPreferences("profile", Context.MODE_PRIVATE)

    fun load(): UserProfile = UserProfile(
        name = prefs.getString("name", "") ?: "",
        age = prefs.getInt("age", 30),
        sex = prefs.getString("sex", "male") ?: "male",
        heightCm = prefs.getFloat("height_cm", 170f).toDouble(),
        weightKg = prefs.getFloat("weight_kg", 70f).toDouble(),
        activityLevel = prefs.getInt("activity_level", 1),
        customGoal = prefs.getInt("custom_goal", 0)
    )

    fun save(profile: UserProfile) {
        prefs.edit()
            .putString("name", profile.name)
            .putInt("age", profile.age)
            .putString("sex", profile.sex)
            .putFloat("height_cm", profile.heightCm.toFloat())
            .putFloat("weight_kg", profile.weightKg.toFloat())
            .putInt("activity_level", profile.activityLevel)
            .putInt("custom_goal", profile.customGoal)
            .apply()
    }
}
