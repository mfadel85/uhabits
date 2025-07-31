/*
 * Copyright (C) 2025 Enhanced by mfadel85
 * 
 * Advanced Analytics Activity for Enhanced uHabits
 * 
 * Displays comprehensive analytics with 100-point scoring system,
 * trends, correlations, and actionable insights.
 */

package org.isoron.uhabits.activities.analytics

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import org.isoron.uhabits.HabitsApplication
import org.isoron.uhabits.R
import org.isoron.uhabits.core.models.analytics.AnalyticsEngine
import org.isoron.uhabits.core.io.analytics.AnalyticsDataExporter
import org.isoron.uhabits.core.utils.DateUtils
import org.isoron.uhabits.inject.HabitsApplicationComponent
import java.io.File
import javax.inject.Inject

/**
 * Activity that displays comprehensive analytics and insights
 */
class AdvancedAnalyticsActivity : AppCompatActivity() {
    
    @Inject
    lateinit var habitList: org.isoron.uhabits.core.models.HabitList
    
    private lateinit var analyticsEngine: AnalyticsEngine
    private lateinit var dataExporter: AnalyticsDataExporter
    
    // UI Components
    private lateinit var tabLayout: TabLayout
    private lateinit var overallScoreText: TextView
    private lateinit var overallScoreProgress: ProgressBar
    private lateinit var trendIndicator: ImageView
    private lateinit var trendText: TextView
    private lateinit var improvementRateText: TextView
    private lateinit var consistencyScoreText: TextView
    private lateinit var recyclerView: RecyclerView
    
    private var currentReport: AnalyticsEngine.AnalyticsReport? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val appComponent = (application as HabitsApplication).component
        appComponent.inject(this)
        
        setContentView(R.layout.activity_advanced_analytics)
        setupToolbar()
        initializeComponents()
        setupTabs()
        loadAnalytics()
    }
    
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.advanced_analytics_menu, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_export_analytics -> {
                exportAnalyticsData()
                true
            }
            R.id.action_refresh -> {
                loadAnalytics()
                true
            }
            R.id.action_date_range -> {
                showDateRangePicker()
                true
            }
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun setupToolbar() {
        supportActionBar?.apply {
            title = "Advanced Analytics"
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }
    }
    
    private fun initializeComponents() {
        analyticsEngine = AnalyticsEngine(habitList)
        
        // Initialize UI components
        tabLayout = findViewById(R.id.tabLayout)
        overallScoreText = findViewById(R.id.overallScoreText)
        overallScoreProgress = findViewById(R.id.overallScoreProgress)
        trendIndicator = findViewById(R.id.trendIndicator)
        trendText = findViewById(R.id.trendText)
        improvementRateText = findViewById(R.id.improvementRateText)
        consistencyScoreText = findViewById(R.id.consistencyScoreText)
        recyclerView = findViewById(R.id.recyclerView)
        
        recyclerView.layoutManager = LinearLayoutManager(this)
    }
    
    private fun setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("Overview"))
        tabLayout.addTab(tabLayout.newTab().setText("Trends"))
        tabLayout.addTab(tabLayout.newTab().setText("Correlations"))
        tabLayout.addTab(tabLayout.newTab().setText("Streaks"))
        tabLayout.addTab(tabLayout.newTab().setText("Recommendations"))
        
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.let { updateTabContent(it.position) }
            }
            
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }
    
    private fun loadAnalytics() {
        // Show loading indicator
        showLoadingState()
        
        // Load analytics in background
        Thread {
            try {
                val report = analyticsEngine.generateReport()
                
                runOnUiThread {
                    currentReport = report
                    hideLoadingState()
                    displayOverallMetrics(report)
                    updateTabContent(tabLayout.selectedTabPosition)
                }
            } catch (e: Exception) {
                runOnUiThread {
                    hideLoadingState()
                    showErrorMessage("Failed to load analytics: ${e.message}")
                }
            }
        }.start()
    }
    
    private fun displayOverallMetrics(report: AnalyticsEngine.AnalyticsReport) {
        // Overall Score (0-100)
        val score = report.overallScore.toInt()
        overallScoreText.text = "${score}/100"
        overallScoreProgress.progress = score
        
        // Color code the score
        val color = when {
            score >= 80 -> getColor(R.color.green_500)
            score >= 60 -> getColor(R.color.orange_500)
            else -> getColor(R.color.red_500)
        }
        overallScoreText.setTextColor(color)
        
        // Trend Direction
        val trend = report.trendAnalysis.overallTrend
        trendText.text = trend.name.lowercase().replaceFirstChar { it.uppercase() }
        
        val trendIcon = when (trend) {
            AnalyticsEngine.AdvancedScore.TrendDirection.UP -> R.drawable.ic_trending_up
            AnalyticsEngine.AdvancedScore.TrendDirection.DOWN -> R.drawable.ic_trending_down
            AnalyticsEngine.AdvancedScore.TrendDirection.STABLE -> R.drawable.ic_trending_flat
        }
        trendIndicator.setImageResource(trendIcon)
        
        // Improvement Rate
        val improvementRate = report.trendAnalysis.improvementRate
        improvementRateText.text = String.format("%.1f%% per week", improvementRate)
        
        // Consistency Score
        val consistency = report.trendAnalysis.volatility
        consistencyScoreText.text = String.format("%.0f/100", consistency)
    }
    
    private fun updateTabContent(position: Int) {
        val report = currentReport ?: return
        
        when (position) {
            0 -> showOverviewTab(report)
            1 -> showTrendsTab(report)
            2 -> showCorrelationsTab(report)
            3 -> showStreaksTab(report)
            4 -> showRecommendationsTab(report)
        }
    }
    
    private fun showOverviewTab(report: AnalyticsEngine.AnalyticsReport) {
        val items = mutableListOf<AnalyticsItem>()
        
        // Weekly Performance Summary
        items.add(AnalyticsItem.Header("Weekly Performance"))
        report.weeklyBreakdown.takeLast(4).forEach { week ->
            items.add(AnalyticsItem.WeeklyItem(week))
        }
        
        // Monthly Performance Summary
        items.add(AnalyticsItem.Header("Monthly Performance"))
        report.monthlyBreakdown.takeLast(3).forEach { month ->
            items.add(AnalyticsItem.MonthlyItem(month))
        }
        
        // Time Patterns
        items.add(AnalyticsItem.Header("Time Patterns"))
        items.add(AnalyticsItem.TimePattern(report.timePatterns))
        
        recyclerView.adapter = AnalyticsAdapter(items)
    }
    
    private fun showTrendsTab(report: AnalyticsEngine.AnalyticsReport) {
        val items = mutableListOf<AnalyticsItem>()
        
        items.add(AnalyticsItem.Header("Trend Analysis"))
        items.add(AnalyticsItem.TrendData(report.trendAnalysis))
        
        // Add chart placeholders
        items.add(AnalyticsItem.Header("Performance Charts"))
        items.add(AnalyticsItem.ChartPlaceholder("7-Day Moving Average"))
        items.add(AnalyticsItem.ChartPlaceholder("30-Day Moving Average"))
        items.add(AnalyticsItem.ChartPlaceholder("90-Day Moving Average"))
        
        recyclerView.adapter = AnalyticsAdapter(items)
    }
    
    private fun showCorrelationsTab(report: AnalyticsEngine.AnalyticsReport) {
        val items = mutableListOf<AnalyticsItem>()
        
        items.add(AnalyticsItem.Header("Habit Correlations"))
        
        if (report.habitCorrelations.isEmpty()) {
            items.add(AnalyticsItem.EmptyState("No significant correlations found. Track more habits to see patterns."))
        } else {
            report.habitCorrelations.forEach { correlation ->
                items.add(AnalyticsItem.CorrelationItem(correlation))
            }
        }
        
        recyclerView.adapter = AnalyticsAdapter(items)
    }
    
    private fun showStreaksTab(report: AnalyticsEngine.AnalyticsReport) {
        val items = mutableListOf<AnalyticsItem>()
        
        items.add(AnalyticsItem.Header("Streak Analysis"))
        items.add(AnalyticsItem.StreakData(report.streakAnalysis))
        
        // Add individual habit streaks
        items.add(AnalyticsItem.Header("Current Streaks by Habit"))
        
        habitList.getFiltered { !it.isArchived }.forEach { habit ->
            val currentStreak = habit.streaks.getAll().firstOrNull()?.length ?: 0
            items.add(AnalyticsItem.HabitStreakItem(habit.name, currentStreak))
        }
        
        recyclerView.adapter = AnalyticsAdapter(items)
    }
    
    private fun showRecommendationsTab(report: AnalyticsEngine.AnalyticsReport) {
        val items = mutableListOf<AnalyticsItem>()
        
        items.add(AnalyticsItem.Header("Personalized Recommendations"))
        
        if (report.recommendations.isEmpty()) {
            items.add(AnalyticsItem.EmptyState("Great job! No specific recommendations at this time."))
        } else {
            report.recommendations.forEach { recommendation ->
                items.add(AnalyticsItem.RecommendationItem(recommendation))
            }
        }
        
        recyclerView.adapter = AnalyticsAdapter(items)
    }
    
    private fun exportAnalyticsData() {
        Toast.makeText(this, "Preparing analytics export...", Toast.LENGTH_SHORT).show()
        
        Thread {
            try {
                val exportDir = File(getExternalFilesDir(null), "analytics_export")
                exportDir.mkdirs()
                
                dataExporter = AnalyticsDataExporter(habitList, analyticsEngine, exportDir)
                val result = dataExporter.exportAnalyticsData()
                
                runOnUiThread {
                    if (result.success) {
                        showExportSuccessDialog(exportDir, result.exportedFiles)
                    } else {
                        showErrorMessage("Export failed")
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    showErrorMessage("Export error: ${e.message}")
                }
            }
        }.start()
    }
    
    private fun showExportSuccessDialog(exportDir: File, files: List<String>) {
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Analytics Export Complete")
            .setMessage("${files.size} files exported to:\n${exportDir.absolutePath}\n\n" +
                       "Files include:\n${files.joinToString("\n") { "• $it" }}\n\n" +
                       "These files are optimized for PowerBI and Looker Studio.")
            .setPositiveButton("Share") { _, _ ->
                shareExportedFiles(exportDir)
            }
            .setNegativeButton("OK", null)
            .create()
        
        dialog.show()
    }
    
    private fun shareExportedFiles(exportDir: File) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "uHabits Analytics Export")
            putExtra(Intent.EXTRA_TEXT, "Analytics data exported from uHabits.\nLocation: ${exportDir.absolutePath}")
        }
        startActivity(Intent.createChooser(intent, "Share Analytics Export"))
    }
    
    private fun showDateRangePicker() {
        // Implement date range picker
        Toast.makeText(this, "Date range picker coming soon", Toast.LENGTH_SHORT).show()
    }
    
    private fun showLoadingState() {
        // Show loading UI
        findViewById<ProgressBar>(R.id.loadingProgress)?.visibility = android.view.View.VISIBLE
    }
    
    private fun hideLoadingState() {
        // Hide loading UI
        findViewById<ProgressBar>(R.id.loadingProgress)?.visibility = android.view.View.GONE
    }
    
    private fun showErrorMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
    
    companion object {
        fun createIntent(context: Context): Intent {
            return Intent(context, AdvancedAnalyticsActivity::class.java)
        }
    }
}
