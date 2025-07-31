/*
 * Copyright (C) 2025 Enhanced by mfadel85
 * 
 * Advanced Analytics Engine for Loop Habit Tracker.
 * 
 * Provides comprehensive analytics, insights, and trend analysis
 * for habit tracking data.
 */

package org.isoron.uhabits.core.models.analytics

import org.isoron.uhabits.core.models.Habit
import org.isoron.uhabits.core.models.HabitList
import org.isoron.uhabits.core.models.Timestamp
import org.isoron.uhabits.core.utils.DateUtils
import kotlin.math.*

/**
 * Comprehensive analytics engine for habit data analysis
 */
class AnalyticsEngine(
    private val habitList: HabitList
) {
    
    /**
     * Complete analytics report for a specific time period
     */
    data class AnalyticsReport(
        val overallScore: Double,           // 0-100 overall performance
        val trendAnalysis: TrendAnalysis,
        val habitCorrelations: List<HabitCorrelation>,
        val timePatterns: TimePatternAnalysis,
        val streakAnalysis: StreakAnalysis,
        val recommendations: List<Recommendation>,
        val weeklyBreakdown: List<WeeklyPerformance>,
        val monthlyBreakdown: List<MonthlyPerformance>
    )
    
    data class TrendAnalysis(
        val movingAverage7Days: Double,
        val movingAverage30Days: Double,
        val movingAverage90Days: Double,
        val overallTrend: AdvancedScore.TrendDirection,
        val improvementRate: Double,        // % improvement per week
        val volatility: Double              // Score consistency (0-100)
    )
    
    data class HabitCorrelation(
        val habit1: String,
        val habit2: String,
        val correlationStrength: Double,    // -1 to 1
        val description: String
    )
    
    data class TimePatternAnalysis(
        val bestPerformanceDayOfWeek: String,
        val worstPerformanceDayOfWeek: String,
        val bestPerformanceHour: Int?,
        val seasonalPatterns: Map<String, Double>,
        val weekendVsWeekdayScore: Pair<Double, Double>
    )
    
    data class StreakAnalysis(
        val longestCurrentStreak: Int,
        val longestOverallStreak: Int,
        val averageStreakLength: Double,
        val streakRecoveryRate: Double,     // % of times user recovered after breaking streak
        val streakMaintainability: Double   // How well user maintains long streaks
    )
    
    data class Recommendation(
        val type: RecommendationType,
        val message: String,
        val priority: Priority,
        val habitName: String?
    )
    
    enum class RecommendationType {
        FOCUS_HABIT, REDUCE_HABIT, TIMING_OPTIMIZATION, 
        STREAK_RECOVERY, CONSISTENCY_IMPROVEMENT, GOAL_ADJUSTMENT
    }
    
    enum class Priority {
        HIGH, MEDIUM, LOW
    }
    
    data class WeeklyPerformance(
        val weekStartDate: Timestamp,
        val overallScore: Double,
        val habitScores: Map<String, Double>,
        val bestHabit: String,
        val worstHabit: String
    )
    
    data class MonthlyPerformance(
        val monthStartDate: Timestamp,
        val overallScore: Double,
        val habitScores: Map<String, Double>,
        val improvement: Double,            // vs previous month
        val consistency: Double
    )
    
    /**
     * Generate comprehensive analytics report
     */
    fun generateReport(
        startDate: Timestamp = DateUtils.getToday().minus(90),
        endDate: Timestamp = DateUtils.getToday()
    ): AnalyticsReport {
        
        val habits = habitList.getFiltered { !it.isArchived }
        
        return AnalyticsReport(
            overallScore = calculateOverallScore(habits, startDate, endDate),
            trendAnalysis = analyzeTrends(habits, startDate, endDate),
            habitCorrelations = analyzeCorrelations(habits, startDate, endDate),
            timePatterns = analyzeTimePatterns(habits, startDate, endDate),
            streakAnalysis = analyzeStreaks(habits),
            recommendations = generateRecommendations(habits, startDate, endDate),
            weeklyBreakdown = generateWeeklyBreakdown(habits, startDate, endDate),
            monthlyBreakdown = generateMonthlyBreakdown(habits, startDate, endDate)
        )
    }
    
    private fun calculateOverallScore(
        habits: List<Habit>,
        startDate: Timestamp,
        endDate: Timestamp
    ): Double {
        if (habits.isEmpty()) return 0.0
        
        val habitScores = habits.map { habit ->
            val scores = habit.scores.getByInterval(startDate, endDate)
            val avgScore = if (scores.isNotEmpty()) {
                scores.map { it.value * 100 }.average()
            } else 0.0
            
            // Weight by habit importance (could be user-defined, for now use frequency)
            val weight = habit.frequency.toDouble().coerceAtLeast(0.1)
            avgScore * weight
        }
        
        val totalWeight = habits.sumOf { it.frequency.toDouble().coerceAtLeast(0.1) }
        return (habitScores.sum() / totalWeight).coerceIn(0.0, 100.0)
    }
    
    private fun analyzeTrends(
        habits: List<Habit>,
        startDate: Timestamp,
        endDate: Timestamp
    ): TrendAnalysis {
        
        val allScores = mutableListOf<Double>()
        
        habits.forEach { habit ->
            val scores = habit.scores.getByInterval(startDate, endDate)
            allScores.addAll(scores.map { it.value * 100 })
        }
        
        if (allScores.isEmpty()) {
            return TrendAnalysis(0.0, 0.0, 0.0, AdvancedScore.TrendDirection.STABLE, 0.0, 0.0)
        }
        
        val movingAvg7 = calculateMovingAverage(allScores, 7)
        val movingAvg30 = calculateMovingAverage(allScores, 30)
        val movingAvg90 = calculateMovingAverage(allScores, 90)
        
        val trend = when {
            movingAvg7 > movingAvg30 + 5 -> AdvancedScore.TrendDirection.UP
            movingAvg7 < movingAvg30 - 5 -> AdvancedScore.TrendDirection.DOWN
            else -> AdvancedScore.TrendDirection.STABLE
        }
        
        val improvementRate = calculateImprovementRate(allScores)
        val volatility = calculateVolatility(allScores)
        
        return TrendAnalysis(
            movingAverage7Days = movingAvg7,
            movingAverage30Days = movingAvg30,
            movingAverage90Days = movingAvg90,
            overallTrend = trend,
            improvementRate = improvementRate,
            volatility = volatility
        )
    }
    
    private fun analyzeCorrelations(
        habits: List<Habit>,
        startDate: Timestamp,
        endDate: Timestamp
    ): List<HabitCorrelation> {
        
        val correlations = mutableListOf<HabitCorrelation>()
        
        // Calculate pairwise correlations
        for (i in habits.indices) {
            for (j in i + 1 until habits.size) {
                val habit1 = habits[i]
                val habit2 = habits[j]
                
                val correlation = calculateCorrelation(habit1, habit2, startDate, endDate)
                
                if (abs(correlation) > 0.3) { // Only show significant correlations
                    val description = when {
                        correlation > 0.7 -> "Strong positive correlation - these habits are often done together"
                        correlation > 0.3 -> "Moderate positive correlation - some tendency to do together"
                        correlation < -0.7 -> "Strong negative correlation - these habits rarely coincide"
                        correlation < -0.3 -> "Moderate negative correlation - some tendency to avoid doing together"
                        else -> "Weak correlation"
                    }
                    
                    correlations.add(
                        HabitCorrelation(
                            habit1 = habit1.name,
                            habit2 = habit2.name,
                            correlationStrength = correlation,
                            description = description
                        )
                    )
                }
            }
        }
        
        return correlations.sortedByDescending { abs(it.correlationStrength) }
    }
    
    private fun analyzeTimePatterns(
        habits: List<Habit>,
        startDate: Timestamp,
        endDate: Timestamp
    ): TimePatternAnalysis {
        
        val dayOfWeekScores = mutableMapOf<Int, MutableList<Double>>()
        val seasonalScores = mutableMapOf<String, MutableList<Double>>()
        
        // Analyze day-of-week patterns
        habits.forEach { habit ->
            val entries = habit.computedEntries.getByInterval(startDate, endDate)
            entries.forEach { entry ->
                val dayOfWeek = entry.timestamp.toCalendar().get(java.util.Calendar.DAY_OF_WEEK)
                val score = habit.scores[entry.timestamp].value * 100
                
                dayOfWeekScores.getOrPut(dayOfWeek) { mutableListOf() }.add(score)
                
                // Seasonal analysis (simplified)
                val month = entry.timestamp.toCalendar().get(java.util.Calendar.MONTH)
                val season = when (month) {
                    11, 0, 1 -> "Winter"
                    2, 3, 4 -> "Spring"
                    5, 6, 7 -> "Summer"
                    else -> "Fall"
                }
                seasonalScores.getOrPut(season) { mutableListOf() }.add(score)
            }
        }
        
        val dayNames = arrayOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
        val bestDay = dayOfWeekScores.maxByOrNull { it.value.average() }?.key?.let { dayNames[it - 1] } ?: "Unknown"
        val worstDay = dayOfWeekScores.minByOrNull { it.value.average() }?.key?.let { dayNames[it - 1] } ?: "Unknown"
        
        val weekdayScores = dayOfWeekScores.filterKeys { it in 2..6 }.values.flatten()
        val weekendScores = dayOfWeekScores.filterKeys { it == 1 || it == 7 }.values.flatten()
        
        val weekendAvg = if (weekendScores.isNotEmpty()) weekendScores.average() else 0.0
        val weekdayAvg = if (weekdayScores.isNotEmpty()) weekdayScores.average() else 0.0
        
        return TimePatternAnalysis(
            bestPerformanceDayOfWeek = bestDay,
            worstPerformanceDayOfWeek = worstDay,
            bestPerformanceHour = null, // Would need time-of-day data
            seasonalPatterns = seasonalScores.mapValues { it.value.average() },
            weekendVsWeekdayScore = Pair(weekendAvg, weekdayAvg)
        )
    }
    
    private fun analyzeStreaks(habits: List<Habit>): StreakAnalysis {
        var longestCurrentStreak = 0
        var longestOverallStreak = 0
        val allStreaks = mutableListOf<Int>()
        
        habits.forEach { habit ->
            val streaks = habit.streaks.getAll()
            if (streaks.isNotEmpty()) {
                val currentStreak = streaks.first().length
                val maxStreak = streaks.maxOf { it.length }
                
                longestCurrentStreak = max(longestCurrentStreak, currentStreak)
                longestOverallStreak = max(longestOverallStreak, maxStreak)
                
                allStreaks.addAll(streaks.map { it.length })
            }
        }
        
        val averageStreakLength = if (allStreaks.isNotEmpty()) allStreaks.average() else 0.0
        
        // Simple recovery rate calculation (would need more sophisticated logic)
        val streakRecoveryRate = 75.0 // Placeholder
        val streakMaintainability = when {
            averageStreakLength > 30 -> 90.0
            averageStreakLength > 14 -> 75.0
            averageStreakLength > 7 -> 60.0
            else -> 40.0
        }
        
        return StreakAnalysis(
            longestCurrentStreak = longestCurrentStreak,
            longestOverallStreak = longestOverallStreak,
            averageStreakLength = averageStreakLength,
            streakRecoveryRate = streakRecoveryRate,
            streakMaintainability = streakMaintainability
        )
    }
    
    private fun generateRecommendations(
        habits: List<Habit>,
        startDate: Timestamp,
        endDate: Timestamp
    ): List<Recommendation> {
        
        val recommendations = mutableListOf<Recommendation>()
        
        habits.forEach { habit ->
            val scores = habit.scores.getByInterval(startDate, endDate)
            if (scores.isNotEmpty()) {
                val avgScore = scores.map { it.value * 100 }.average()
                val recentScores = scores.takeLast(7).map { it.value * 100 }
                val recentAvg = recentScores.average()
                
                when {
                    avgScore < 40 -> {
                        recommendations.add(
                            Recommendation(
                                type = RecommendationType.FOCUS_HABIT,
                                message = "Consider focusing more attention on '${habit.name}' - current performance is ${avgScore.toInt()}%",
                                priority = Priority.HIGH,
                                habitName = habit.name
                            )
                        )
                    }
                    avgScore < 60 && recentAvg < avgScore - 10 -> {
                        recommendations.add(
                            Recommendation(
                                type = RecommendationType.STREAK_RECOVERY,
                                message = "'${habit.name}' performance has declined recently. Focus on rebuilding consistency.",
                                priority = Priority.MEDIUM,
                                habitName = habit.name
                            )
                        )
                    }
                    avgScore > 85 -> {
                        recommendations.add(
                            Recommendation(
                                type = RecommendationType.GOAL_ADJUSTMENT,
                                message = "'${habit.name}' is performing excellently (${avgScore.toInt()}%). Consider increasing the challenge.",
                                priority = Priority.LOW,
                                habitName = habit.name
                            )
                        )
                    }
                }
            }
        }
        
        return recommendations.sortedBy { it.priority }
    }
    
    private fun generateWeeklyBreakdown(
        habits: List<Habit>,
        startDate: Timestamp,
        endDate: Timestamp
    ): List<WeeklyPerformance> {
        
        val weeks = mutableListOf<WeeklyPerformance>()
        var currentWeekStart = startDate
        
        while (currentWeekStart.isSameOrBefore(endDate)) {
            val weekEnd = currentWeekStart.plus(6)
            
            val habitScores = habits.associate { habit ->
                val weekScores = habit.scores.getByInterval(currentWeekStart, weekEnd)
                val avgScore = if (weekScores.isNotEmpty()) {
                    weekScores.map { it.value * 100 }.average()
                } else 0.0
                
                habit.name to avgScore
            }
            
            val overallScore = if (habitScores.isNotEmpty()) {
                habitScores.values.average()
            } else 0.0
            
            val bestHabit = habitScores.maxByOrNull { it.value }?.key ?: "None"
            val worstHabit = habitScores.minByOrNull { it.value }?.key ?: "None"
            
            weeks.add(
                WeeklyPerformance(
                    weekStartDate = currentWeekStart,
                    overallScore = overallScore,
                    habitScores = habitScores,
                    bestHabit = bestHabit,
                    worstHabit = worstHabit
                )
            )
            
            currentWeekStart = currentWeekStart.plus(7)
        }
        
        return weeks
    }
    
    private fun generateMonthlyBreakdown(
        habits: List<Habit>,
        startDate: Timestamp,
        endDate: Timestamp
    ): List<MonthlyPerformance> {
        
        val months = mutableListOf<MonthlyPerformance>()
        var currentMonth = startDate
        
        while (currentMonth.isSameOrBefore(endDate)) {
            val monthEnd = currentMonth.plus(29) // Simplified month calculation
            
            val habitScores = habits.associate { habit ->
                val monthScores = habit.scores.getByInterval(currentMonth, monthEnd)
                val avgScore = if (monthScores.isNotEmpty()) {
                    monthScores.map { it.value * 100 }.average()
                } else 0.0
                
                habit.name to avgScore
            }
            
            val overallScore = if (habitScores.isNotEmpty()) {
                habitScores.values.average()
            } else 0.0
            
            // Calculate consistency for the month
            val consistency = calculateConsistencyForPeriod(habits, currentMonth, monthEnd)
            
            months.add(
                MonthlyPerformance(
                    monthStartDate = currentMonth,
                    overallScore = overallScore,
                    habitScores = habitScores,
                    improvement = 0.0, // Would calculate vs previous month
                    consistency = consistency
                )
            )
            
            currentMonth = currentMonth.plus(30)
        }
        
        return months
    }
    
    // Helper methods
    private fun calculateMovingAverage(scores: List<Double>, window: Int): Double {
        if (scores.size < window) return scores.average()
        return scores.takeLast(window).average()
    }
    
    private fun calculateImprovementRate(scores: List<Double>): Double {
        if (scores.size < 14) return 0.0
        
        val recent = scores.takeLast(7).average()
        val previous = scores.dropLast(7).takeLast(7).average()
        
        return ((recent - previous) / previous * 100).coerceIn(-100.0, 100.0)
    }
    
    private fun calculateVolatility(scores: List<Double>): Double {
        if (scores.isEmpty()) return 0.0
        
        val mean = scores.average()
        val variance = scores.map { (it - mean).pow(2) }.average()
        val stdDev = sqrt(variance)
        
        // Convert to 0-100 scale (lower volatility = higher score)
        return (100 - (stdDev * 2)).coerceIn(0.0, 100.0)
    }
    
    private fun calculateCorrelation(
        habit1: Habit,
        habit2: Habit,
        startDate: Timestamp,
        endDate: Timestamp
    ): Double {
        
        val entries1 = habit1.computedEntries.getByInterval(startDate, endDate)
        val entries2 = habit2.computedEntries.getByInterval(startDate, endDate)
        
        // Simple correlation calculation (would need more sophisticated implementation)
        val commonDays = entries1.map { it.timestamp }.intersect(entries2.map { it.timestamp }.toSet())
        
        if (commonDays.size < 7) return 0.0
        
        val values1 = commonDays.map { timestamp ->
            entries1.find { it.timestamp == timestamp }?.value?.toDouble() ?: 0.0
        }
        
        val values2 = commonDays.map { timestamp ->
            entries2.find { it.timestamp == timestamp }?.value?.toDouble() ?: 0.0
        }
        
        return calculatePearsonCorrelation(values1, values2)
    }
    
    private fun calculatePearsonCorrelation(x: List<Double>, y: List<Double>): Double {
        if (x.size != y.size || x.isEmpty()) return 0.0
        
        val n = x.size
        val sumX = x.sum()
        val sumY = y.sum()
        val sumXY = x.zip(y) { a, b -> a * b }.sum()
        val sumX2 = x.map { it * it }.sum()
        val sumY2 = y.map { it * it }.sum()
        
        val numerator = n * sumXY - sumX * sumY
        val denominator = sqrt((n * sumX2 - sumX * sumX) * (n * sumY2 - sumY * sumY))
        
        return if (denominator != 0.0) numerator / denominator else 0.0
    }
    
    private fun calculateConsistencyForPeriod(
        habits: List<Habit>,
        startDate: Timestamp,
        endDate: Timestamp
    ): Double {
        
        val allScores = mutableListOf<Double>()
        
        habits.forEach { habit ->
            val scores = habit.scores.getByInterval(startDate, endDate)
            allScores.addAll(scores.map { it.value * 100 })
        }
        
        return calculateVolatility(allScores)
    }
}
