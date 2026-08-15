package com.example.healthmonitor.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.healthmonitor.MainActivity
import com.example.healthmonitor.R
import com.example.healthmonitor.camera.HeartRateAnalyzer
import com.example.healthmonitor.data.Achievements
import com.example.healthmonitor.data.TargetsCalculator
import com.example.healthmonitor.databinding.FragmentHeartRateBinding
import java.util.concurrent.Executors
import kotlin.math.roundToInt

class HeartRateFragment : Fragment() {

    private var _binding: FragmentHeartRateBinding? = null
    private val binding get() = _binding!!
    private val main get() = requireActivity() as MainActivity

    private lateinit var analyzer: HeartRateAnalyzer
    private var camera: Camera? = null
    private val executor = Executors.newSingleThreadExecutor()

    private var cameraReady = false
    private var measuring = false
    private var gotReading = false
    private var lastSampleTime = 0L
    private var lastUiUpdate = 0L
    private var readingSum = 0.0
    private var readingCount = 0

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result[Manifest.permission.CAMERA] == true) {
            startCamera()
        } else {
            Toast.makeText(requireContext(), R.string.camera_permission_needed, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHeartRateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        analyzer = HeartRateAnalyzer()

        binding.textBpm.text = "--"
        binding.textStatus.setText(R.string.press_start)
        binding.buttonMeasure.setOnClickListener { toggleMeasurement() }

        val profile = main.profileStore.load()
        val zones = TargetsCalculator.heartRateZones(profile.age)
        binding.textZones.text = getString(R.string.zones_hint) + "\n" +
            zones.joinToString("\n") { (name, range) -> "$name: ${range.first}-${range.last} bpm" }
        binding.textDisclaimer.setText(R.string.hr_disclaimer)

        startCameraIfReady()
    }

    private fun startCameraIfReady() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA))
        }
    }

    private fun startCamera() {
        val future = ProcessCameraProvider.getInstance(requireContext())
        future.addListener({
            val provider = future.get()

            val preview = Preview.Builder().build()
            preview.setSurfaceProvider(binding.previewView.surfaceProvider)

            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                .build()
            analysis.setAnalyzer(executor) { proxy -> analyzeFrame(proxy) }

            provider.unbindAll()
            try {
                camera = provider.bindToLifecycle(
                    viewLifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis
                )
                cameraReady = true
                if (camera?.cameraInfo?.hasFlashUnit() == false) {
                    requireActivity().runOnUiThread {
                        binding.textStatus.setText(R.string.no_flash)
                    }
                }
            } catch (e: Exception) {
                requireActivity().runOnUiThread {
                    Toast.makeText(requireContext(), R.string.camera_error, Toast.LENGTH_SHORT).show()
                }
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun toggleMeasurement() {
        if (measuring) stopMeasurement() else startMeasurement()
    }

    private fun startMeasurement() {
        if (!cameraReady) {
            startCameraIfReady()
            return
        }
        measuring = true
        analyzer.reset()
        gotReading = false
        readingSum = 0.0
        readingCount = 0
        binding.buttonMeasure.setText(R.string.stop)
        binding.textStatus.setText(R.string.place_finger)
        binding.textConfidence.setText(R.string.measuring)
        camera?.cameraControl?.enableTorch(true)
    }

    private fun stopMeasurement() {
        if (!measuring) return
        measuring = false
        camera?.cameraControl?.enableTorch(false)
        binding.buttonMeasure.setText(R.string.start)
        binding.textStatus.setText(R.string.press_start)

        if (gotReading && readingCount > 0) {
            val average = (readingSum / readingCount).roundToInt()
            main.statsStore.saveHeartRate(average)
            main.statsStore.addHeartRateSession()
            binding.textBpm.text = average.toString()
            binding.textConfidence.text = getString(R.string.session_average, average)

            val profile = main.profileStore.load()
            val steps = main.stepTracker.totalToday
            Achievements.checkAndEarn(
                main.statsStore,
                steps,
                TargetsCalculator.distanceKm(steps.toInt(), profile),
                TargetsCalculator.calories(steps.toInt(), profile),
                TargetsCalculator.bmi(profile),
                hrRead = true
            )
        } else {
            binding.textBpm.text = "--"
            binding.textConfidence.setText(R.string.no_reading)
        }
    }

    private fun analyzeFrame(image: ImageProxy) {
        try {
            if (!measuring) {
                image.close()
                return
            }
            val now = System.currentTimeMillis()
            if (now - lastSampleTime < 30) {
                image.close()
                return
            }
            lastSampleTime = now

            val frame = image.image ?: run {
                image.close()
                return
            }

            val red = averageRed(frame, image.width, image.height)
            analyzer.addSample(red)

            if (analyzer.size >= 120) {
                val result = analyzer.analyze()
                if (result != null && result.bpm > 0) {
                    gotReading = true
                    readingSum += result.bpm
                    readingCount++
                    updateUi(result)
                }
            }
            image.close()
        } catch (e: Exception) {
            image.close()
        }
    }

    private fun averageRed(frame: android.media.Image, width: Int, height: Int): Double {
        val yPlane = frame.planes[0]
        val vPlane = frame.planes[2]
        val yBuffer = yPlane.buffer
        val vBuffer = vPlane.buffer
        val yRow = yPlane.rowStride
        val vRow = vPlane.rowStride
        val yPixel = yPlane.pixelStride
        val vPixel = vPlane.pixelStride

        val left = (width * 0.3).toInt()
        val top = (height * 0.3).toInt()
        val right = (width * 0.7).toInt()
        val bottom = (height * 0.7).toInt()

        var sum = 0.0
        var count = 0
        var y = top
        while (y < bottom) {
            var x = left
            while (x < right) {
                val yIndex = y * yRow + x * yPixel
                val vIndex = (y / 2) * vRow + (x / 2) * vPixel
                if (yIndex + yPixel <= yBuffer.limit() && vIndex + vPixel <= vBuffer.limit()) {
                    val Y = (yBuffer.get(yIndex).toInt() and 0xFF).toDouble()
                    val V = (vBuffer.get(vIndex).toInt() and 0xFF).toDouble()
                    sum += Y + 1.402 * (V - 128.0)
                    count++
                }
                x += 4
            }
            y += 4
        }
        return if (count > 0) sum / count else 0.0
    }

    private fun updateUi(result: HeartRateAnalyzer.Result) {
        val now = System.currentTimeMillis()
        if (now - lastUiUpdate < 400) return
        lastUiUpdate = now
        requireActivity().runOnUiThread {
            binding.textBpm.text = result.bpm.toString()
            binding.textConfidence.text =
                getString(R.string.confidence_pct, (result.confidence * 100).toInt())
            binding.waveform.setWaveform(result.waveform)
        }
    }

    override fun onDestroyView() {
        if (measuring) {
            measuring = false
            camera?.cameraControl?.enableTorch(false)
        }
        camera = null
        executor.shutdown()
        _binding = null
        super.onDestroyView()
    }
}
