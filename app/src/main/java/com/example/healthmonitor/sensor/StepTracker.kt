package com.example.healthmonitor.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.example.healthmonitor.data.StatsStore
import java.time.LocalDate
import kotlin.math.sqrt

/**
 * Counts steps using the phone's own hardware:
 *  - TYPE_STEP_COUNTER when available (requires ACTIVITY_RECOGNITION on Android 10+),
 *  - falls back to an accelerometer peak-detector otherwise.
 * Never relies on a watch or on cloud step data.
 */
class StepTracker(
    context: Context,
    private val statsStore: StatsStore
) : SensorEventListener {

    interface Listener {
        fun onStepsChanged(totalToday: Long) {}
        fun onSensorTypeChanged(usingStepCounter: Boolean) {}
    }

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepCounter = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var accumulatedBeforeSession = 0L
    private var sessionBase = -1L
    private var currentDateKey = ""
    private var usingStepCounter = true

    private var filteredMag = 0.0
    private var accelPeakActive = false
    private var lastStepTime = 0L

    var listener: Listener? = null

    var totalToday: Long = 0L
        private set

    val usesStepCounterSensor: Boolean
        get() = usingStepCounter

    fun start() {
        currentDateKey = LocalDate.now().toString()
        accumulatedBeforeSession = statsStore.getStepsFor(currentDateKey)
        totalToday = accumulatedBeforeSession
        listener?.onStepsChanged(totalToday)

        sensorManager.unregisterListener(this)
        accelPeakActive = false
        filteredMag = 0.0

        if (stepCounter != null) {
            usingStepCounter = true
            sessionBase = -1
            try {
                sensorManager.registerListener(this, stepCounter, SensorManager.SENSOR_DELAY_NORMAL)
            } catch (e: SecurityException) {
                usingStepCounter = false
                startAccelerometer()
            }
        } else {
            usingStepCounter = false
            startAccelerometer()
        }
        listener?.onSensorTypeChanged(usingStepCounter)
    }

    private fun startAccelerometer() {
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    fun flush() {
        if (currentDateKey.isNotEmpty()) {
            statsStore.saveSteps(currentDateKey, totalToday)
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_STEP_COUNTER -> handleStepCounter(event)
            Sensor.TYPE_ACCELEROMETER -> handleAccelerometer(event)
        }
    }

    private fun handleStepCounter(event: SensorEvent) {
        val value = event.values[0].toLong()
        if (sessionBase < 0) {
            sessionBase = value
            return
        }
        val sessionSteps = (value - sessionBase).coerceAtLeast(0)
        val rolledOver = checkDayRollover()
        totalToday = accumulatedBeforeSession + if (rolledOver) 0 else sessionSteps
        listener?.onStepsChanged(totalToday)
    }

    private fun handleAccelerometer(event: SensorEvent) {
        val x = event.values[0].toDouble()
        val y = event.values[1].toDouble()
        val z = event.values[2].toDouble()
        val mag = sqrt(x * x + y * y + z * z)
        if (filteredMag == 0.0) filteredMag = mag
        filteredMag = 0.8 * filteredMag + 0.2 * mag

        val delta = mag - filteredMag
        val now = System.currentTimeMillis()
        if (delta > 1.3 && !accelPeakActive) {
            accelPeakActive = true
            if (now - lastStepTime > 400) {
                checkDayRollover()
                totalToday += 1
                lastStepTime = now
                listener?.onStepsChanged(totalToday)
            }
        } else if (delta < 0.4) {
            accelPeakActive = false
        }
    }

    private fun checkDayRollover(): Boolean {
        val day = LocalDate.now().toString()
        if (day != currentDateKey) {
            statsStore.saveSteps(currentDateKey, totalToday)
            currentDateKey = day
            accumulatedBeforeSession = statsStore.getStepsFor(day)
            sessionBase = -1
            totalToday = accumulatedBeforeSession
            return true
        }
        return false
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
