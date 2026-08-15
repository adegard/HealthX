package com.example.healthmonitor.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.healthmonitor.R
import com.example.healthmonitor.advice.SedentaryReminder
import com.example.healthmonitor.databinding.FragmentAdviceBinding

class AdviceFragment : Fragment() {

    private var _binding: FragmentAdviceBinding? = null
    private val binding get() = _binding!!

    private val intervalValues = intArrayOf(30, 60, 90, 120)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdviceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.textWalking.text = joinList(R.array.walking_tips)
        binding.textMorning.text = joinList(R.array.morning_routine_items)
        binding.textExercises.text = joinList(R.array.exercise_items)

        binding.spinnerInterval.adapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.sedentary_intervals,
            android.R.layout.simple_spinner_item
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        val current = SedentaryReminder.intervalMin(requireContext())
        val defaultIndex = intervalValues.indexOf(current).coerceAtLeast(0)
        binding.spinnerInterval.setSelection(defaultIndex)

        binding.switchSedentary.isChecked = SedentaryReminder.isEnabled(requireContext())
        binding.switchSedentary.setOnCheckedChangeListener { _, isChecked ->
            val minutes = intervalValues[binding.spinnerInterval.selectedItemPosition]
            SedentaryReminder.set(requireContext(), isChecked, minutes)
            Toast.makeText(
                requireContext(),
                if (isChecked) R.string.advice_reminder_enabled else R.string.advice_reminder_disabled,
                Toast.LENGTH_SHORT
            ).show()
        }
        binding.spinnerInterval.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (binding.switchSedentary.isChecked) {
                    val minutes = intervalValues[position]
                    SedentaryReminder.set(requireContext(), true, minutes)
                }
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        })
    }

    private fun joinList(arrayRes: Int): String =
        resources.getStringArray(arrayRes).joinToString("\n") { "•  $it" }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
