package com.example.healthmonitor.ui

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.healthmonitor.R
import com.example.healthmonitor.data.Achievements
import com.example.healthmonitor.databinding.FragmentAchievementsBinding
import com.example.healthmonitor.databinding.ItemAchievementBinding

class AchievementsFragment : Fragment() {

    private var _binding: FragmentAchievementsBinding? = null
    private val binding get() = _binding!!
    private val main get() = requireActivity() as MainActivity

    private val colors = intArrayOf(
        Color.rgb(224, 54, 90),
        Color.rgb(244, 162, 97),
        Color.rgb(250, 204, 21),
        Color.rgb(54, 209, 161),
        Color.rgb(36, 160, 224),
        Color.rgb(168, 85, 247)
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAchievementsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun render() {
        val earned = main.statsStore.getEarnedAchievements()
        val all = Achievements.all()
        binding.textProgress.text = getString(R.string.achievements_progress, earned.size, all.size)
        binding.listLayout.removeAllViews()

        all.forEachIndexed { index, achievement ->
            val row = ItemAchievementBinding.inflate(layoutInflater)
            val isEarned = achievement.id in earned
            val dotColor = if (isEarned) colors[index % colors.size]
            else Color.argb(60, 255, 255, 255)

            row.dot.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(dotColor)
            }
            row.txtTitle.text = achievement.title
            row.txtTitle.setTextColor(
                if (isEarned) Color.WHITE else Color.rgb(140, 146, 158)
            )
            row.txtDesc.text = achievement.description
            binding.listLayout.addView(row.root)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
