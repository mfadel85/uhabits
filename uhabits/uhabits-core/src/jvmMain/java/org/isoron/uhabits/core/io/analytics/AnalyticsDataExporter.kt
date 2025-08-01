/*
 * Copyright (C) 2025 Enhanced by mfadel85
 * 
 * Advanced Data Export System for BI Integration
 * 
 * Provides structured data exports optimized for PowerBI, Looker Studio,
 * and other business intelligence platforms.
 */

package org.isoron.uhabits.core.io.analytics

import com.opencsv.CSVWriter
import org.isoron.uhabits.core.models.Habit
import org.isoron.uhabits.core.models.HabitList
import org.isoron.uhabits.core.models.HabitMatcher
import org.isoron.uhabits.core.models.Timestamp
import org.isoron.uhabits.core.models.analytics.AnalyticsEngine
import org.isoron.uhabits.core.models.analytics.AdvancedScore
import org.isoron.uhabits.core.utils.DateFormats
import org.isoron.uhabits.core.utils.DateUtils
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import java.io.File
import java.io.FileWriter
import java.io.Writer
import java.util.*
import java.util.Calendar

/**
 * Enhanced CSV exporter optimized for Business Intelligence platforms
 */
class AnalyticsDataExporter(
    private val habitList: HabitList,
    private val analyticsEngine: AnalyticsEngine,
    private val outputDir: File
) {
    
    private val dateFormat = DateFormats.getCSVDateFormat()
    
    /**
     * Export comprehensive analytics data in multiple formats
     */
    fun exportAnalyticsData(
        startDate: Timestamp = DateUtils.getToday().minus(365),
        endDate: Timestamp = DateUtils.getToday()
    ): AnalyticsExportResult {
        
        val outputFiles = mutableListOf<String>()
        
        // Generate analytics report
        val report = analyticsEngine.generateReport(startDate, endDate)
        
        // Export different data sets
        outputFiles.add(exportDailyScoreData(startDate, endDate))
        outputFiles.add(exportHabitMetadata())
        outputFiles.add(exportWeeklyPerformance(report.weeklyBreakdown))
        outputFiles.add(exportMonthlyPerformance(report.monthlyBreakdown))
        outputFiles.add(exportTrendAnalysis(report.trendAnalysis))
        outputFiles.add(exportCorrelationData(report.habitCorrelations))
        outputFiles.add(exportStreakAnalysis(report.streakAnalysis))
        outputFiles.add(exportRecommendations(report.recommendations))
        
        // Export JSON summary for modern BI tools
        outputFiles.add(exportJSONSummary(report))
        
        // Export PowerBI optimized dataset
        outputFiles.add(exportPowerBIDataset(startDate, endDate, report))
        
        return AnalyticsExportResult(
            success = true,
            exportedFiles = outputFiles,
            totalRecords = calculateTotalRecords(startDate, endDate),
            exportTimestamp = DateUtils.getToday()
        )
    }
    
    /**
     * Export daily scores in flat table format for BI tools
     */
    private fun exportDailyScoreData(startDate: Timestamp, endDate: Timestamp): String {
        val filename = "daily_scores.csv"
        val file = File(outputDir, filename)
        val writer = FileWriter(file)
        val csv = CSVWriter(writer)
        
        // Header row optimized for BI analysis
        csv.writeNext(arrayOf(
            "Date",
            "Habit_Name",
            "Habit_ID", 
            "Original_Score",        // 0-1 scale
            "Daily_Score_100",       // 0-100 scale
            "Weekly_Score_100",      // 0-100 scale  
            "Monthly_Score_100",     // 0-100 scale
            "Consistency_Score",     // 0-100 scale
            "Velocity_Score",        // Rate of change
            "Streak_Bonus",          // Bonus points
            "Trend_Direction",       // UP/DOWN/STABLE
            "Day_of_Week",
            "Day_of_Month", 
            "Month",
            "Year",
            "Week_Number",
            "Quarter",
            "Is_Weekend",
            "Is_Completed",
            "Entry_Value",           // Actual entry value
            "Entry_Notes",
            "Habit_Frequency_Numerator",
            "Habit_Frequency_Denominator",
            "Habit_Type",
            "Habit_Category",        // Could be added as custom field
            "Target_Value",
            "Unit"
        ), false)
        
        val habits = habitList.getFiltered(HabitMatcher(isArchivedAllowed = false))
        
        habits.forEach { habit ->
            val scores = habit.scores.getByInterval(startDate, endDate)
            val entries = habit.computedEntries.getByInterval(startDate, endDate)
            
            scores.forEach { score ->
                val timestamp = score.timestamp
                val entry = entries.find { it.timestamp == timestamp }
                
                // Create advanced score
                val advancedScore = AdvancedScore.fromOriginalScore(
                    originalScore = score.value,
                    habit = habit,
                    timestamp = timestamp,
                    historicalScores = scores
                )
                
                val calendar = timestamp.toCalendar()
                val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                val isWeekend = dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY
                
                csv.writeNext(arrayOf(
                    dateFormat.format(timestamp.toJavaDate()),
                    habit.name,
                    habit.id?.toString() ?: "",
                    "%.6f".format(Locale.US, score.value),
                    "%.2f".format(Locale.US, advancedScore.dailyScore),
                    "%.2f".format(Locale.US, advancedScore.weeklyScore),
                    "%.2f".format(Locale.US, advancedScore.monthlyScore),
                    "%.2f".format(Locale.US, advancedScore.consistencyScore),
                    "%.2f".format(Locale.US, advancedScore.velocityScore),
                    "%.2f".format(Locale.US, advancedScore.streakBonus),
                    advancedScore.trendDirection.name,
                    dayOfWeek.toString(),
                    calendar.get(Calendar.DAY_OF_MONTH).toString(),
                    (calendar.get(Calendar.MONTH) + 1).toString(),
                    calendar.get(Calendar.YEAR).toString(),
                    calendar.get(Calendar.WEEK_OF_YEAR).toString(),
                    ((calendar.get(Calendar.MONTH) / 3) + 1).toString(),
                    isWeekend.toString(),
                    entry?.let { it.value > 0 }?.toString() ?: "false",
                    entry?.formattedValue ?: "",
                    entry?.notes ?: "",
                    habit.frequency.numerator.toString(),
                    habit.frequency.denominator.toString(),
                    habit.type.name,
                    "", // Placeholder for category
                    if (habit.isNumerical) habit.targetValue.toString() else "",
                    habit.unit
                ), false)
            }
        }
        
        csv.close()
        writer.close()
        
        return filename
    }
    
    /**
     * Export habit metadata for dimension tables
     */
    private fun exportHabitMetadata(): String {
        val filename = "habit_metadata.csv"
        val file = File(outputDir, filename)
        val writer = FileWriter(file)
        val csv = CSVWriter(writer)
        
        csv.writeNext(arrayOf(
            "Habit_ID",
            "Habit_Name", 
            "Description",
            "Type",
            "Frequency_Numerator",
            "Frequency_Denominator",
            "Frequency_Description",
            "Color",
            "Unit",
            "Target_Type",
            "Target_Value",
            "Is_Archived",
            "Position",
            "Creation_Date",
            "Question",
            "Has_Reminder",
            "UUID"
        ), false)
        
        habitList.forEach { habit ->
            csv.writeNext(arrayOf(
                habit.id?.toString() ?: "",
                habit.name,
                habit.description,
                habit.type.name,
                habit.frequency.numerator.toString(),
                habit.frequency.denominator.toString(),
                when {
                    habit.frequency.numerator == 1 && habit.frequency.denominator == 1 -> "Daily"
                    habit.frequency.numerator == 1 && habit.frequency.denominator == 7 -> "Weekly"
                    habit.frequency.numerator == 1 && habit.frequency.denominator == 30 -> "Monthly"
                    else -> "${habit.frequency.numerator} times per ${habit.frequency.denominator} days"
                },
                habit.color.toCsvColor(),
                habit.unit,
                if (habit.isNumerical) habit.targetType.name else "",
                if (habit.isNumerical) habit.targetValue.toString() else "",
                habit.isArchived.toString(),
                habit.position.toString(),
                "", // Would need creation date tracking
                habit.question,
                habit.hasReminder().toString(),
                habit.uuid ?: ""
            ), false)
        }
        
        csv.close()
        writer.close()
        
        return filename
    }
    
    /**
     * Export weekly performance summary
     */
    private fun exportWeeklyPerformance(weeklyBreakdown: List<AnalyticsEngine.WeeklyPerformance>): String {
        val filename = "weekly_performance.csv"
        val file = File(outputDir, filename)
        val writer = FileWriter(file)
        val csv = CSVWriter(writer)
        
        csv.writeNext(arrayOf(
            "Week_Start_Date",
            "Week_End_Date",
            "Year",
            "Week_Number",
            "Overall_Score",
            "Best_Habit",
            "Worst_Habit",
            "Habit_Count",
            "Performance_Category"
        ), false)
        
        weeklyBreakdown.forEach { week ->
            val weekEnd = week.weekStartDate.plus(6)
            val calendar = week.weekStartDate.toCalendar()
            
            val performanceCategory = when {
                week.overallScore >= 80 -> "Excellent"
                week.overallScore >= 60 -> "Good"  
                week.overallScore >= 40 -> "Fair"
                else -> "Needs Improvement"
            }
            
            csv.writeNext(arrayOf(
                dateFormat.format(week.weekStartDate.toJavaDate()),
                dateFormat.format(weekEnd.toJavaDate()),
                calendar.get(Calendar.YEAR).toString(),
                calendar.get(Calendar.WEEK_OF_YEAR).toString(),
                "%.2f".format(Locale.US, week.overallScore),
                week.bestHabit,
                week.worstHabit,
                week.habitScores.size.toString(),
                performanceCategory
            ), false)
        }
        
        csv.close()
        writer.close()
        
        return filename
    }
    
    /**
     * Export monthly performance summary  
     */
    private fun exportMonthlyPerformance(monthlyBreakdown: List<AnalyticsEngine.MonthlyPerformance>): String {
        val filename = "monthly_performance.csv"
        val file = File(outputDir, filename)
        val writer = FileWriter(file)
        val csv = CSVWriter(writer)
        
        csv.writeNext(arrayOf(
            "Month_Start_Date",
            "Year",
            "Month",
            "Quarter",
            "Overall_Score", 
            "Consistency_Score",
            "Improvement_Rate",
            "Performance_Grade"
        ), false)
        
        monthlyBreakdown.forEach { month ->
            val calendar = month.monthStartDate.toCalendar()
            
            val grade = when {
                month.overallScore >= 90 -> "A+"
                month.overallScore >= 85 -> "A"
                month.overallScore >= 80 -> "A-"
                month.overallScore >= 75 -> "B+"
                month.overallScore >= 70 -> "B"
                month.overallScore >= 65 -> "B-"
                month.overallScore >= 60 -> "C+"
                month.overallScore >= 55 -> "C"
                month.overallScore >= 50 -> "C-"
                else -> "D"
            }
            
            csv.writeNext(arrayOf(
                dateFormat.format(month.monthStartDate.toJavaDate()),
                calendar.get(Calendar.YEAR).toString(),
                (calendar.get(Calendar.MONTH) + 1).toString(),
                ((calendar.get(Calendar.MONTH) / 3) + 1).toString(),
                "%.2f".format(Locale.US, month.overallScore),
                "%.2f".format(Locale.US, month.consistency),
                "%.2f".format(Locale.US, month.improvement),
                grade
            ), false)
        }
        
        csv.close()
        writer.close()
        
        return filename
    }
    
    /**
     * Export trend analysis data
     */
    private fun exportTrendAnalysis(trendAnalysis: AnalyticsEngine.TrendAnalysis): String {
        val filename = "trend_analysis.csv"
        val file = File(outputDir, filename)
        val writer = FileWriter(file)
        val csv = CSVWriter(writer)
        
        csv.writeNext(arrayOf(
            "Metric",
            "Value",
            "Description"
        ), false)
        
        csv.writeNext(arrayOf("Moving_Average_7_Days", "%.2f".format(Locale.US, trendAnalysis.movingAverage7Days), "7-day moving average performance"))
        csv.writeNext(arrayOf("Moving_Average_30_Days", "%.2f".format(Locale.US, trendAnalysis.movingAverage30Days), "30-day moving average performance"))
        csv.writeNext(arrayOf("Moving_Average_90_Days", "%.2f".format(Locale.US, trendAnalysis.movingAverage90Days), "90-day moving average performance"))
        csv.writeNext(arrayOf("Overall_Trend", trendAnalysis.overallTrend.name, "Overall trend direction"))
        csv.writeNext(arrayOf("Improvement_Rate", "%.2f".format(Locale.US, trendAnalysis.improvementRate), "Weekly improvement rate (%)"))
        csv.writeNext(arrayOf("Volatility", "%.2f".format(Locale.US, trendAnalysis.volatility), "Score consistency (0-100)"))
        
        csv.close()
        writer.close()
        
        return filename
    }
    
    /**
     * Export habit correlation data
     */
    private fun exportCorrelationData(correlations: List<AnalyticsEngine.HabitCorrelation>): String {
        val filename = "habit_correlations.csv"
        val file = File(outputDir, filename)
        val writer = FileWriter(file)
        val csv = CSVWriter(writer)
        
        csv.writeNext(arrayOf(
            "Habit_1",
            "Habit_2", 
            "Correlation_Strength",
            "Correlation_Type",
            "Significance_Level",
            "Description"
        ), false)
        
        correlations.forEach { correlation ->
            val type = when {
                correlation.correlationStrength > 0 -> "Positive"
                correlation.correlationStrength < 0 -> "Negative" 
                else -> "None"
            }
            
            val significance = when {
                kotlin.math.abs(correlation.correlationStrength) >= 0.7 -> "Strong"
                kotlin.math.abs(correlation.correlationStrength) >= 0.5 -> "Moderate"
                kotlin.math.abs(correlation.correlationStrength) >= 0.3 -> "Weak"
                else -> "Negligible"
            }
            
            csv.writeNext(arrayOf(
                correlation.habit1,
                correlation.habit2,
                "%.4f".format(Locale.US, correlation.correlationStrength),
                type,
                significance,
                correlation.description
            ), false)
        }
        
        csv.close()
        writer.close()
        
        return filename
    }
    
    /**
     * Export streak analysis
     */
    private fun exportStreakAnalysis(streakAnalysis: AnalyticsEngine.StreakAnalysis): String {
        val filename = "streak_analysis.csv"
        val file = File(outputDir, filename)
        val writer = FileWriter(file)
        val csv = CSVWriter(writer)
        
        csv.writeNext(arrayOf(
            "Metric",
            "Value",
            "Description"
        ), false)
        
        csv.writeNext(arrayOf("Longest_Current_Streak", streakAnalysis.longestCurrentStreak.toString(), "Longest active streak across all habits"))
        csv.writeNext(arrayOf("Longest_Overall_Streak", streakAnalysis.longestOverallStreak.toString(), "Best streak ever achieved"))
        csv.writeNext(arrayOf("Average_Streak_Length", "%.1f".format(Locale.US, streakAnalysis.averageStreakLength), "Average length of all streaks"))
        csv.writeNext(arrayOf("Streak_Recovery_Rate", "%.1f".format(Locale.US, streakAnalysis.streakRecoveryRate), "Percentage of successful streak recoveries"))
        csv.writeNext(arrayOf("Streak_Maintainability", "%.1f".format(Locale.US, streakAnalysis.streakMaintainability), "Ability to maintain long streaks"))
        
        csv.close()
        writer.close()
        
        return filename
    }
    
    /**
     * Export recommendations
     */
    private fun exportRecommendations(recommendations: List<AnalyticsEngine.Recommendation>): String {
        val filename = "recommendations.csv"
        val file = File(outputDir, filename)
        val writer = FileWriter(file)
        val csv = CSVWriter(writer)
        
        csv.writeNext(arrayOf(
            "Recommendation_Type",
            "Priority",
            "Habit_Name",
            "Message",
            "Action_Required"
        ), false)
        
        recommendations.forEach { recommendation ->
            val actionRequired = when (recommendation.type) {
                AnalyticsEngine.RecommendationType.FOCUS_HABIT -> "Increase attention and consistency"
                AnalyticsEngine.RecommendationType.REDUCE_HABIT -> "Consider reducing frequency or difficulty"
                AnalyticsEngine.RecommendationType.TIMING_OPTIMIZATION -> "Adjust timing for better performance"
                AnalyticsEngine.RecommendationType.STREAK_RECOVERY -> "Focus on rebuilding momentum"
                AnalyticsEngine.RecommendationType.CONSISTENCY_IMPROVEMENT -> "Work on maintaining regular schedule"
                AnalyticsEngine.RecommendationType.GOAL_ADJUSTMENT -> "Consider increasing challenge level"
            }
            
            csv.writeNext(arrayOf(
                recommendation.type.name,
                recommendation.priority.name,
                recommendation.habitName ?: "",
                recommendation.message,
                actionRequired
            ), false)
        }
        
        csv.close()
        writer.close()
        
        return filename
    }
    
    /**
     * Export JSON summary for modern BI tools
     */
    private fun exportJSONSummary(report: AnalyticsEngine.AnalyticsReport): String {
        val filename = "analytics_summary.json"
        val file = File(outputDir, filename)
        
        val json = Json {
            prettyPrint = true
            ignoreUnknownKeys = true
        }
        
        @Serializable
        data class JSONSummary(
            val exportDate: String,
            val overallScore: Double,
            val trendDirection: String,
            val totalHabits: Int,
            val activeStreaks: Int,
            val topCorrelations: List<String>,
            val keyRecommendations: List<String>,
            val performanceGrade: String
        )
        
        val grade = when {
            report.overallScore >= 90 -> "Excellent"
            report.overallScore >= 75 -> "Good"
            report.overallScore >= 60 -> "Fair"
            else -> "Needs Improvement"
        }
        
        val summary = JSONSummary(
            exportDate = dateFormat.format(DateUtils.getToday().toJavaDate()),
            overallScore = report.overallScore,
            trendDirection = report.trendAnalysis.overallTrend.name,
            totalHabits = habitList.size(),
            activeStreaks = report.streakAnalysis.longestCurrentStreak,
            topCorrelations = report.habitCorrelations.take(3).map { "${it.habit1} ↔ ${it.habit2}" },
            keyRecommendations = report.recommendations.take(5).map { it.message },
            performanceGrade = grade
        )
        
        file.writeText(json.encodeToString(summary))
        
        return filename
    }
    
    /**
     * Export PowerBI optimized dataset 
     */
    private fun exportPowerBIDataset(
        startDate: Timestamp, 
        endDate: Timestamp,
        report: AnalyticsEngine.AnalyticsReport
    ): String {
        val filename = "powerbi_dataset.csv"
        val file = File(outputDir, filename)
        val writer = FileWriter(file)
        val csv = CSVWriter(writer)
        
        // Optimized header for PowerBI
        csv.writeNext(arrayOf(
            "Date",
            "Habit",
            "Score",
            "ScoreCategory", 
            "Trend",
            "WeekOfYear",
            "Month",
            "Quarter",
            "Year",
            "IsWeekend",
            "DayOfWeek",
            "HabitType",
            "HabitFrequency"
        ), false)
        
        val habits = habitList.getFiltered(HabitMatcher(isArchivedAllowed = false))
        
        habits.forEach { habit ->
            val scores = habit.scores.getByInterval(startDate, endDate)
            
            scores.forEach { score ->
                val timestamp = score.timestamp
                val calendar = timestamp.toCalendar()
                val scoreValue = score.value * 100
                
                val scoreCategory = when {
                    scoreValue >= 80 -> "Excellent"
                    scoreValue >= 60 -> "Good"
                    scoreValue >= 40 -> "Fair"
                    else -> "Poor"
                }
                
                val isWeekend = calendar.get(Calendar.DAY_OF_WEEK) in listOf(Calendar.SATURDAY, Calendar.SUNDAY)
                val dayOfWeek = when (calendar.get(Calendar.DAY_OF_WEEK)) {
                    Calendar.SUNDAY -> "Sunday"
                    Calendar.MONDAY -> "Monday"
                    Calendar.TUESDAY -> "Tuesday"
                    Calendar.WEDNESDAY -> "Wednesday"
                    Calendar.THURSDAY -> "Thursday"
                    Calendar.FRIDAY -> "Friday"
                    Calendar.SATURDAY -> "Saturday"
                    else -> "Unknown"
                }
                
                csv.writeNext(arrayOf(
                    dateFormat.format(timestamp.toJavaDate()),
                    habit.name,
                    "%.2f".format(Locale.US, scoreValue),
                    scoreCategory,
                    report.trendAnalysis.overallTrend.name,
                    calendar.get(Calendar.WEEK_OF_YEAR).toString(),
                    (calendar.get(Calendar.MONTH) + 1).toString(),
                    ((calendar.get(Calendar.MONTH) / 3) + 1).toString(),
                    calendar.get(Calendar.YEAR).toString(),
                    isWeekend.toString(),
                    dayOfWeek,
                    habit.type.name,
                    when {
                        habit.frequency.denominator == 1 -> "Daily"
                        habit.frequency.denominator == 7 -> "Weekly"
                        habit.frequency.denominator == 30 -> "Monthly"
                        else -> "Custom"
                    }
                ), false)
            }
        }
        
        csv.close()
        writer.close()
        
        return filename
    }
    
    private fun calculateTotalRecords(startDate: Timestamp, endDate: Timestamp): Int {
        val habits = habitList.getFiltered(HabitMatcher(isArchivedAllowed = false))
        val days = startDate.daysUntil(endDate) + 1
        return habits.size * days
    }
    
    data class AnalyticsExportResult(
        val success: Boolean,
        val exportedFiles: List<String>,
        val totalRecords: Int,
        val exportTimestamp: Timestamp
    )
}
