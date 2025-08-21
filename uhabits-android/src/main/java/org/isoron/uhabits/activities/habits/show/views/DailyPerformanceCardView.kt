/*
 * Copyright (C) 2025 Enhanced by mfadel85
 *
 * Daily Performance Card View for Android
 *
 * Android implementation of the daily performance visualization
 */

package org.isoron.uhabits.activities.habits.show.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.LinearLayout
import android.widget.TextView
import org.isoron.platform.gui.toInt
import org.isoron.uhabits.core.ui.screens.habits.show.views.DailyPerformanceCardPresenter
import org.isoron.uhabits.core.ui.screens.habits.show.views.DailyPerformanceCardState
import org.isoron.uhabits.databinding.ShowHabitDailyPerformanceBinding
import java.text.DecimalFormat

class DailyPerformanceCardView(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs) {

    private var binding = ShowHabitDailyPerformanceBinding.inflate(LayoutInflater.from(context), this, true)
    private val scoreFormat = DecimalFormat("#.0")

    fun setState(state: DailyPerformanceCardState) {
        val androidColor = state.theme.color(state.color).toInt()

        // Set title color
        binding.title.setTextColor(androidColor)

        // Set spinner selection
        binding.spinner.setSelection(state.spinnerPosition)

        // Configure chart
        val chart = binding.performanceChart
        chart.setColor(androidColor)

        if (state.dailyScores.isNotEmpty()) {
            chart.setDailyScores(state.dailyScores)
        } else {
            chart.setDailyScores(state.weeklyAverages)
        }

        // Update current score display
        binding.currentScoreValue.text = scoreFormat.format(state.currentScore.weightedScore)
        binding.currentScoreValue.setTextColor(getCategoryColor(state.currentScore.weightedCategory))
        binding.currentScoreCategory.text = state.currentScore.weightedCategory.name.lowercase()
            .replaceFirstChar { it.uppercase() }

        // Update completion rate
        binding.completionRateValue.text = "${scoreFormat.format(state.todayCompletionRate)}%"
        binding.completionRateProgress.progress = state.todayCompletionRate.toInt()
        binding.completionRateProgress.progressTintList = android.content.res.ColorStateList.valueOf(androidColor)

        // Update habits completed today
        binding.habitsCompletedValue.text = "${state.currentScore.completedHabits}/${state.currentScore.totalHabits}"

        // Update trend indicators
        updateTrendIndicator(binding.weeklyTrendIcon, binding.weeklyTrendText, state.weeklyTrend, "Weekly")
        updateTrendIndicator(binding.monthlyTrendIcon, binding.monthlyTrendText, state.monthlyTrend, "Monthly")

        // Update statistics
        binding.averageScoreValue.text = scoreFormat.format(state.averageScore)
        binding.totalDaysValue.text = state.totalDaysTracked.toString()

        // Update best/worst day
        state.bestDay?.let { best ->
            binding.bestDayScore.text = scoreFormat.format(best.weightedScore)
            binding.bestDayScore.setTextColor(getCategoryColor(best.weightedCategory))
            binding.bestDayDate.text = java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault())
                .format(best.timestamp.toJavaDate())
        }

        state.worstDay?.let { worst ->
            binding.worstDayScore.text = scoreFormat.format(worst.weightedScore)
            binding.worstDayScore.setTextColor(getCategoryColor(worst.weightedCategory))
            binding.worstDayDate.text = java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault())
                .format(worst.timestamp.toJavaDate())
        }

        // Show appropriate sections based on data
        binding.weeklySection.visibility = if (state.weeklyAverages.isNotEmpty()) View.VISIBLE else View.GONE
        binding.dailySection.visibility = if (state.dailyScores.isNotEmpty()) View.VISIBLE else View.GONE
    }

    fun setListener(presenter: DailyPerformanceCardPresenter) {
        binding.spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                presenter.onSpinnerPosition(position)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun updateTrendIndicator(
        iconView: TextView,
        textView: TextView,
        trend: DailyPerformanceCardState.TrendDirection,
        label: String
    ) {
        when (trend) {
            DailyPerformanceCardState.TrendDirection.UP -> {
                iconView.text = "↗"
                iconView.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
                textView.text = "$label ↗"
                textView.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
            }
            DailyPerformanceCardState.TrendDirection.DOWN -> {
                iconView.text = "↘"
                iconView.setTextColor(android.graphics.Color.parseColor("#F44336"))
                textView.text = "$label ↘"
                textView.setTextColor(android.graphics.Color.parseColor("#F44336"))
            }
            DailyPerformanceCardState.TrendDirection.STABLE -> {
                iconView.text = "→"
                iconView.setTextColor(android.graphics.Color.parseColor("#FF9800"))
                textView.text = "$label →"
                textView.setTextColor(android.graphics.Color.parseColor("#FF9800"))
            }
        }
    }

    private fun getCategoryColor(category: org.isoron.uhabits.core.models.DailyScore.ScoreCategory): Int {
        return when (category) {
            org.isoron.uhabits.core.models.DailyScore.ScoreCategory.EXCELLENT -> android.graphics.Color.parseColor("#4CAF50")
            org.isoron.uhabits.core.models.DailyScore.ScoreCategory.GOOD -> android.graphics.Color.parseColor("#8BC34A")
            org.isoron.uhabits.core.models.DailyScore.ScoreCategory.AVERAGE -> android.graphics.Color.parseColor("#FF9800")
            org.isoron.uhabits.core.models.DailyScore.ScoreCategory.POOR -> android.graphics.Color.parseColor("#F44336")
        }
    }
}
