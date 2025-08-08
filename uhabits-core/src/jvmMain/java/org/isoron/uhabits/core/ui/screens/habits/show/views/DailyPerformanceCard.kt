/*
 * Copyright (C) 2025 Enhanced by mfadel85
 *
 * Daily Performance Card for uHabits
 *
 * Displays daily performance metrics and charts
 */

package org.isoron.uhabits.core.ui.screens.habits.show.views

import org.isoron.uhabits.core.models.DailyScore
import org.isoron.uhabits.core.models.Entry
import org.isoron.uhabits.core.models.Habit
import org.isoron.uhabits.core.models.PaletteColor
import org.isoron.uhabits.core.preferences.Preferences
import org.isoron.uhabits.core.ui.views.Theme
import org.isoron.uhabits.core.utils.DateUtils

data class DailyPerformanceCardState(
    val color: PaletteColor,
    val dailyScores: List<DailyScore>,
    val weeklyAverages: List<DailyScore> = emptyList(),
    val currentScore: DailyScore,
    val completionRate: Float,
    val todayCompletionRate: Float,
    val bestDay: DailyScore?,
    val worstDay: DailyScore?,
    val trend: String,
    val weeklyTrend: TrendDirection,
    val monthlyTrend: TrendDirection,
    val averageScore: Double,
    val totalDaysTracked: Int,
    val spinnerPosition: Int,
    val theme: Theme
) {
    enum class TrendDirection {
        UP, DOWN, STABLE
    }
}

class DailyPerformanceCardPresenter(
    val preferences: Preferences,
    val screen: Screen
) {

    companion object {
        fun buildState(
            habit: Habit,
            spinnerPosition: Int,
            theme: Theme
        ): DailyPerformanceCardState {
            val days = when (spinnerPosition) {
                0 -> 7 // Last 7 days
                1 -> 30 // Last 30 days
                2 -> 84 // Last 12 weeks
                else -> 30
            }

            val today = DateUtils.getTodayWithOffset()
            val startDate = today.minus(days - 1)

            // For single habit, create daily scores based on habit completion
            val dailyScores = mutableListOf<DailyScore>()

            for (i in 0 until days) {
                val date = startDate.plus(i)
                val entry = habit.computedEntries.get(date)
                val score = when {
                    entry.value >= Entry.YES_MANUAL -> 100.0
                    entry.value > 0 -> (entry.value.toDouble() / 1000 * 100)
                    else -> 0.0
                }

                dailyScores.add(
                    DailyScore(
                        timestamp = date,
                        score = score,
                        category = when {
                            score >= 80.0 -> DailyScore.ScoreCategory.EXCELLENT
                            score >= 60.0 -> DailyScore.ScoreCategory.GOOD
                            score >= 40.0 -> DailyScore.ScoreCategory.AVERAGE
                            else -> DailyScore.ScoreCategory.POOR
                        },
                        completedHabits = if (score > 0) 1 else 0,
                        totalHabits = 1,
                        skippedHabits = if (entry.value == Entry.SKIP) 1 else 0
                    )
                )
            }

            val currentScore = dailyScores.lastOrNull() ?: DailyScore(
                timestamp = today,
                score = 0.0,
                category = DailyScore.ScoreCategory.POOR,
                completedHabits = 0,
                totalHabits = 1,
                skippedHabits = 0
            )

            val completionRate = if (dailyScores.isNotEmpty()) {
                dailyScores.count { it.score > 0 }.toFloat() / dailyScores.size
            } else {
                0f
            }

            val todayCompletionRate = currentScore.score.toFloat()

            val bestDay = dailyScores.maxByOrNull { it.score }
            val worstDay = dailyScores.minByOrNull { it.score }

            val averageScore = if (dailyScores.isNotEmpty()) {
                dailyScores.map { it.score }.average()
            } else {
                0.0
            }

            // Calculate trends
            val weeklyTrend = calculateTrend(dailyScores.takeLast(7))
            val monthlyTrend = calculateTrend(dailyScores.takeLast(30))

            // Calculate trend (simple: compare first half vs second half)
            val trend = if (dailyScores.size >= 4) {
                val firstHalf = dailyScores.take(dailyScores.size / 2)
                val secondHalf = dailyScores.drop(dailyScores.size / 2)
                val firstAvg = firstHalf.map { it.score }.average()
                val secondAvg = secondHalf.map { it.score }.average()
                when {
                    secondAvg > firstAvg + 5 -> "Improving"
                    secondAvg < firstAvg - 5 -> "Declining"
                    else -> "Stable"
                }
            } else {
                "Stable"
            }

            return DailyPerformanceCardState(
                color = habit.color,
                dailyScores = dailyScores,
                weeklyAverages = emptyList(), // For single habit, we don't need weekly averages
                currentScore = currentScore,
                completionRate = completionRate,
                todayCompletionRate = todayCompletionRate,
                bestDay = bestDay,
                worstDay = worstDay,
                trend = trend,
                weeklyTrend = weeklyTrend,
                monthlyTrend = monthlyTrend,
                averageScore = averageScore,
                totalDaysTracked = dailyScores.size,
                spinnerPosition = spinnerPosition,
                theme = theme
            )
        }

        private fun calculateTrend(scores: List<DailyScore>): DailyPerformanceCardState.TrendDirection {
            if (scores.size < 2) return DailyPerformanceCardState.TrendDirection.STABLE

            val firstHalf = scores.take(scores.size / 2)
            val secondHalf = scores.drop(scores.size / 2)

            if (firstHalf.isEmpty() || secondHalf.isEmpty()) return DailyPerformanceCardState.TrendDirection.STABLE

            val firstAvg = firstHalf.map { it.score }.average()
            val secondAvg = secondHalf.map { it.score }.average()

            return when {
                secondAvg > firstAvg + 5 -> DailyPerformanceCardState.TrendDirection.UP
                secondAvg < firstAvg - 5 -> DailyPerformanceCardState.TrendDirection.DOWN
                else -> DailyPerformanceCardState.TrendDirection.STABLE
            }
        }
    }

    fun onSpinnerPosition(position: Int) {
        preferences.dailyPerformanceSpinnerPosition = position
        screen.updateWidgets()
        screen.refresh()
    }

    interface Screen {
        fun updateWidgets()
        fun refresh()
    }
}
