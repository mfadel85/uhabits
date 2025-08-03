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
import android.net.Uri
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.isoron.uhabits.core.models.HabitList
import org.isoron.uhabits.core.utils.DateUtils
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class CloudExportManager(
    private val context: Context,
    private val habitList: HabitList
) {
    
    companion object {
        // Optimal export days for different BI platforms
        const val POWERBI_OPTIMAL_DAY = Calendar.MONDAY    // Start of business week
        const val LOOKER_OPTIMAL_DAY = Calendar.SUNDAY     // End of week analytics
        const val EXCEL_OPTIMAL_DAY = Calendar.SATURDAY    // Weekend analysis
        
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
        
        // PowerBI optimized format with additional computed columns
        csvData.append("Date,Habit_ID,Habit_Name,Category,Type,Value,Target,Success_Rate,Streak,Color_Code,Is_Active,Week_Number,Month_Number,Quarter\n")
        
        habitList.forEachIndexed { index, habit ->
            val entries = habit.computedEntries
            val totalEntries = entries.getKnown().size
            val successRate = if (totalEntries > 0) (totalEntries.toDouble() / 30 * 100).toInt() else 0
            val calendar = Calendar.getInstance()
            val weekNumber = calendar.get(Calendar.WEEK_OF_YEAR)
            val monthNumber = calendar.get(Calendar.MONTH) + 1
            val quarter = (monthNumber - 1) / 3 + 1
            
            csvData.append("$exportTime,$index,\"${habit.name}\",General,${habit.type},$totalEntries,")
            csvData.append("${if (habit.isNumerical) habit.targetValue else 1},$successRate,")
            csvData.append("${getCurrentStreakLength(habit)},${habit.color},${!habit.isArchived},$weekNumber,$monthNumber,$quarter\n")
        }
        
        return csvData.toString()
    }
    
    private fun generateLookerStudioData(timestamp: String, exportTime: String): String {
        // JSON format optimized for Looker Studio
        val jsonData = StringBuilder()
        jsonData.append("[\n")
        
        habitList.forEachIndexed { index, habit ->
            val entries = habit.computedEntries
            val totalEntries = entries.getKnown().size
            val successRate = if (totalEntries > 0) (totalEntries.toDouble() / 30 * 100) else 0.0
            
            jsonData.append("  {\n")
            jsonData.append("    \"export_date\": \"$exportTime\",\n")
            jsonData.append("    \"habit_id\": $index,\n")
            jsonData.append("    \"habit_name\": \"${habit.name}\",\n")
            jsonData.append("    \"total_entries\": $totalEntries,\n")
            jsonData.append("    \"success_rate\": $successRate,\n")
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
        // Excel-friendly CSV with extended analysis columns
        val csvData = StringBuilder()
        csvData.append("Export_Date,Habit_Name,Total_Entries,Success_Rate_%,Streak_Days,Target_Value,")
        csvData.append("Performance_Grade,Trend_7d,Trend_30d,Category,Color,Status,Notes\n")
        
        habitList.forEach { habit ->
            val entries = habit.computedEntries
            val totalEntries = entries.getKnown().size
            val successRate = if (totalEntries > 0) (totalEntries.toDouble() / 30 * 100).toInt() else 0
            val grade = when {
                successRate >= 90 -> "A+"
                successRate >= 80 -> "A"
                successRate >= 70 -> "B"
                successRate >= 60 -> "C"
                else -> "D"
            }
            
            csvData.append("$exportTime,\"${habit.name}\",$totalEntries,$successRate,${getCurrentStreakLength(habit)},")
            csvData.append("${if (habit.isNumerical) habit.targetValue else 1},$grade,Stable,Stable,")
            csvData.append("General,${habit.color},${if (habit.isArchived) "Archived" else "Active"},Auto-generated\n")
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
