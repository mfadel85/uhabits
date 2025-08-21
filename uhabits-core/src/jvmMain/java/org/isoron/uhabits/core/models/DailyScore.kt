/*
 * Copyright (C) 2025 Enhanced by mfadel85
 *
 * Daily Performance Score calculation for uHabits
 *
 * Provides comprehensive daily scoring based on habit completion
 */

package org.isoron.uhabits.core.models

import org.isoron.uhabits.core.utils.DateUtils
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Represents a daily performance score for all habits
 */
data class DailyScore(
    val timestamp: Timestamp,
    val score: Double,
    val weightedScore: Double,
    val totalHabits: Int,
    val completedHabits: Int,
    val skippedHabits: Int,
    val category: ScoreCategory,
    val weightedCategory: ScoreCategory
) {
    enum class ScoreCategory {
        EXCELLENT, // 80-100
        GOOD, // 60-79
        AVERAGE, // 40-59
        POOR // 0-39
    }

    companion object {
        fun getCategory(score: Double): ScoreCategory {
            return when {
                score >= 80.0 -> ScoreCategory.EXCELLENT
                score >= 60.0 -> ScoreCategory.GOOD
                score >= 40.0 -> ScoreCategory.AVERAGE
                else -> ScoreCategory.POOR
            }
        }
    }
}

/**
 * Calculator for daily performance scores across all habits
 */
class DailyScoreCalculator(private val habitList: HabitList) {

    /**
     * Calculate daily score for a specific date
     */
    fun calculateDailyScore(timestamp: Timestamp): DailyScore {
        val activeHabits = habitList.getFiltered(
            HabitMatcher(isArchivedAllowed = false)
        )

        if (activeHabits.size() == 0) {
            return DailyScore(
                timestamp = timestamp,
                score = 0.0,
                weightedScore = 0.0,
                totalHabits = 0,
                completedHabits = 0,
                skippedHabits = 0,
                category = DailyScore.ScoreCategory.POOR,
                weightedCategory = DailyScore.ScoreCategory.POOR
            )
        }

        var totalScore = 0.0
        var totalWeightedScore = 0.0
        var totalWeight = 0.0
        var completedCount = 0
        var skippedCount = 0
        val totalCount = activeHabits.size()

        for (i in 0 until activeHabits.size()) {
            val habit = activeHabits.getByPosition(i)
            val entry = habit.computedEntries.get(timestamp)
            val habitScore = habit.scores[timestamp].value
            // Safe access to priority with fallback
            val habitWeight = try {
                habit.priority?.weight ?: 1.0
            } catch (e: Exception) {
                1.0 // Default weight if priority access fails
            }

            totalWeight += habitWeight

            when {
                entry.value == Entry.YES_MANUAL || entry.value == Entry.YES_AUTO -> {
                    completedCount++
                    val basePoints = habitScore * 100 + getCompletionBonus(habit, timestamp)
                    // Unweighted score
                    totalScore += basePoints
                    // Weighted score
                    totalWeightedScore += basePoints * habitWeight
                }
                entry.value == Entry.SKIP -> {
                    skippedCount++
                    val basePoints = habitScore * 50
                    // Unweighted score
                    totalScore += basePoints
                    // Weighted score
                    totalWeightedScore += basePoints * habitWeight
                }
                entry.value == Entry.NO || entry.value == Entry.UNKNOWN -> {
                    // No points for missed habits (both weighted and unweighted)
                    totalScore += 0.0
                    totalWeightedScore += 0.0
                }
                else -> {
                    // Numerical habits - check against target
                    if (habit.isNumerical) {
                        val targetMet = isTargetMet(habit, entry)
                        if (targetMet) {
                            completedCount++
                            val basePoints = habitScore * 100 + getCompletionBonus(habit, timestamp)
                            totalScore += basePoints
                            totalWeightedScore += basePoints * habitWeight
                        } else {
                            // Partial credit based on percentage completed
                            val partialCredit = calculatePartialCredit(habit, entry)
                            val basePoints = habitScore * partialCredit
                            totalScore += basePoints
                            totalWeightedScore += basePoints * habitWeight
                        }
                    }
                }
            }
        }

        // Calculate final scores (0-100 scale)
        val finalScore = if (totalCount > 0) {
            val baseScore = totalScore / totalCount
            val consistencyBonus = getConsistencyBonus(activeHabits, timestamp)
            val streakBonus = getStreakBonus(activeHabits, timestamp)

            min(100.0, baseScore + consistencyBonus + streakBonus)
        } else {
            0.0
        }

        val finalWeightedScore = if (totalWeight > 0) {
            val baseWeightedScore = totalWeightedScore / totalWeight
            val consistencyBonus = getConsistencyBonus(activeHabits, timestamp)
            val streakBonus = getStreakBonus(activeHabits, timestamp)

            min(100.0, baseWeightedScore + consistencyBonus + streakBonus)
        } else {
            0.0
        }

        return DailyScore(
            timestamp = timestamp,
            score = finalScore,
            weightedScore = finalWeightedScore,
            totalHabits = totalCount,
            completedHabits = completedCount,
            skippedHabits = skippedCount,
            category = DailyScore.getCategory(finalScore),
            weightedCategory = DailyScore.getCategory(finalWeightedScore)
        )
    }

    /**
     * Calculate scores for a range of days
     */
    fun calculateScoreRange(fromDate: Timestamp, toDate: Timestamp): List<DailyScore> {
        val scores = mutableListOf<DailyScore>()
        var current = fromDate

        while (current <= toDate) {
            scores.add(calculateDailyScore(current))
            current = current.plus(1)
        }

        return scores.reversed() // Most recent first
    }

    /**
     * Get the last 30 days of scores
     */
    fun getLast30Days(): List<DailyScore> {
        val today = DateUtils.getTodayWithOffset()
        val thirtyDaysAgo = today.minus(29)
        return calculateScoreRange(thirtyDaysAgo, today)
    }

    /**
     * Get weekly averages for the last 12 weeks
     */
    fun getWeeklyAverages(): List<DailyScore> {
        val today = DateUtils.getTodayWithOffset()
        val weeklyScores = mutableListOf<DailyScore>()

        for (week in 0 until 12) {
            val weekStart = today.minus(week * 7 + 6)
            val weekEnd = today.minus(week * 7)

            val dailyScores = calculateScoreRange(weekStart, weekEnd)
            val avgScore = dailyScores.map { it.score }.average()
            val avgWeightedScore = dailyScores.map { it.weightedScore }.average()
            val totalHabits = dailyScores.maxOfOrNull { it.totalHabits } ?: 0
            val avgCompleted = dailyScores.map { it.completedHabits }.average().roundToInt()
            val avgSkipped = dailyScores.map { it.skippedHabits }.average().roundToInt()

            weeklyScores.add(
                DailyScore(
                    timestamp = weekEnd, // Use end of week as timestamp
                    score = avgScore,
                    weightedScore = avgWeightedScore,
                    totalHabits = totalHabits,
                    completedHabits = avgCompleted,
                    skippedHabits = avgSkipped,
                    category = DailyScore.getCategory(avgScore),
                    weightedCategory = DailyScore.getCategory(avgWeightedScore)
                )
            )
        }

        return weeklyScores
    }

    private fun isTargetMet(habit: Habit, entry: Entry): Boolean {
        if (!habit.isNumerical) return entry.value == Entry.YES_MANUAL || entry.value == Entry.YES_AUTO

        val value = entry.value / 1000.0
        return when (habit.targetType) {
            NumericalHabitType.AT_LEAST -> value >= habit.targetValue
            NumericalHabitType.AT_MOST -> value <= habit.targetValue
        }
    }

    private fun calculatePartialCredit(habit: Habit, entry: Entry): Double {
        if (!habit.isNumerical) return 0.0

        val value = entry.value / 1000.0
        val target = habit.targetValue

        return when (habit.targetType) {
            NumericalHabitType.AT_LEAST -> {
                if (target > 0) min(100.0, (value / target) * 100.0) else 0.0
            }
            NumericalHabitType.AT_MOST -> {
                if (target > 0 && value <= target) {
                    100.0
                } else {
                    max(0.0, 100.0 - ((value - target) / target) * 50.0)
                }
            }
        }
    }

    private fun getCompletionBonus(habit: Habit, timestamp: Timestamp): Double {
        // Bonus points for completing habits on difficult days (weekends, etc.)
        val calendar = timestamp.toCalendar()
        val dayOfWeek = calendar.get(java.util.Calendar.DAY_OF_WEEK)

        return when (dayOfWeek) {
            java.util.Calendar.SATURDAY, java.util.Calendar.SUNDAY -> 2.0 // Weekend bonus
            java.util.Calendar.MONDAY -> 1.0 // Monday motivation bonus
            else -> 0.0
        }
    }

    private fun getConsistencyBonus(habits: HabitList, timestamp: Timestamp): Double {
        // Bonus for maintaining consistency across multiple habits
        val last7Days = (0..6).map { timestamp.minus(it) }
        var consistentHabits = 0

        for (i in 0 until habits.size()) {
            val habit = habits.getByPosition(i)
            val completed = last7Days.count { day ->
                val entry = habit.computedEntries.get(day)
                entry.value == Entry.YES_MANUAL || entry.value == Entry.YES_AUTO ||
                    (habit.isNumerical && isTargetMet(habit, entry))
            }

            if (completed >= 5) { // Consistent if completed 5+ days in last week
                consistentHabits++
            }
        }

        return min(10.0, consistentHabits * 2.0) // Max 10 point consistency bonus
    }

    private fun getStreakBonus(habits: HabitList, timestamp: Timestamp): Double {
        // Bonus for active streaks
        var totalStreakBonus = 0.0

        for (i in 0 until habits.size()) {
            val habit = habits.getByPosition(i)
            val streaks = habit.streaks.getBest(1)

            if (streaks.isNotEmpty() && streaks[0].end >= timestamp) {
                val streakLength = streaks[0].length
                when {
                    streakLength >= 30 -> totalStreakBonus += 3.0 // Long streak bonus
                    streakLength >= 7 -> totalStreakBonus += 1.5 // Week streak bonus
                    streakLength >= 3 -> totalStreakBonus += 0.5 // Short streak bonus
                }
            }
        }

        return min(15.0, totalStreakBonus) // Max 15 point streak bonus
    }

    /**
     * Get priority distribution for current habits
     */
    fun getPriorityDistribution(): Map<HabitPriority, Int> {
        val activeHabits = habitList.getFiltered(
            HabitMatcher(isArchivedAllowed = false)
        )

        val distribution = mutableMapOf<HabitPriority, Int>()
        HabitPriority.values().forEach { priority ->
            distribution[priority] = 0
        }

        for (i in 0 until activeHabits.size()) {
            val habit = activeHabits.getByPosition(i)
            val habitPriority = try {
                habit.priority ?: HabitPriority.NORMAL
            } catch (e: Exception) {
                HabitPriority.NORMAL
            }
            distribution[habitPriority] = distribution[habitPriority]!! + 1
        }

        return distribution
    }

    /**
     * Calculate total weight of all active habits
     */
    fun getTotalHabitWeight(): Double {
        val activeHabits = habitList.getFiltered(
            HabitMatcher(isArchivedAllowed = false)
        )

        var totalWeight = 0.0
        for (i in 0 until activeHabits.size()) {
            val habit = activeHabits.getByPosition(i)
            val habitWeight = try {
                habit.priority?.weight ?: 1.0
            } catch (e: Exception) {
                1.0 // Default weight if priority access fails
            }
            totalWeight += habitWeight
        }

        return totalWeight
    }
}
