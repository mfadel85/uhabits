/*
 * Copyright (C) 2025 Enhanced by mfadel85
 *
 * Cloud Export Manager for uHabits Analytics
 *
 * Provides automated cloud storage integration for PowerBI and Looker Studio
 */

package org.isoron.uhabits.activities.analytics

import android.content.Context
import android.content.Intent
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.isoron.uhabits.core.models.HabitList
import org.isoron.uhabits.core.models.HabitPriority
import org.isoron.uhabits.core.utils.DateUtils
import java.text.SimpleDateFormat
import java.util.*

class CloudExportManager(
    private val context: Context,
    private val habitList: HabitList
) {

    companion object {
        // Optimal export days for different BI platforms
        const val POWERBI_OPTIMAL_DAY = Calendar.MONDAY // Start of business week
        const val LOOKER_OPTIMAL_DAY = Calendar.SUNDAY // End of week analytics
        const val EXCEL_OPTIMAL_DAY = Calendar.SATURDAY // Weekend analysis

        // Cloud service identifiers
        const val SERVICE_ONEDRIVE = "onedrive"
        const val SERVICE_GOOGLE_DRIVE = "googledrive"
        const val SERVICE_DROPBOX = "dropbox"
        const val SERVICE_SHAREPOINT = "sharepoint"
    }

    /**
     * Get the optimal export day recommendation based on current date and BI platform
     */
    fun getOptimalExportRecommendation(): String {
        val calendar = Calendar.getInstance()
        val currentDay = calendar.get(Calendar.DAY_OF_WEEK)
        val dayFormatter = SimpleDateFormat("EEEE", Locale.getDefault())

        return when (currentDay) {
            Calendar.SUNDAY -> {
                "🌟 PERFECT DAY for Looker Studio!\n" +
                    "✅ Export now for weekly analytics review\n" +
                    "📊 Weekend data is complete for analysis"
            }
            Calendar.MONDAY -> {
                "🌟 PERFECT DAY for PowerBI!\n" +
                    "✅ Export now for business week planning\n" +
                    "📈 Fresh start with complete weekend data"
            }
            Calendar.SATURDAY -> {
                "🌟 PERFECT DAY for Excel Analysis!\n" +
                    "✅ Export now for weekend deep-dive\n" +
                    "📋 Complete work week data available"
            }
            Calendar.FRIDAY -> {
                "⏰ Good day for export!\n" +
                    "💡 Consider waiting until Monday for PowerBI\n" +
                    "🎯 Or export now for weekend analysis"
            }
            else -> {
                val nextOptimalDay = when {
                    currentDay < Calendar.SATURDAY -> "Saturday (Excel) or Sunday (Looker)"
                    else -> "Monday (PowerBI)"
                }
                "📅 Current day: ${dayFormatter.format(calendar.time)}\n" +
                    "⏰ Next optimal export day: $nextOptimalDay\n" +
                    "💡 You can export anytime, but these days give best insights"
            }
        }
    }

    /**
     * Export to cloud storage with BI platform optimization
     */
    fun exportToCloud(
        platform: String, // "powerbi", "looker", "excel"
        cloudService: String, // SERVICE_* constants
        includeScheduling: Boolean = true
    ) {
        Toast.makeText(context, "🌤️ Preparing cloud export for $platform...", Toast.LENGTH_SHORT).show()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val exportData = generateExportData(platform)
                val fileName = generateFileName(platform)

                withContext(Dispatchers.Main) {
                    when (cloudService) {
                        SERVICE_ONEDRIVE -> shareToOneDrive(exportData, fileName, platform)
                        SERVICE_GOOGLE_DRIVE -> shareToGoogleDrive(exportData, fileName, platform)
                        SERVICE_DROPBOX -> shareToDropbox(exportData, fileName, platform)
                        SERVICE_SHAREPOINT -> shareToSharePoint(exportData, fileName, platform)
                        else -> shareToGenericCloud(exportData, fileName, platform)
                    }
                }

                if (includeScheduling) {
                    withContext(Dispatchers.Main) {
                        showSchedulingRecommendation(platform)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "❌ Export error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    /**
     * Get the current streak length for a habit (most recent streak)
     */
    private fun getCurrentStreakLength(habit: org.isoron.uhabits.core.models.Habit): Int {
        val bestStreaks = habit.streaks.getBest(1)
        return if (bestStreaks.isNotEmpty()) bestStreaks[0].length else 0
    }

    /**
     * Calculate actual success rate based on habit frequency and completion rate
     * Much more accurate than simple entry count / 30 days
     */
    private fun calculateActualSuccessRate(habit: org.isoron.uhabits.core.models.Habit): Double {
        val today = org.isoron.uhabits.core.utils.DateUtils.getTodayWithOffset()
        val thirtyDaysAgo = today.minus(29) // Last 30 days including today
        
        // Get entries for the last 30 days
        val entries = habit.computedEntries.getByInterval(thirtyDaysAgo, today)
        
        if (entries.isEmpty()) return 0.0
        
        // Calculate expected completions based on frequency
        val frequency = habit.frequency
        val expectedCompletions = when {
            frequency.numerator == 1 && frequency.denominator == 1 -> 30.0 // Daily
            frequency.numerator == 1 && frequency.denominator == 7 -> 30.0 / 7.0 // Weekly
            frequency.numerator == 1 && frequency.denominator == 30 -> 1.0 // Monthly
            else -> (30.0 * frequency.numerator) / frequency.denominator // Custom frequency
        }
        
        // Count actual completions
        val actualCompletions = entries.count { entry ->
            if (habit.isNumerical) {
                // For numerical habits, check if target was met
                val value = entry.value / 1000.0
                when (habit.targetType) {
                    org.isoron.uhabits.core.models.NumericalHabitType.AT_LEAST -> value >= habit.targetValue
                    org.isoron.uhabits.core.models.NumericalHabitType.AT_MOST -> value <= habit.targetValue
                }
            } else {
                // For boolean habits, check if completed
                entry.value == org.isoron.uhabits.core.models.Entry.YES_MANUAL || 
                entry.value == org.isoron.uhabits.core.models.Entry.YES_AUTO
            }
        }.toDouble()
        
        // Calculate success rate as percentage
        return if (expectedCompletions > 0) {
            kotlin.math.min(100.0, (actualCompletions / expectedCompletions) * 100.0)
        } else {
            0.0
        }
    }

    /**
     * Get performance grade based on success rate
     */
    private fun getPerformanceGrade(successRate: Double): String {
        return when {
            successRate >= 95 -> "A+"
            successRate >= 90 -> "A"
            successRate >= 85 -> "A-"
            successRate >= 80 -> "B+"
            successRate >= 75 -> "B"
            successRate >= 70 -> "B-"
            successRate >= 65 -> "C+"
            successRate >= 60 -> "C"
            successRate >= 55 -> "C-"
            successRate >= 50 -> "D+"
            successRate >= 40 -> "D"
            else -> "F"
        }
    }

    /**
     * Get weighted performance grade considering priority impact
     */
    private fun getWeightedPerformanceGrade(weightedScore: Double, priority: HabitPriority): String {
        // Adjust thresholds based on priority level
        val adjustedScore = when (priority) {
            HabitPriority.CRITICAL -> weightedScore / priority.weight // More stringent for critical habits
            HabitPriority.HIGH -> weightedScore / priority.weight * 0.9 // Slightly more stringent
            HabitPriority.NORMAL -> weightedScore / priority.weight
            HabitPriority.LOW -> weightedScore / priority.weight * 1.1 // More lenient for low priority
        }
        
        return getPerformanceGrade(adjustedScore)
    }

    private fun generateExportData(platform: String): String {
        val timestamp = DateUtils.getToday().toString()
        val timeFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault())
        val exportTime = timeFormat.format(Date())

        return when (platform) {
            "powerbi" -> generatePowerBIData(timestamp, exportTime)
            "looker" -> generateLookerStudioData(timestamp, exportTime)
            "excel" -> generateExcelData(timestamp, exportTime)
            else -> generateGenericData(timestamp, exportTime)
        }
    }

    private fun generatePowerBIData(timestamp: String, exportTime: String): String {
        val csvData = StringBuilder()

        // PowerBI optimized format with weighted performance calculations
        csvData.append("Date,Habit_ID,Habit_Name,Category,Type,Value,Target,Success_Rate,Weighted_Success_Rate,Streak,Priority,Weight,Color_Code,Is_Active,Week_Number,Month_Number,Quarter\n")

        habitList.forEachIndexed { index, habit ->
            val entries = habit.computedEntries
            val totalEntries = entries.getKnown().size
            
            // Calculate actual success rate based on habit frequency and completion
            val actualSuccessRate = calculateActualSuccessRate(habit)
            
            // Calculate weighted success rate using priority
            val priority = habit.priority ?: HabitPriority.NORMAL
            val weightedSuccessRate = actualSuccessRate * priority.weight
            
            val calendar = Calendar.getInstance()
            val weekNumber = calendar.get(Calendar.WEEK_OF_YEAR)
            val monthNumber = calendar.get(Calendar.MONTH) + 1
            val quarter = (monthNumber - 1) / 3 + 1

            csvData.append("$exportTime,$index,\"${habit.name}\",${priority.displayName},${habit.type},$totalEntries,")
            csvData.append("${if (habit.isNumerical) habit.targetValue else 1},${"%.1f".format(actualSuccessRate)},")
            csvData.append("${"%.1f".format(weightedSuccessRate)},${getCurrentStreakLength(habit)},")
            csvData.append("${priority.displayName},${priority.weight},${habit.color},${!habit.isArchived},$weekNumber,$monthNumber,$quarter\n")
        }

        return csvData.toString()
    }

    private fun generateLookerStudioData(timestamp: String, exportTime: String): String {
        // JSON format optimized for Looker Studio with weighted analytics
        val jsonData = StringBuilder()
        jsonData.append("[\n")

        habitList.forEachIndexed { index, habit ->
            val entries = habit.computedEntries
            val totalEntries = entries.getKnown().size
            
            // Calculate actual and weighted success rates
            val actualSuccessRate = calculateActualSuccessRate(habit)
            val priority = habit.priority ?: HabitPriority.NORMAL
            val weightedSuccessRate = actualSuccessRate * priority.weight

            jsonData.append("  {\n")
            jsonData.append("    \"export_date\": \"$exportTime\",\n")
            jsonData.append("    \"habit_id\": $index,\n")
            jsonData.append("    \"habit_name\": \"${habit.name}\",\n")
            jsonData.append("    \"total_entries\": $totalEntries,\n")
            jsonData.append("    \"success_rate\": $actualSuccessRate,\n")
            jsonData.append("    \"weighted_success_rate\": $weightedSuccessRate,\n")
            jsonData.append("    \"priority\": \"${priority.displayName}\",\n")
            jsonData.append("    \"priority_weight\": ${priority.weight},\n")
            jsonData.append("    \"priority_icon\": \"${priority.icon}\",\n")
            jsonData.append("    \"streak_length\": ${getCurrentStreakLength(habit)},\n")
            jsonData.append("    \"is_numerical\": ${habit.isNumerical},\n")
            jsonData.append("    \"target_value\": ${if (habit.isNumerical) habit.targetValue else 1},\n")
            jsonData.append("    \"color_code\": \"${habit.color}\",\n")
            jsonData.append("    \"is_active\": ${!habit.isArchived}\n")
            jsonData.append("  }")
            if (index < habitList.size() - 1) jsonData.append(",")
            jsonData.append("\n")
        }

        jsonData.append("]")
        return jsonData.toString()
    }

    private fun generateExcelData(timestamp: String, exportTime: String): String {
        // Excel-friendly CSV with weighted analysis and priority-aware grading
        val csvData = StringBuilder()
        csvData.append("Export_Date,Habit_Name,Priority,Weight,Success_Rate_%,Weighted_Success_Rate_%,Streak_Days,Target_Value,")
        csvData.append("Performance_Grade,Weighted_Grade,Priority_Impact,Trend_7d,Trend_30d,Category,Color,Status,Notes\n")

        habitList.forEach { habit ->
            val actualSuccessRate = calculateActualSuccessRate(habit)
            val priority = habit.priority ?: HabitPriority.NORMAL
            val weightedSuccessRate = actualSuccessRate * priority.weight
            
            // Priority-aware grading system
            val grade = getPerformanceGrade(actualSuccessRate)
            val weightedGrade = getWeightedPerformanceGrade(weightedSuccessRate, priority)
            
            // Calculate priority impact score
            val priorityImpact = when (priority) {
                HabitPriority.CRITICAL -> "🔴 High Impact"
                HabitPriority.HIGH -> "🟡 Medium Impact"
                HabitPriority.NORMAL -> "🟢 Normal Impact"
                HabitPriority.LOW -> "🔵 Low Impact"
            }

            csvData.append("$exportTime,\"${habit.name}\",${priority.displayName},${priority.weight},${"%.1f".format(actualSuccessRate)},")
            csvData.append("${"%.1f".format(weightedSuccessRate)},${getCurrentStreakLength(habit)},")
            csvData.append("${if (habit.isNumerical) habit.targetValue else 1},$grade,$weightedGrade,$priorityImpact,")
            csvData.append("Stable,Stable,${priority.displayName},${habit.color},")
            csvData.append("${if (habit.isArchived) "Archived" else "Active"},Priority: ${priority.description}\n")
        }

        return csvData.toString()
    }

    private fun generateGenericData(timestamp: String, exportTime: String): String {
        return generatePowerBIData(timestamp, exportTime) // Default to PowerBI format
    }

    private fun generateFileName(platform: String): String {
        val timeFormat = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault())
        val timestamp = timeFormat.format(Date())

        return when (platform) {
            "powerbi" -> "uHabits_PowerBI_$timestamp.csv"
            "looker" -> "uHabits_Looker_$timestamp.json"
            "excel" -> "uHabits_Excel_$timestamp.csv"
            else -> "uHabits_Export_$timestamp.csv"
        }
    }

    private fun shareToOneDrive(data: String, fileName: String, platform: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = if (platform == "looker") "application/json" else "text/csv"
            putExtra(Intent.EXTRA_TEXT, data)
            putExtra(Intent.EXTRA_SUBJECT, "uHabits Analytics Export for ${platform.uppercase()}")
            putExtra(Intent.EXTRA_TITLE, fileName)

            // Try OneDrive specifically
            setPackage("com.microsoft.skydrive")
        }

        try {
            context.startActivity(Intent.createChooser(shareIntent, "📁 Save to OneDrive for PowerBI"))
        } catch (e: Exception) {
            shareToGenericCloud(data, fileName, platform)
        }
    }

    private fun shareToGoogleDrive(data: String, fileName: String, platform: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = if (platform == "looker") "application/json" else "text/csv"
            putExtra(Intent.EXTRA_TEXT, data)
            putExtra(Intent.EXTRA_SUBJECT, "uHabits Analytics Export for ${platform.uppercase()}")
            putExtra(Intent.EXTRA_TITLE, fileName)

            // Try Google Drive specifically
            setPackage("com.google.android.apps.docs")
        }

        try {
            context.startActivity(Intent.createChooser(shareIntent, "📁 Save to Google Drive for Looker Studio"))
        } catch (e: Exception) {
            shareToGenericCloud(data, fileName, platform)
        }
    }

    private fun shareToDropbox(data: String, fileName: String, platform: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = if (platform == "looker") "application/json" else "text/csv"
            putExtra(Intent.EXTRA_TEXT, data)
            putExtra(Intent.EXTRA_SUBJECT, "uHabits Analytics Export for ${platform.uppercase()}")
            putExtra(Intent.EXTRA_TITLE, fileName)

            // Try Dropbox specifically
            setPackage("com.dropbox.android")
        }

        try {
            context.startActivity(Intent.createChooser(shareIntent, "📁 Save to Dropbox"))
        } catch (e: Exception) {
            shareToGenericCloud(data, fileName, platform)
        }
    }

    private fun shareToSharePoint(data: String, fileName: String, platform: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = if (platform == "looker") "application/json" else "text/csv"
            putExtra(Intent.EXTRA_TEXT, data)
            putExtra(Intent.EXTRA_SUBJECT, "uHabits Analytics Export for ${platform.uppercase()}")
            putExtra(Intent.EXTRA_TITLE, fileName)

            // Try SharePoint/Office apps
            setPackage("com.microsoft.office.outlook")
        }

        try {
            context.startActivity(Intent.createChooser(shareIntent, "📁 Save to SharePoint"))
        } catch (e: Exception) {
            shareToGenericCloud(data, fileName, platform)
        }
    }

    private fun shareToGenericCloud(data: String, fileName: String, platform: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = if (platform == "looker") "application/json" else "text/csv"
            putExtra(Intent.EXTRA_TEXT, data)
            putExtra(Intent.EXTRA_SUBJECT, "uHabits Analytics Export for ${platform.uppercase()}")
            putExtra(Intent.EXTRA_TITLE, fileName)
        }

        context.startActivity(Intent.createChooser(shareIntent, "🌤️ Choose Cloud Storage"))
    }

    private fun showSchedulingRecommendation(platform: String) {
        val recommendation = getOptimalExportRecommendation()
        val scheduleInfo = getScheduleInfo(platform)

        Toast.makeText(
            context,
            "📅 Export Complete!\n\n$recommendation\n\n$scheduleInfo",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun getScheduleInfo(platform: String): String {
        return when (platform) {
            "powerbi" -> """
                📊 PowerBI Schedule Tips:
                • Export Mondays for weekly business review
                • Set up auto-refresh in PowerBI service
                • Use OneDrive for seamless integration
            """.trimIndent()

            "looker" -> """
                📈 Looker Studio Tips:
                • Export Sundays for weekly insights
                • Use Google Drive for direct connection
                • JSON format enables advanced visualizations
            """.trimIndent()

            "excel" -> """
                📋 Excel Analysis Tips:
                • Export Saturdays for weekend analysis
                • Use Dropbox for team sharing
                • Enhanced columns for pivot tables
            """.trimIndent()

            else -> "💡 Regular exports help track progress trends!"
        }
    }
}
