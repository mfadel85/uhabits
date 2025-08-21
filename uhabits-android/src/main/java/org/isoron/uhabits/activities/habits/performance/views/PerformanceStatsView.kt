/*
 * Copyright (C) 2025 Enhanced by mfadel85
 *
 * Performance Stats View
 *
 * Shows overview statistics for daily performance
 */

package org.isoron.uhabits.activities.habits.performance.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import org.isoron.uhabits.R
import org.isoron.uhabits.activities.habits.performance.MonthlyStats
import org.isoron.uhabits.core.models.DailyScore
import kotlin.math.roundToInt

class PerformanceStatsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private var dailyScores: List<DailyScore> = emptyList()
    private var monthlyStats: List<MonthlyStats> = emptyList()

    init {
        orientation = VERTICAL
        LayoutInflater.from(context).inflate(R.layout.performance_stats_view, this, true)
    }

    fun setData(scores: List<DailyScore>, stats: List<MonthlyStats>) {
        this.dailyScores = scores
        this.monthlyStats = stats
        refreshData()
    }

    private fun refreshData() {
        if (dailyScores.isEmpty()) return

        // Overall stats
        val overallAverage = dailyScores.map { it.score }.average()
        val totalDays = dailyScores.size
        val excellentDays = dailyScores.count { DailyScore.getCategory(it.score) == DailyScore.ScoreCategory.EXCELLENT }
        val goodDays = dailyScores.count { DailyScore.getCategory(it.score) == DailyScore.ScoreCategory.GOOD }
        val totalHabitsCompleted = dailyScores.sumOf { it.completedHabits }
        val averageHabitsPerDay = totalHabitsCompleted.toDouble() / totalDays

        // Update UI
        findViewById<TextView>(R.id.overallAverageText).text = "${overallAverage.roundToInt()}"
        findViewById<TextView>(R.id.totalDaysText).text = "$totalDays"
        findViewById<TextView>(R.id.excellentDaysText).text = "$excellentDays"
        findViewById<TextView>(R.id.goodDaysText).text = "$goodDays"
        findViewById<TextView>(R.id.totalHabitsText).text = "$totalHabitsCompleted"
        findViewById<TextView>(R.id.avgHabitsPerDayText).text = "${"%.1f".format(averageHabitsPerDay)}"

        // Monthly comparison
        if (monthlyStats.size >= 2) {
            val currentMonth = monthlyStats.first()
            val previousMonth = monthlyStats[1]
            val improvement = currentMonth.averageScore - previousMonth.averageScore

            val improvementText = findViewById<TextView>(R.id.monthlyImprovementText)
            improvementText.text = "${if (improvement >= 0) "+" else ""}${"%.1f".format(improvement)}"
        }
    }
}
