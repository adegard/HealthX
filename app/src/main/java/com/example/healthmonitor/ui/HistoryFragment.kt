package com.example.healthmonitor.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.healthmonitor.MainActivity
import com.example.healthmonitor.R
import com.example.healthmonitor.data.TargetsCalculator
import com.example.healthmonitor.databinding.FragmentHistoryBinding
import com.example.healthmonitor.databinding.ItemHistoryBinding
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!
    private val main get() = requireActivity() as MainActivity

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun render() {
        val profile = main.profileStore.load()
        val history = main.statsStore.getHistory(60)
        val chrono = history.reversed()
        val chartValues = chrono.takeLast(14).map { it.steps.toInt() }
        binding.chart.setValues(chartValues)

        val total = history.sumOf { it.steps }
        val best = history.maxOfOrNull { it.steps } ?: 0
        binding.textSummary.text =
            getString(R.string.history_summary, history.size, total, best)

        binding.listLayout.removeAllViews()
        history.forEach { stat ->
            val row = ItemHistoryBinding.inflate(layoutInflater)
            val distance = TargetsCalculator.distanceKm(stat.steps.toInt(), profile)
            val calories = TargetsCalculator.calories(stat.steps.toInt(), profile)
            row.txtDate.text = formatDate(stat.date)
            row.txtSteps.text = String.format(Locale.US, "%,d", stat.steps)
            val hrText = if (stat.avgHr > 0) " · HR ${stat.avgHr}" else ""
            row.txtMetrics.text = String.format(
                Locale.US, "%.2f km · %.0f kcal%s", distance, calories, hrText
            )
            binding.listLayout.addView(row.root)
        }
    }

    private fun formatDate(dateKey: String): String {
        return try {
            LocalDate.parse(dateKey)
                .format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault()))
        } catch (e: Exception) {
            dateKey
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
