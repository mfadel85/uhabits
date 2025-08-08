/*
 * Copyright (C) 2025 Enhanced by mfadel85
 *
 * Monthly Performance View
 *
 * Shows detailed monthly breakdowns of daily performance
 */

package org.isoron.uhabits.activities.habits.performance.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import org.isoron.uhabits.R
import org.isoron.uhabits.activities.habits.performance.MonthlyStats
import org.isoron.uhabits.core.models.DailyScore
import org.isoron.uhabits.utils.StyledResources
import org.isoron.uhabits.utils.dp
import kotlin.math.roundToInt

class MonthlyPerformanceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private var monthlyStats: List<MonthlyStats> = emptyList()

    init {
        orientation = VERTICAL
        LayoutInflater.from(context).inflate(R.layout.monthly_performance_view, this, true)
    }

    fun setMonthlyStats(stats: List<MonthlyStats>) {
        this.monthlyStats = stats
        refreshData()
    }

    private fun refreshData() {
        // Clear existing month views
        val monthContainer = findViewById<LinearLayout>(R.id.monthContainer)
        monthContainer.removeAllViews()

        monthlyStats.forEach { stats ->
            val monthView = createMonthView(stats)
            monthContainer.addView(monthView)
        }
    }

    private fun createMonthView(stats: MonthlyStats): View {
        val monthView = LayoutInflater.from(context).inflate(R.layout.month_performance_item, null)
        
        // Set month title
        monthView.findViewById<TextView>(R.id.monthTitle).text = stats.monthName
        
        // Set average score
        val avgScoreText = monthView.findViewById<TextView>(R.id.avgScoreText)
        val avgScoreCategory = monthView.findViewById<TextView>(R.id.avgScoreCategory)
        
        avgScoreText.text = "${stats.averageScore.roundToInt()}"
        avgScoreCategory.text = getCategoryText(DailyScore.getCategory(stats.averageScore))
        avgScoreCategory.setTextColor(getCategoryColor(DailyScore.getCategory(stats.averageScore)))
        
        // Set best and worst days
        stats.bestDay?.let { bestDay ->
            val bestDayText = monthView.findViewById<TextView>(R.id.bestDayText)
            val bestDayDate = monthView.findViewById<TextView>(R.id.bestDayDate)
            bestDayText.text = "${bestDay.score.roundToInt()}"
            bestDayDate.text = formatDate(bestDay.timestamp)
        }
        
        stats.worstDay?.let { worstDay ->
            val worstDayText = monthView.findViewById<TextView>(R.id.worstDayText)
            val worstDayDate = monthView.findViewById<TextView>(R.id.worstDayDate)
            worstDayText.text = "${worstDay.score.roundToInt()}"
            worstDayDate.text = formatDate(worstDay.timestamp)
        }
        
        // Set category breakdown
        monthView.findViewById<TextView>(R.id.excellentDaysText).text = "${stats.excellentDays}"
        monthView.findViewById<TextView>(R.id.goodDaysText).text = "${stats.goodDays}"
        monthView.findViewById<TextView>(R.id.averageDaysText).text = "${stats.averageDays}"
        monthView.findViewById<TextView>(R.id.poorDaysText).text = "${stats.poorDays}"
        
        // Set habits completed
        monthView.findViewById<TextView>(R.id.totalHabitsText).text = "${stats.totalHabitsCompleted}"
        monthView.findViewById<TextView>(R.id.avgHabitsText).text = "${"%.1f".format(stats.averageHabitsPerDay)}"
        
        // Set category distribution chart
        val categoryChart = monthView.findViewById<CategoryDistributionChart>(R.id.categoryChart)
        categoryChart.setData(stats.excellentDays, stats.goodDays, stats.averageDays, stats.poorDays)
        
        return monthView
    }

    private fun getCategoryText(category: DailyScore.ScoreCategory): String {
        return when (category) {
            DailyScore.ScoreCategory.EXCELLENT -> "Excellent"
            DailyScore.ScoreCategory.GOOD -> "Good"
            DailyScore.ScoreCategory.AVERAGE -> "Average"
            DailyScore.ScoreCategory.POOR -> "Poor"
        }
    }

    private fun getCategoryColor(category: DailyScore.ScoreCategory): Int {
        return when (category) {
            DailyScore.ScoreCategory.EXCELLENT -> ContextCompat.getColor(context, R.color.green_500)
            DailyScore.ScoreCategory.GOOD -> ContextCompat.getColor(context, R.color.blue_500)
            DailyScore.ScoreCategory.AVERAGE -> ContextCompat.getColor(context, R.color.orange_500)
            DailyScore.ScoreCategory.POOR -> ContextCompat.getColor(context, R.color.red_500)
        }
    }

    private fun formatDate(timestamp: org.isoron.uhabits.core.models.Timestamp): String {
        val calendar = timestamp.toCalendar()
        return java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault()).format(calendar.time)
    }
}

/**
 * Custom view for showing category distribution as a horizontal bar chart
 */
class CategoryDistributionChart @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var excellentDays = 0
    private var goodDays = 0
    private var averageDays = 0
    private var poorDays = 0

    fun setData(excellent: Int, good: Int, average: Int, poor: Int) {
        this.excellentDays = excellent
        this.goodDays = good
        this.averageDays = average
        this.poorDays = poor
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val total = excellentDays + goodDays + averageDays + poorDays
        if (total == 0) return

        val chartHeight = height - dp(8f)
        val chartTop = dp(4f)
        val cornerRadius = dp(4f)
        
        var currentX = 0f
        val chartWidth = width.toFloat()

        // Draw excellent section
        if (excellentDays > 0) {
            val sectionWidth = chartWidth * excellentDays / total
            paint.color = ContextCompat.getColor(context, R.color.green_500)
            val rect = RectF(currentX, chartTop, currentX + sectionWidth, chartTop + chartHeight)
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)
            currentX += sectionWidth
        }

        // Draw good section
        if (goodDays > 0) {
            val sectionWidth = chartWidth * goodDays / total
            paint.color = ContextCompat.getColor(context, R.color.blue_500)
            val rect = RectF(currentX, chartTop, currentX + sectionWidth, chartTop + chartHeight)
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)
            currentX += sectionWidth
        }

        // Draw average section
        if (averageDays > 0) {
            val sectionWidth = chartWidth * averageDays / total
            paint.color = ContextCompat.getColor(context, R.color.orange_500)
            val rect = RectF(currentX, chartTop, currentX + sectionWidth, chartTop + chartHeight)
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)
            currentX += sectionWidth
        }

        // Draw poor section
        if (poorDays > 0) {
            val sectionWidth = chartWidth * poorDays / total
            paint.color = ContextCompat.getColor(context, R.color.red_500)
            val rect = RectF(currentX, chartTop, currentX + sectionWidth, chartTop + chartHeight)
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = dp(20f).toInt()
        setMeasuredDimension(width, height)
    }
}
