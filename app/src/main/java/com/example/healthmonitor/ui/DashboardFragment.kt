package com.example.healthmonitor.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.healthmonitor.MainActivity
import com.example.healthmonitor.R
import com.example.healthmonitor.data.Achievements
import com.example.healthmonitor.data.LifeExpectancyCalculator
import com.example.healthmonitor.data.TargetsCalculator
import com.example.healthmonitor.databinding.FragmentDashboardBinding
import com.example.healthmonitor.sensor.StepTracker
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.min

class DashboardFragment : Fragment(), StepTracker.Listener {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private val main get() = requireActivity() as MainActivity

    private val handler = Handler(Looper.getMainLooper())
    private var remainingSeconds = 0L

    private val ticker = object : Runnable {
        override fun run() {
            remainingSeconds = (remainingSeconds - 1).coerceAtLeast(0L)
            binding.textLifeCountdown.text = formatCountdown(remainingSeconds)
            handler.postDelayed(this, 1000L)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.textDate.text = LocalDate.now()
            .format(DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.getDefault()))
    }

    override fun onResume() {
        super.onResume()
        main.stepTracker.listener = this
        binding.textSensor.text = getString(
            if (main.stepTracker.usesStepCounterSensor) R.string.sensor_step_counter
            else R.string.sensor_accelerometer
        )
        refresh()
        handler.post(ticker)
    }

    override fun onPause() {
        super.onPause()
        main.stepTracker.listener = null
        handler.removeCallbacks(ticker)
    }

    override fun onStepsChanged(totalToday: Long) {
        if (totalToday > 0) {
            main.statsStore.markActive(main.statsStore.todayKey())
        }
        refresh()
    }

    override fun onSensorTypeChanged(usingStepCounter: Boolean) {
        binding.textSensor.text = getString(
            if (usingStepCounter) R.string.sensor_step_counter else R.string.sensor_accelerometer
        )
    }

    private fun refresh() {
        val profile = main.profileStore.load()
        val steps = main.stepTracker.totalToday
        val goal = TargetsCalculator.dailyStepsGoal(profile)
        val distance = TargetsCalculator.distanceKm(steps.toInt(), profile)
        val calories = TargetsCalculator.calories(steps.toInt(), profile)
        val minutes = TargetsCalculator.activeMinutes(steps.toInt())
        val bmi = TargetsCalculator.bmi(profile)

        Achievements.checkAndEarn(main.statsStore, steps, distance, calories, bmi)

        binding.textSteps.text = String.format(Locale.US, "%,d", steps)
        binding.progressGoal.max = goal
        binding.progressGoal.progress = min(steps, goal.toLong()).toInt()
        binding.textGoal.text = getString(R.string.goal_progress, goal, min(steps, goal.toLong()))
        binding.textDistance.text = String.format(Locale.US, "%.2f km", distance)
        binding.textCalories.text = String.format(Locale.US, "%.0f kcal", calories)
        binding.textMinutes.text = getString(R.string.minutes_value, minutes)
        binding.textBmi.text = String.format(Locale.US, "%.1f", bmi)

        val zones = TargetsCalculator.heartRateZones(profile.age)
        binding.textZones.text = zones.joinToString("\n") { (name, range) ->
            "$name: ${range.first}-${range.last} bpm"
        }

        val streak = Achievements.currentStreak(main.statsStore.getDaysActive())
        binding.textStreak.text = getString(R.string.streak_label, streak)

        val life = LifeExpectancyCalculator.estimate(
            profile,
            main.statsStore.getHistory(30)
        )
        remainingSeconds = life.remainingSeconds
        binding.textLifeCountdown.text = formatCountdown(life.remainingSeconds)
        binding.textLifeDetail.text = getString(
            R.string.life_detail,
            life.stepsAdjustment,
            life.bmiAdjustment
        )
        binding.textLifeBasis.text = if (life.hasStepsData) {
            getString(
                R.string.life_basis,
                String.format(Locale.US, "%,d", life.avgDailySteps),
                life.bmi,
                life.lifespanYears
            )
        } else {
            getString(R.string.life_basis_nodata, life.lifespanYears)
        }
    }

    private fun formatCountdown(seconds: Long): String = getString(
        R.string.life_remaining,
        seconds / LifeExpectancyCalculator.SECONDS_PER_YEAR,
        (seconds % LifeExpectancyCalculator.SECONDS_PER_YEAR) / 86_400L,
        (seconds % 86_400L) / 3_600L,
        (seconds % 3_600L) / 60L,
        seconds % 60L
    )

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
