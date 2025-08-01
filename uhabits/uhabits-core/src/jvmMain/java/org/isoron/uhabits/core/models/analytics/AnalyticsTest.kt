/*
 * Copyright (C) 2025 Enhanced by mfadel85
 * 
 * Simple test file to validate Analytics Engine functionality
 */

package org.isoron.uhabits.core.models.analytics

import org.isoron.uhabits.core.models.Timestamp

/**
 * Simple validation test for Analytics Engine
 */
class AnalyticsTest {
    
    fun testAdvancedScoreCreation() {
        val timestamp = Timestamp.today()
        
        val score = AdvancedScore(
            timestamp = timestamp,
            dailyScore = 85.0,
            weeklyScore = 78.5,
            monthlyScore = 82.3,
            trendDirection = AdvancedScore.TrendDirection.UP,
            consistencyScore = 76.8,
            velocityScore = 12.5,
            streakBonus = 5.0,
            rawScore = 0.85
        )
        
        println("AdvancedScore created successfully:")
        println("Daily Score: ${score.dailyScore}")
        println("Trend: ${score.trendDirection}")
        println("Test passed!")
    }
    
    fun testAnalyticsDataStructures() {
        // Test analytics report structure
        val report = AnalyticsEngine.AnalyticsReport(
            overallScore = 82.5,
            trends = emptyList(),
            correlations = emptyList(),
            timePatterns = emptyList(),
            streakAnalysis = AnalyticsEngine.StreakAnalysis(
                longestCurrentStreak = 10,
                longestOverallStreak = 25,
                averageStreakLength = 8.3,
                streakRecoveryRate = 75.0,
                streakMaintainability = 82.0
            ),
            recommendations = emptyList(),
            weeklyBreakdown = emptyMap(),
            monthlyBreakdown = emptyMap(),
            performanceMetrics = AnalyticsEngine.PerformanceMetrics(
                averageScore = 82.5,
                medianScore = 85.0,
                consistency = 76.8,
                improvementRate = 8.2,
                volatility = 15.3
            ),
            bestPerformanceDayOfWeek = "Monday",
            worstPerformanceDayOfWeek = "Friday",
            period = "Last 30 days"
        )
        
        println("Analytics Report structure test passed!")
        println("Overall Score: ${report.overallScore}")
        println("Best Day: ${report.bestPerformanceDayOfWeek}")
    }
}
