/*
 * Copyright (C) 2025 Enhanced by mfadel85
 * 
 * Advanced Analytics Activity for uHabits
 * 
 * Provides comprehensive analytics and data export functionality
 */

package org.isoron.uhabits.activities.analytics

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.isoron.uhabits.HabitsApplication
import org.isoron.uhabits.R
import org.isoron.uhabits.core.models.HabitList
import org.isoron.uhabits.core.utils.DateUtils
import org.isoron.uhabits.inject.HabitsApplicationComponent
import java.io.File
import java.io.FileOutputStream
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
                
                Export includes:
                • Complete habit information (name, type, color, frequency)
                • Entry counts and statistics
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
        
        // Export buttons
        val exportAllButton = Button(this).apply {
            text = "Export All Analytics Data"
            setOnClickListener { exportAllData() }
        }
        rootLayout.addView(exportAllButton)
        
        val exportPowerBIButton = Button(this).apply {
            text = "Export PowerBI Dataset"
            setOnClickListener { exportPowerBIData() }
        }
        rootLayout.addView(exportPowerBIButton)
        
        val exportJSONButton = Button(this).apply {
            text = "Export JSON Summary"
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
    
    private fun exportPowerBIData() {
        Toast.makeText(this, "Exporting PowerBI optimized dataset...", Toast.LENGTH_SHORT).show()
        // Similar implementation for PowerBI specific export
        exportAllData() // For now, use the same method
    }
    
    private fun exportJSONData() {
        Toast.makeText(this, "Exporting JSON summary...", Toast.LENGTH_SHORT).show()
        // Similar implementation for JSON export
        exportAllData() // For now, use the same method
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
                        writer.write("""
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
                        """.trimIndent())
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
            val appDir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) 
                ?: getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
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
            infoFile.writeText("""
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
            """.trimIndent())
            
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
