package com.example.healthmonitor.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.io.File

class HealthDatabase(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    companion object {
        const val DB_NAME = "health_monitor.db"
        private const val DB_VERSION = 2
    }

    private val appContext = context.applicationContext

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE daily_stats (" +
                "date TEXT PRIMARY KEY, " +
                "steps INTEGER NOT NULL DEFAULT 0, " +
                "avg_hr INTEGER NOT NULL DEFAULT 0, " +
                "hr_sessions INTEGER NOT NULL DEFAULT 0)"
        )
        db.execSQL(
            "CREATE TABLE achievements (" +
                "id TEXT PRIMARY KEY, " +
                "earned_at INTEGER NOT NULL)"
        )
        db.execSQL(
            "CREATE TABLE profile (" +
                "id INTEGER PRIMARY KEY CHECK(id = 1), " +
                "name TEXT NOT NULL DEFAULT '', " +
                "age INTEGER NOT NULL DEFAULT 30, " +
                "sex TEXT NOT NULL DEFAULT 'male', " +
                "height_cm REAL NOT NULL DEFAULT 170, " +
                "weight_kg REAL NOT NULL DEFAULT 70, " +
                "activity_level INTEGER NOT NULL DEFAULT 1, " +
                "custom_goal INTEGER NOT NULL DEFAULT 0)"
        )
        db.execSQL(
            "CREATE TABLE sync_log (" +
                "date TEXT PRIMARY KEY, " +
                "synced_at INTEGER NOT NULL)"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS sync_log (" +
                    "date TEXT PRIMARY KEY, " +
                    "synced_at INTEGER NOT NULL)"
            )
        }
    }

    @Synchronized
    fun getSteps(dateKey: String): Long {
        readableDatabase.query(
            "daily_stats", arrayOf("steps"), "date = ?", arrayOf(dateKey), null, null, null
        ).use { c ->
            return if (c.moveToFirst()) c.getLong(0) else 0L
        }
    }

    @Synchronized
    fun upsertSteps(dateKey: String, steps: Long) {
        ensureRow(dateKey)
        writableDatabase.execSQL(
            "UPDATE daily_stats SET steps = ? WHERE date = ?",
            arrayOf(steps, dateKey)
        )
    }

    @Synchronized
    fun recordHeartRate(dateKey: String, bpm: Int) {
        ensureRow(dateKey)
        writableDatabase.execSQL(
            "UPDATE daily_stats SET avg_hr = ?, hr_sessions = hr_sessions + 1 WHERE date = ?",
            arrayOf(bpm, dateKey)
        )
    }

    @Synchronized
    fun getHeartRateSessions(dateKey: String): Int {
        readableDatabase.query(
            "daily_stats", arrayOf("hr_sessions"), "date = ?", arrayOf(dateKey), null, null, null
        ).use { c ->
            return if (c.moveToFirst()) c.getInt(0) else 0
        }
    }

    @Synchronized
    fun getLastHeartRate(): Int {
        readableDatabase.query(
            "daily_stats", arrayOf("avg_hr"), "avg_hr > 0", null, null, null, "date DESC", "1"
        ).use { c ->
            return if (c.moveToFirst()) c.getInt(0) else 0
        }
    }

    @Synchronized
    fun getHistory(limit: Int): List<DailyStat> {
        val out = mutableListOf<DailyStat>()
        readableDatabase.query(
            "daily_stats", null, null, null, null, null, "date DESC", limit.toString()
        ).use { c ->
            while (c.moveToNext()) {
                out += DailyStat(
                    date = c.getString(c.getColumnIndexOrThrow("date")),
                    steps = c.getLong(c.getColumnIndexOrThrow("steps")),
                    avgHr = c.getInt(c.getColumnIndexOrThrow("avg_hr")),
                    hrSessions = c.getInt(c.getColumnIndexOrThrow("hr_sessions"))
                )
            }
        }
        return out
    }

    @Synchronized
    fun getDaysActive(): Set<String> {
        val out = mutableSetOf<String>()
        readableDatabase.query(
            "daily_stats", arrayOf("date"), "steps > 0", null, null, null, null
        ).use { c ->
            while (c.moveToNext()) out.add(c.getString(0))
        }
        return out
    }

    @Synchronized
    fun getUnsyncedDates(limit: Int): List<String> {
        val out = mutableListOf<String>()
        readableDatabase.rawQuery(
            "SELECT d.date FROM daily_stats d " +
                "LEFT JOIN sync_log s ON d.date = s.date " +
                "WHERE s.date IS NULL ORDER BY d.date ASC LIMIT ?",
            arrayOf(limit.toString())
        ).use { c ->
            while (c.moveToNext()) out.add(c.getString(0))
        }
        return out
    }

    @Synchronized
    fun countUnsynced(): Int {
        readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM daily_stats d " +
                "LEFT JOIN sync_log s ON d.date = s.date WHERE s.date IS NULL",
            null
        ).use { c ->
            return if (c.moveToFirst()) c.getInt(0) else 0
        }
    }

    @Synchronized
    fun markSynced(dateKey: String) {
        writableDatabase.insertWithOnConflict(
            "sync_log", null,
            ContentValues().apply {
                put("date", dateKey)
                put("synced_at", System.currentTimeMillis())
            },
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    @Synchronized
    fun getAchievements(): Set<String> {
        val out = mutableSetOf<String>()
        readableDatabase.query("achievements", arrayOf("id"), null, null, null, null, null).use { c ->
            while (c.moveToNext()) out.add(c.getString(0))
        }
        return out
    }

    @Synchronized
    fun earnAchievement(id: String) {
        writableDatabase.insertWithOnConflict(
            "achievements", null,
            ContentValues().apply {
                put("id", id)
                put("earned_at", System.currentTimeMillis())
            },
            SQLiteDatabase.CONFLICT_IGNORE
        )
    }

    @Synchronized
    fun getProfile(): UserProfile {
        readableDatabase.query("profile", null, "id = 1", null, null, null, null).use { c ->
            if (c.moveToFirst()) {
                return UserProfile(
                    name = c.getString(c.getColumnIndexOrThrow("name")),
                    age = c.getInt(c.getColumnIndexOrThrow("age")),
                    sex = c.getString(c.getColumnIndexOrThrow("sex")),
                    heightCm = c.getDouble(c.getColumnIndexOrThrow("height_cm")),
                    weightKg = c.getDouble(c.getColumnIndexOrThrow("weight_kg")),
                    activityLevel = c.getInt(c.getColumnIndexOrThrow("activity_level")),
                    customGoal = c.getInt(c.getColumnIndexOrThrow("custom_goal"))
                )
            }
        }
        return UserProfile()
    }

    @Synchronized
    fun saveProfile(profile: UserProfile) {
        writableDatabase.insertWithOnConflict(
            "profile", null,
            ContentValues().apply {
                put("id", 1)
                put("name", profile.name)
                put("age", profile.age)
                put("sex", profile.sex)
                put("height_cm", profile.heightCm)
                put("weight_kg", profile.weightKg)
                put("activity_level", profile.activityLevel)
                put("custom_goal", profile.customGoal)
            },
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    fun backupTo(dest: File) {
        synchronized(this) {
            close()
            try {
                appContext.getDatabasePath(DB_NAME).inputStream().use { input ->
                    dest.outputStream().use { output -> input.copyTo(output) }
                }
            } finally {
                readableDatabase
            }
        }
    }

    fun restoreFrom(src: File) {
        synchronized(this) {
            close()
            val dbFile = appContext.getDatabasePath(DB_NAME)
            src.inputStream().use { input ->
                dbFile.outputStream().use { output -> input.copyTo(output) }
            }
            File(dbFile.path + "-journal").delete()
            File(dbFile.path + "-wal").delete()
            File(dbFile.path + "-shm").delete()
            readableDatabase
        }
    }

    private fun ensureRow(dateKey: String) {
        writableDatabase.execSQL(
            "INSERT OR IGNORE INTO daily_stats(date, steps, avg_hr, hr_sessions) VALUES(?, 0, 0, 0)",
            arrayOf(dateKey)
        )
    }
}
