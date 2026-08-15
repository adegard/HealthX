package com.example.healthmonitor.data

class ProfileStore(private val db: HealthDatabase) {

    fun load(): UserProfile = db.getProfile()

    fun save(profile: UserProfile) = db.saveProfile(profile)
}
