/*
 * Copyright (C) 2025 Enhanced by mfadel85
 *
 * Daily Performance Details Activity
 *
 * Shows comprehensive daily performance analytics and monthly breakdowns
 */

package org.isoron.uhabits.activities.habits.performance

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.isoron.uhabits.HabitsApplication
import org.isoron.uhabits.core.models.DailyScore
import org.isoron.uhabits.core.models.DailyScoreCalculator
import org.isoron.uhabits.core.models.PaletteColor
import org.isoron.uhabits.core.preferences.Preferences
import org.isoron.uhabits.core.tasks.TaskRunner
import org.isoron.uhabits.core.utils.DateUtils
import org.isoron.uhabits.inject.ActivityContext
import org.isoron.uhabits.inject.ActivityContextModule
import org.isoron.uhabits.inject.ActivityScope
import org.isoron.uhabits.inject.DaggerHabitsActivityComponent
import org.isoron.uhabits.inject.HabitsActivityComponent
import org.isoron.uhabits.inject.HabitsApplicationComponent
import org.isoron.uhabits.utils.applyRootViewInsets
import org.isoron.uhabits.utils.currentTheme
import javax.inject.Inject

class DailyPerformanceDetailsActivity : AppCompatActivity() {

    lateinit var appComponent: HabitsApplicationComponent
    lateinit var component: HabitsActivityComponent
    lateinit var rootView: DailyPerformanceDetailsRootView
    lateinit var taskRunner: TaskRunner
    private lateinit var dailyScoreCalculator: DailyScoreCalculator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appComponent = (applicationContext as HabitsApplication).component
        component = DaggerHabitsActivityComponent
            .builder()
            .activityContextModule(ActivityContextModule(this))
            .habitsApplicationComponent(appComponent)
            .build()
        component.themeSwitcher.apply()

        taskRunner = appComponent.taskRunner
        dailyScoreCalculator = DailyScoreCalculator(appComponent.habitList)
        rootView = DailyPerformanceDetailsRootView(this, dailyScoreCalculator, taskRunner)
        
        rootView.applyRootViewInsets()
        setContentView(rootView)
        
        loadPerformanceData()
    }

    private fun loadPerformanceData() {
        taskRunner.run {
            val last90Days = dailyScoreCalculator.getLast90Days()
            val monthlyStats = calculateMonthlyStats(last90Days)
            val weeklyStats = dailyScoreCalculator.getWeeklyAverages()
            
            runOnUiThread {
                rootView.setPerformanceData(last90Days, monthlyStats, weeklyStats)
            }
        }
    }

    private fun calculateMonthlyStats(dailyScores: List<DailyScore>): List<MonthlyStats> {
        val monthlyGroups = dailyScores.groupBy { score ->
            val calendar = score.timestamp.toCalendar()
            "${calendar.get(java.util.Calendar.YEAR)}-${calendar.get(java.util.Calendar.MONTH)}"
        }

        return monthlyGroups.map { (monthKey, scores) ->
            val calendar = scores.first().timestamp.toCalendar()
            val monthName = java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.getDefault())
                .format(calendar.time)

            MonthlyStats(
                monthKey = monthKey,
                monthName = monthName,
                averageScore = scores.map { it.score }.average(),
                bestDay = scores.maxByOrNull { it.score },
                worstDay = scores.minByOrNull { it.score },
                excellentDays = scores.count { it.category == DailyScore.ScoreCategory.EXCELLENT },
                goodDays = scores.count { it.category == DailyScore.ScoreCategory.GOOD },
                averageDays = scores.count { it.category == DailyScore.ScoreCategory.AVERAGE },
                poorDays = scores.count { it.category == DailyScore.ScoreCategory.POOR },
                totalDays = scores.size,
                totalHabitsCompleted = scores.sumOf { it.completedHabits },
                averageHabitsPerDay = scores.map { it.completedHabits }.average()
            )
        }.sortedByDescending { it.monthKey }
    }

    companion object {
        fun createIntent(context: Context): Intent {
            return Intent(context, DailyPerformanceDetailsActivity::class.java)
        }
    }
}

// Extension function for DailyScoreCalculator
fun DailyScoreCalculator.getLast90Days(): List<DailyScore> {
    val today = DateUtils.getTodayWithOffset()
    val ninetyDaysAgo = today.minus(89)
    return calculateScoreRange(ninetyDaysAgo, today)
}

data class MonthlyStats(
    val monthKey: String,
    val monthName: String,
    val averageScore: Double,
    val bestDay: DailyScore?,
    val worstDay: DailyScore?,
    val excellentDays: Int,
    val goodDays: Int,
    val averageDays: Int,
    val poorDays: Int,
    val totalDays: Int,
    val totalHabitsCompleted: Int,
    val averageHabitsPerDay: Double
)
