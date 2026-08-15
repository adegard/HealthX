package com.example.healthmonitor.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.healthmonitor.MainActivity
import com.example.healthmonitor.R
import com.example.healthmonitor.data.ProfileStore
import com.example.healthmonitor.data.TargetsCalculator
import com.example.healthmonitor.data.UserProfile
import com.example.healthmonitor.databinding.FragmentProfileBinding
import java.util.Locale

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val main get() = requireActivity() as MainActivity
    private lateinit var store: ProfileStore

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        store = main.profileStore

        binding.spinnerSex.adapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.sex_options,
            android.R.layout.simple_spinner_item
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        binding.spinnerActivity.adapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.activity_levels,
            android.R.layout.simple_spinner_item
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        binding.btnSave.setOnClickListener { save() }
        loadProfile()
    }

    private fun loadProfile() {
        val profile = store.load()
        binding.edtName.setText(profile.name)
        binding.edtAge.setText(profile.age.toString())
        binding.spinnerSex.setSelection(if (profile.sex == "female") 1 else 0)
        binding.edtHeight.setText(trimDecimal(profile.heightCm))
        binding.edtWeight.setText(trimDecimal(profile.weightKg))
        binding.spinnerActivity.setSelection(profile.activityLevel)
        binding.edtGoal.setText(profile.customGoal.toString())
        updatePreview(profile)
    }

    private fun trimDecimal(value: Double): String =
        if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()

    private fun save() {
        val profile = UserProfile(
            name = binding.edtName.text?.toString()?.trim().orEmpty(),
            age = binding.edtAge.text?.toString()?.toIntOrNull() ?: 30,
            sex = if (binding.spinnerSex.selectedItemPosition == 1) "female" else "male",
            heightCm = binding.edtHeight.text?.toString()?.toDoubleOrNull() ?: 170.0,
            weightKg = binding.edtWeight.text?.toString()?.toDoubleOrNull() ?: 70.0,
            activityLevel = binding.spinnerActivity.selectedItemPosition,
            customGoal = binding.edtGoal.text?.toString()?.toIntOrNull() ?: 0
        )
        store.save(profile)
        updatePreview(profile)
        Toast.makeText(requireContext(), R.string.saved, Toast.LENGTH_SHORT).show()
    }

    private fun updatePreview(profile: UserProfile) {
        val goal = TargetsCalculator.dailyStepsGoal(profile)
        val maxHr = TargetsCalculator.maxHeartRate(profile.age)
        val bmi = TargetsCalculator.bmi(profile)
        val zones = TargetsCalculator.heartRateZones(profile.age)

        binding.txtTargetsPreview.text = String.format(
            Locale.US,
            "Daily goal: %,d steps\nMax HR: %d bpm\nBMI: %.1f\n\nZones:\n%s",
            goal,
            maxHr,
            bmi,
            zones.joinToString("\n") { (name, range) -> "$name: ${range.first}-${range.last} bpm" }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
