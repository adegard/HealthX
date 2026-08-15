package com.example.healthmonitor

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.healthmonitor.data.ProfileStore
import com.example.healthmonitor.data.StatsStore
import com.example.healthmonitor.databinding.ActivityMainBinding
import com.example.healthmonitor.sensor.StepService
import com.example.healthmonitor.sensor.StepTracker
import com.example.healthmonitor.ui.AchievementsFragment
import com.example.healthmonitor.ui.DashboardFragment
import com.example.healthmonitor.ui.HeartRateFragment
import com.example.healthmonitor.ui.HistoryFragment
import com.example.healthmonitor.ui.ProfileFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val app get() = application as HealthApp

    lateinit var profileStore: ProfileStore
        private set
    lateinit var statsStore: StatsStore
        private set
    lateinit var stepTracker: StepTracker
        private set

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result[Manifest.permission.ACTIVITY_RECOGNITION] == true && this::stepTracker.isInitialized) {
            stepTracker.start()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        profileStore = app.profileStore
        statsStore = app.statsStore
        stepTracker = app.stepTracker
        stepTracker.start()
        StepService.start(this)

        requestPermissionsIfNeeded()

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> showFragment(DashboardFragment())
                R.id.nav_heart -> showFragment(HeartRateFragment())
                R.id.nav_history -> showFragment(HistoryFragment())
                R.id.nav_achievements -> showFragment(AchievementsFragment())
                R.id.nav_profile -> showFragment(ProfileFragment())
                else -> {}
            }
            true
        }
        binding.bottomNav.selectedItemId = R.id.nav_dashboard
    }

    private fun requestPermissionsIfNeeded() {
        val needed = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            needed.add(Manifest.permission.CAMERA)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            needed.add(Manifest.permission.ACTIVITY_RECOGNITION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            needed.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (needed.isNotEmpty()) {
            permissionLauncher.launch(needed.toTypedArray())
        }
    }

    private fun showFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    override fun onDestroy() {
        if (this::stepTracker.isInitialized) {
            stepTracker.flush()
        }
        super.onDestroy()
    }
}
