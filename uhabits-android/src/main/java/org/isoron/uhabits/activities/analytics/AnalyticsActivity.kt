/*
 * Copyright (C) 2025 Enhanced by mfadel85
 *
 * Advanced Analytics Activity for uHabits
 *
 * Provides comprehensive analytics and data export functionality
 */

package org.isoron.uhabits.activities.analytics

import android.content.ContentValues
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.isoron.uhabits.HabitsApplication
import org.isoron.uhabits.core.models.HabitList
import org.isoron.uhabits.core.models.HabitPriority
import org.isoron.uhabits.core.utils.DateUtils
import org.isoron.uhabits.inject.HabitsApplicationComponent
import java.io.File
import java.io.OutputStreamWriter

class AnalyticsActivity : AppCompatActivity() {

    private lateinit var appComponent: HabitsApplicationComponent
    private lateinit var habitList: HabitList
    private val STORAGE_PERMISSION_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appComponent = (applicationContext as HabitsApplication).component
        habitList = appComponent.habitList

        setupUI()
    }

    private fun setupUI() {
        // Create layout programmatically since we don't have XML layout files
        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        // Title
        val titleText = TextView(this).apply {
            text = "Analytics & Data Export"
            textSize = 24f
            setPadding(0, 0, 0, 32)
        }
        rootLayout.addView(titleText)

        // Summary
        val summaryText = TextView(this).apply {
            text = """
                Export your habit data for analysis in business intelligence tools like:
                • PowerBI
                • Looker Studio
                • Excel
                • Custom analysis tools
                
                🎯 NEW: Priority-Weighted Analytics
                • ⭐ CRITICAL (4.0x): Core life habits & KPIs (Quran, Tasks, Health)
                • 🔥 HIGH (2.5x): Important daily habits (Exercise, Study, Work)
                • 📝 NORMAL (1.0x): Standard habits (Reading, Journaling)
                • 🌱 LOW (0.5x): Nice-to-have habits (Entertainment, Minor routines)
                
                Export includes:
                • Weighted success rates based on habit priority
                • Priority-aware performance grading
                • Individual and weighted analytics
                • Core KPI vs minor habit separation
                • Target values for numerical habits
                • Archive status and metadata
                • Timestamped export files
                
                📁 Smart Storage System:
                • Android 10+: Direct to Downloads folder (no permissions!)
                • Older Android: App storage (accessible via file manager)
                • Automatic fallback for compatibility
                
                ✅ Files will be easily accessible!
                📱 Modern Android: Downloads/uHabits_Analytics/
                📱 Older Android: File manager → Android/data/org.isoron.uhabits/
            """.trimIndent()
            setPadding(0, 0, 0, 32)
        }
        rootLayout.addView(summaryText)

        // Cloud Export Section
        val cloudTitle = TextView(this).apply {
            text = "🌤️ Cloud Export for BI Tools"
            textSize = 18f
            setPadding(0, 32, 0, 16)
        }
        rootLayout.addView(cloudTitle)

        // Priority Management Section
        val priorityTitle = TextView(this).apply {
            text = "⭐ Habit Priority Management"
            textSize = 18f
            setPadding(0, 32, 0, 16)
        }
        rootLayout.addView(priorityTitle)

        // Show current habit priorities
        val priorityStatsText = TextView(this).apply {
            text = getPriorityDistributionText()
            setPadding(16, 8, 16, 16)
            setBackgroundColor(0xFF4CAF50.toInt())
            setTextColor(0xFFFFFFFF.toInt())
        }
        rootLayout.addView(priorityStatsText)

        // Priority recommendation button
        val priorityRecommendButton = Button(this).apply {
            text = "🤖 Auto-Assign Smart Priorities"
            setOnClickListener { autoAssignPriorities() }
        }
        rootLayout.addView(priorityRecommendButton)

        // Optimal day recommendation
        val cloudExportManager = CloudExportManager(this, habitList)
        val dayRecommendation = TextView(this).apply {
            text = cloudExportManager.getOptimalExportRecommendation()
            setPadding(16, 8, 16, 16)
            setBackgroundColor(0xFF2196F3.toInt())
            setTextColor(0xFFFFFFFF.toInt())
        }
        rootLayout.addView(dayRecommendation)

        // PowerBI Cloud Export
        val exportPowerBIButton = Button(this).apply {
            text = "📊 Export to OneDrive (PowerBI)"
            setOnClickListener {
                CloudExportManager(this@AnalyticsActivity, habitList)
                    .exportToCloud("powerbi", CloudExportManager.SERVICE_ONEDRIVE)
            }
        }
        rootLayout.addView(exportPowerBIButton)

        // Looker Studio Cloud Export
        val exportLookerButton = Button(this).apply {
            text = "📈 Export to Google Drive (Looker)"
            setOnClickListener {
                CloudExportManager(this@AnalyticsActivity, habitList)
                    .exportToCloud("looker", CloudExportManager.SERVICE_GOOGLE_DRIVE)
            }
        }
        rootLayout.addView(exportLookerButton)

        // Excel Cloud Export
        val exportExcelButton = Button(this).apply {
            text = "📋 Export to Dropbox (Excel)"
            setOnClickListener {
                CloudExportManager(this@AnalyticsActivity, habitList)
                    .exportToCloud("excel", CloudExportManager.SERVICE_DROPBOX)
            }
        }
        rootLayout.addView(exportExcelButton)

        // Generic Cloud Export
        val exportCloudButton = Button(this).apply {
            text = "🌤️ Export to Any Cloud Service"
            setOnClickListener {
                CloudExportManager(this@AnalyticsActivity, habitList)
                    .exportToCloud("excel", "generic")
            }
        }
        rootLayout.addView(exportCloudButton)

        // Local Export Section
        val localTitle = TextView(this).apply {
            text = "📱 Local Export Options"
            textSize = 18f
            setPadding(0, 32, 0, 16)
        }
        rootLayout.addView(localTitle)

        // Export buttons
        val exportAllButton = Button(this).apply {
            text = "📁 Export to Downloads Folder"
            setOnClickListener { exportAllData() }
        }
        rootLayout.addView(exportAllButton)

        val exportJSONButton = Button(this).apply {
            text = "🔧 Export JSON Summary"
            setOnClickListener { exportJSONData() }
        }
        rootLayout.addView(exportJSONButton)

        setContentView(rootLayout)

        // Setup toolbar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Analytics"
    }

    private fun exportAllData() {
        // Use modern Android storage approach - no permissions needed
        performExportModern()
    }

    private fun performExportModern() {
        Toast.makeText(this, "Starting data export to Downloads...", Toast.LENGTH_SHORT).show()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Android 10+ - Use MediaStore API for Downloads
                    exportUsingMediaStore()
                } else {
                    // Android 6-9 - Use app-specific storage (always works)
                    exportToAppStorage()
                }

                withContext(Dispatchers.Main) {
                    if (result.isNotEmpty()) {
                        Toast.makeText(
                            this@AnalyticsActivity,
                            "✅ Data exported successfully!\n📁 $result",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        Toast.makeText(
                            this@AnalyticsActivity,
                            "❌ Export failed. Trying app storage instead...",
                            Toast.LENGTH_SHORT
                        ).show()

                        // Fallback to app storage
                        val fallbackResult = exportToAppStorage()
                        if (fallbackResult.isNotEmpty()) {
                            Toast.makeText(
                                this@AnalyticsActivity,
                                "✅ Exported to app storage!\n📁 $fallbackResult",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@AnalyticsActivity,
                        "Export error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun exportJSONData() {
        Toast.makeText(this, "Exporting JSON summary...", Toast.LENGTH_SHORT).show()
        // Use cloud export manager for JSON
        CloudExportManager(this, habitList).exportToCloud("looker", "generic", false)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        // Modern Android handles storage automatically - no action needed
    }

    /**
     * Get current priority distribution stats
     */
    private fun getPriorityDistributionText(): String {
        val priorities = mutableMapOf<HabitPriority, MutableList<String>>()
        
        // Initialize priority lists
        HabitPriority.values().forEach { priority ->
            priorities[priority] = mutableListOf()
        }
        
        // Categorize habits by priority
        for (i in 0 until habitList.size()) {
            val habit = habitList.getByPosition(i)
            if (!habit.isArchived) {
                val priority = habit.priority
                priorities[priority]?.add(habit.name)
            }
        }
        
        val stats = StringBuilder()
        stats.append("📊 Current Priority Distribution:\n\n")
        
        priorities.forEach { (priority, habits) ->
            if (habits.isNotEmpty()) {
                stats.append("${priority.icon} ${priority.displayName} (${priority.weight}x): ${habits.size} habits\n")
                habits.take(3).forEach { habitName ->
                    stats.append("  • $habitName\n")
                }
                if (habits.size > 3) {
                    stats.append("  • ... and ${habits.size - 3} more\n")
                }
                stats.append("\n")
            }
        }
        
        val totalWeight = priorities.map { (priority, habits) -> 
            priority.weight * habits.size 
        }.sum()
        
        stats.append("🎯 Total Weight Score: ${"%.1f".format(totalWeight)}\n")
        stats.append("💡 Higher weights = greater impact on performance scores")
        
        return stats.toString()
    }

    /**
     * Auto-assign smart priorities based on habit names
     */
    private fun autoAssignPriorities() {
        var assignedCount = 0
        
        for (i in 0 until habitList.size()) {
            val habit = habitList.getByPosition(i)
            val recommendedPriority = HabitPriority.getRecommendedPriority(habit.name)
            
            // Only update if priority is different
            if (habit.priority != recommendedPriority) {
                habit.priority = recommendedPriority
                assignedCount++
            }
        }
        
        Toast.makeText(
            this,
            "🤖 Smart priorities assigned to $assignedCount habits!\n" +
            "Quran, Tasks & Health → CRITICAL\n" +
            "Study & Exercise → HIGH\n" +
            "Entertainment → LOW",
            Toast.LENGTH_LONG
        ).show()
        
        // Refresh the display
        setupUI()
    }

    /**
     * Export using MediaStore API (Android 10+) - saves directly to Downloads
     */
    private fun exportUsingMediaStore(): String {
        return try {
            val timestamp = DateUtils.getToday().toString()
            val csvFileName = "uhabits_export_$timestamp.csv"
            val txtFileName = "export_info_$timestamp.txt"

            // Create CSV file using MediaStore
            val csvValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, csvFileName)
                put(MediaStore.Downloads.MIME_TYPE, "text/csv")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/uHabits_Analytics")
            }

            val csvUri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, csvValues)
            if (csvUri != null) {
                contentResolver.openOutputStream(csvUri)?.use { outputStream ->
                    OutputStreamWriter(outputStream).use { writer ->
                        writer.write("Habit_Name,Total_Entries,Is_Archived,Type,Color,Frequency,Target_Value\n")

                        habitList.forEach { habit ->
                            val entries = habit.computedEntries
                            val totalEntries = entries.getKnown().size
                            val color = habit.color.toString()
                            val frequency = habit.frequency.toString()
                            val targetValue = if (habit.isNumerical) habit.targetValue else "N/A"

                            writer.write("\"${habit.name}\",$totalEntries,${habit.isArchived},${habit.type},$color,$frequency,$targetValue\n")
                        }
                    }
                }
            }

            // Create info file using MediaStore
            val txtValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, txtFileName)
                put(MediaStore.Downloads.MIME_TYPE, "text/plain")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/uHabits_Analytics")
            }

            val txtUri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, txtValues)
            if (txtUri != null) {
                contentResolver.openOutputStream(txtUri)?.use { outputStream ->
                    OutputStreamWriter(outputStream).use { writer ->
                        writer.write(
                            """
                            uHabits Analytics Export
                            ========================
                            Export Date: ${DateUtils.getToday()}
                            Export Time: ${System.currentTimeMillis()}
                            Total Habits: ${habitList.size()}
                            Active Habits: ${habitList.getFiltered(org.isoron.uhabits.core.models.HabitMatcher(isArchivedAllowed = false)).size()}
                            
                            📁 Files exported to Downloads/uHabits_Analytics/:
                            - $csvFileName: Complete habit data
                            - $txtFileName: This info file
                            
                            📊 How to use with PowerBI:
                            1. Open PowerBI Desktop
                            2. Get Data → Text/CSV
                            3. Select $csvFileName from Downloads
                            4. Transform and create visualizations
                            
                            📈 How to use with Excel:
                            1. Open Excel
                            2. Data → From Text/CSV
                            3. Select $csvFileName from Downloads
                            4. Import and analyze
                            
                            Happy analyzing! 🚀
                            """.trimIndent()
                        )
                    }
                }
            }

            "Downloads/uHabits_Analytics/\n✅ Check your Downloads folder!"
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Export to app-specific storage (always works, no permissions needed)
     */
    private fun exportToAppStorage(): String {
        return try {
            val timestamp = DateUtils.getToday().toString()
            val folderName = "uHabits_Analytics"

            // Use app-specific external storage
            val appDir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
                ?: File(filesDir, folderName)

            val outputDir = File(appDir, folderName)
            if (!outputDir.exists()) {
                outputDir.mkdirs()
            }

            // Create CSV file
            val csvFile = File(outputDir, "uhabits_export_$timestamp.csv")
            csvFile.writeText("Habit_Name,Total_Entries,Is_Archived,Type,Color,Frequency,Target_Value\n")

            habitList.forEach { habit ->
                val entries = habit.computedEntries
                val totalEntries = entries.getKnown().size
                val color = habit.color.toString()
                val frequency = habit.frequency.toString()
                val targetValue = if (habit.isNumerical) habit.targetValue else "N/A"

                csvFile.appendText("\"${habit.name}\",$totalEntries,${habit.isArchived},${habit.type},$color,$frequency,$targetValue\n")
            }

            // Create info file
            val infoFile = File(outputDir, "export_info_$timestamp.txt")
            infoFile.writeText(
                """
                uHabits Analytics Export
                ========================
                Export Date: ${DateUtils.getToday()}
                Total Habits: ${habitList.size()}
                
                📁 Files exported:
                - uhabits_export_$timestamp.csv
                - export_info_$timestamp.txt
                
                📱 Location: ${outputDir.absolutePath}
                
                📊 Access via file manager:
                Android/data/org.isoron.uhabits/files/Download/uHabits_Analytics/
                
                Happy analyzing! 🚀
                """.trimIndent()
            )

            "App Storage/uHabits_Analytics/\n📱 Check file manager → Android/data/org.isoron.uhabits/"
        } catch (e: Exception) {
            ""
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
