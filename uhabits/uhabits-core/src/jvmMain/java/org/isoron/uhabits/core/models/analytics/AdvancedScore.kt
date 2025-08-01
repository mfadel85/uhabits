/*
 * Copyright (C) 2025 Enhanced by mfadel85
 * 
 * Advanced Analytics Extension for Loop Habit Tracker.
 * 
 * This file extends the original Loop Habit Tracker with advanced scoring
 * and analytics capabilities for better insights and BI integration.
 */

package org.isoron.uhabits.core.models.analytics

import org.isoron.uhabits.core.models.Habit
import org.isoron.uhabits.core.models.Timestamp
import kotlin.math.*

/**
 * Advanced scoring system that provides 100-point scale scoring
 * with enhanced analytics for habits.
 */
data class AdvancedScore(
    val timestamp: Timestamp,
    val dailyScore: Double,           // 0-100 daily performance
    val weeklyScore: Double,          // 0-100 weekly performance  
    val monthlyScore: Double,         // 0-100 monthly performance
    val trendDirection: TrendDirection, // UP, DOWN, STABLE
    val consistencyScore: Double,     // 0-100 consistency rating
    val velocityScore: Double,        // Rate of improvement/decline
    val streakBonus: Double,          // Bonus points for streaks
    val rawScore: Double             // Original 0-1 score from uHabits
) {
    
    enum class TrendDirection {
        UP, DOWN, STABLE
    }
    
    companion object {
        
        /**
         * Convert the original uHabits score (0-1) to enhanced 100-point system
         */
        fun fromOriginalScore(
            originalScore: Double,
            habit: Habit,
            timestamp: Timestamp,
            historicalScores: List<org.isoron.uhabits.core.models.Score> = emptyList()
        ): AdvancedScore {
            
            // Base score conversion (0-1 to 0-100)
            val baseScore = (originalScore * 100).coerceIn(0.0, 100.0)
            
            // Calculate enhanced metrics
            val consistencyScore = calculateConsistencyScore(historicalScores)
            val velocityScore = calculateVelocityScore(historicalScores)
            val trendDirection = calculateTrendDirection(historicalScores)
            val streakBonus = calculateStreakBonus(habit, timestamp)
            
            // Calculate weighted scores for different time periods
            val dailyScore = calculateDailyScore(baseScore, consistencyScore, streakBonus)
            val weeklyScore = calculateWeeklyScore(dailyScore, historicalScores, habit)
            val monthlyScore = calculateMonthlyScore(weeklyScore, historicalScores, habit)
            
            return AdvancedScore(
                timestamp = timestamp,
                dailyScore = dailyScore,
                weeklyScore = weeklyScore,
                monthlyScore = monthlyScore,
                trendDirection = trendDirection,
                consistencyScore = consistencyScore,
                velocityScore = velocityScore,
                streakBonus = streakBonus,
                rawScore = originalScore
            )
        }
        
        private fun calculateDailyScore(
            baseScore: Double, 
            consistencyScore: Double, 
            streakBonus: Double
        ): Double {
            // Weighted combination: 70% base score, 20% consistency, 10% streak bonus
            val weightedScore = (baseScore * 0.7) + (consistencyScore * 0.2) + (streakBonus * 0.1)
            return weightedScore.coerceIn(0.0, 100.0)
        }
        
        private fun calculateWeeklyScore(
            dailyScore: Double,
            historicalScores: List<org.isoron.uhabits.core.models.Score>,
            habit: Habit
        ): Double {
            if (historicalScores.size < 7) return dailyScore
            
            // Get last 7 days of scores
            val last7Days = historicalScores.takeLast(7)
            val averageScore = (last7Days.sumOf { it.value * 100 } / 7.0)
            
            // Factor in habit frequency requirements
            val frequencyMultiplier = when {
                habit.frequency.toDouble() >= 1.0 -> 1.0  // Daily habits
                habit.frequency.toDouble() >= 0.5 -> 1.1  // 3-4x per week
                else -> 1.2  // Less frequent habits get bonus
            }
            
            val weeklyScore = averageScore * frequencyMultiplier
            return weeklyScore.coerceIn(0.0, 100.0)
        }
        
        private fun calculateMonthlyScore(
            weeklyScore: Double,
            historicalScores: List<org.isoron.uhabits.core.models.Score>,
            habit: Habit
        ): Double {
            if (historicalScores.size < 30) return weeklyScore
            
            // Get last 30 days of scores
            val last30Days = historicalScores.takeLast(30)
            val averageScore = (last30Days.sumOf { it.value * 100 } / 30.0)
            
            // Add maturity bonus for established habits
            val maturityBonus = min(5.0, historicalScores.size / 30.0)
            
            val monthlyScore = averageScore + maturityBonus
            return monthlyScore.coerceIn(0.0, 100.0)
        }
        
        private fun calculateConsistencyScore(
            historicalScores: List<org.isoron.uhabits.core.models.Score>
        ): Double {
            if (historicalScores.size < 7) return 50.0
            
            val last7Days = historicalScores.takeLast(7)
            val scores = last7Days.map { it.value * 100 }
            
            // Calculate standard deviation
            val mean = scores.average()
            val variance = scores.map { (it - mean).pow(2) }.average()
            val stdDev = sqrt(variance)
            
            // Lower standard deviation = higher consistency
            // Map std dev (0-50) to consistency score (100-0)
            val consistencyScore = 100.0 - (stdDev * 2).coerceIn(0.0, 100.0)
            return consistencyScore
        }
        
        private fun calculateVelocityScore(
            historicalScores: List<org.isoron.uhabits.core.models.Score>
        ): Double {
            if (historicalScores.size < 14) return 0.0
            
            val recent7Days = historicalScores.takeLast(7).map { it.value * 100 }
            val previous7Days = historicalScores.dropLast(7).takeLast(7).map { it.value * 100 }
            
            val recentAvg = recent7Days.average()
            val previousAvg = previous7Days.average()
            
            // Calculate rate of change
            val velocity = recentAvg - previousAvg
            
            // Normalize to -50 to +50 range
            return velocity.coerceIn(-50.0, 50.0)
        }
        
        private fun calculateTrendDirection(
            historicalScores: List<org.isoron.uhabits.core.models.Score>
        ): TrendDirection {
            if (historicalScores.size < 7) return TrendDirection.STABLE
            
            val recent = historicalScores.takeLast(3).map { it.value * 100 }.average()
            val previous = historicalScores.dropLast(3).takeLast(3).map { it.value * 100 }.average()
            
            return when {
                recent > previous + 5 -> TrendDirection.UP
                recent < previous - 5 -> TrendDirection.DOWN
                else -> TrendDirection.STABLE
            }
        }
        
        private fun calculateStreakBonus(habit: Habit, timestamp: Timestamp): Double {
            // Get current streak length
            val streakList = habit.streaks
            val currentStreak = streakList.getAll().firstOrNull()?.length ?: 0
            
            // Bonus points based on streak length
            return when {
                currentStreak >= 100 -> 20.0  // 100+ day streak
                currentStreak >= 50 -> 15.0   // 50+ day streak  
                currentStreak >= 30 -> 10.0   // 30+ day streak
                currentStreak >= 14 -> 5.0    // 14+ day streak
                currentStreak >= 7 -> 2.0     // 7+ day streak
                else -> 0.0
            }.coerceIn(0.0, 20.0)
        }
    }
}
