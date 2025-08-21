/*
 * Copyright (C) 2025 Enhanced by mfadel85
 *
 * Performance Trends View
 *
 * Shows trend analysis and charts for daily performance over time
 */

package org.isoron.uhabits.activities.habits.performance.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import org.isoron.uhabits.R
import org.isoron.uhabits.core.models.DailyScore
import org.isoron.uhabits.utils.dp
import kotlin.math.max
import kotlin.math.min

class PerformanceTrendsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private var dailyScores: List<DailyScore> = emptyList()

    init {
        orientation = VERTICAL
        LayoutInflater.from(context).inflate(R.layout.performance_trends_view, this, true)
    }

    fun setDailyScores(scores: List<DailyScore>) {
        this.dailyScores = scores.sortedBy { it.timestamp }
        refreshData()
    }

    private fun refreshData() {
        calculateTrends()

        // Update trend chart
        val trendChart = findViewById<TrendChart>(R.id.trendChart)
        trendChart.setData(dailyScores)
    }

    private fun calculateTrends() {
        if (dailyScores.isEmpty()) return

        val scores = dailyScores.map { it.score }
        val recent30Days = scores.takeLast(30)
        val previous30Days = scores.drop(max(0, scores.size - 60)).take(30)

        // Calculate trend direction
        val recentAvg = recent30Days.average()
        val previousAvg = if (previous30Days.isNotEmpty()) previous30Days.average() else recentAvg
        val trendDirection = recentAvg - previousAvg

        // Update trend indicators
        val trendDirectionText = findViewById<TextView>(R.id.trendDirectionText)
        val trendPercentageText = findViewById<TextView>(R.id.trendPercentageText)

        when {
            trendDirection > 2 -> {
                trendDirectionText.text = "↗ Improving"
                trendDirectionText.setTextColor(ContextCompat.getColor(context, R.color.green_500))
            }
            trendDirection < -2 -> {
                trendDirectionText.text = "↘ Declining"
                trendDirectionText.setTextColor(ContextCompat.getColor(context, R.color.red_500))
            }
            else -> {
                trendDirectionText.text = "→ Stable"
                trendDirectionText.setTextColor(ContextCompat.getColor(context, R.color.grey_600))
            }
        }

        val percentageChange = if (previousAvg > 0) (trendDirection / previousAvg * 100) else 0.0
        trendPercentageText.text = "${if (percentageChange >= 0) "+" else ""}${"%.1f".format(percentageChange)}%"

        // Calculate streak information
        val currentStreak = calculateCurrentStreak()
        val longestStreak = calculateLongestStreak()

        findViewById<TextView>(R.id.currentStreakText).text = "$currentStreak days"
        findViewById<TextView>(R.id.longestStreakText).text = "$longestStreak days"

        // Calculate consistency metrics
        val consistency = calculateConsistency()
        findViewById<TextView>(R.id.consistencyText).text = "${"%.1f".format(consistency)}%"

        // Calculate volatility (standard deviation)
        val volatility = calculateVolatility()
        findViewById<TextView>(R.id.volatilityText).text = "${"%.1f".format(volatility)}"
    }

    private fun calculateCurrentStreak(): Int {
        var streak = 0
        for (i in dailyScores.indices.reversed()) {
            if (dailyScores[i].score >= 70) { // Consider 70+ as good performance
                streak++
            } else {
                break
            }
        }
        return streak
    }

    private fun calculateLongestStreak(): Int {
        var longestStreak = 0
        var currentStreak = 0

        dailyScores.forEach { score ->
            if (score.score >= 70) {
                currentStreak++
                longestStreak = max(longestStreak, currentStreak)
            } else {
                currentStreak = 0
            }
        }

        return longestStreak
    }

    private fun calculateConsistency(): Double {
        if (dailyScores.isEmpty()) return 0.0

        val goodDays = dailyScores.count { it.score >= 70 }
        return (goodDays.toDouble() / dailyScores.size) * 100
    }

    private fun calculateVolatility(): Double {
        if (dailyScores.size < 2) return 0.0

        val scores = dailyScores.map { it.score }
        val mean = scores.average()
        val variance = scores.map { (it - mean) * (it - mean) }.average()
        return kotlin.math.sqrt(variance)
    }
}

/**
 * Custom view for showing performance trend chart
 */
class TrendChart @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = dp(2f)
        style = Paint.Style.STROKE
    }

    private val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = dp(1f)
        style = Paint.Style.STROKE
        color = ContextCompat.getColor(context, R.color.grey_300)
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = dp(10f)
        color = ContextCompat.getColor(context, R.color.grey_600)
    }

    private var dailyScores: List<DailyScore> = emptyList()
    private val points = mutableListOf<PointF>()

    fun setData(scores: List<DailyScore>) {
        this.dailyScores = scores.sortedBy { it.timestamp }
        calculatePoints()
        invalidate()
    }

    private fun calculatePoints() {
        points.clear()
        if (dailyScores.isEmpty()) return

        val padding = dp(20f)
        val chartWidth = width - 2 * padding
        val chartHeight = height - 2 * padding

        val maxScore = 100.0f
        val minScore = 0.0f

        dailyScores.forEachIndexed { index, score ->
            val x = padding + (index.toFloat() / (dailyScores.size - 1).toFloat()) * chartWidth
            val y = padding + chartHeight - ((score.score.toFloat() - minScore) / (maxScore - minScore)) * chartHeight
            points.add(PointF(x, y))
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        calculatePoints()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (dailyScores.isEmpty()) {
            // Draw empty state
            val emptyText = "No data available"
            val textWidth = textPaint.measureText(emptyText)
            canvas.drawText(
                emptyText,
                (width - textWidth) / 2,
                height / 2f,
                textPaint
            )
            return
        }

        drawGrid(canvas)
        drawTrendLine(canvas)
        drawDataPoints(canvas)
    }

    private fun drawGrid(canvas: Canvas) {
        val padding = dp(20f)
        val chartWidth = width - 2 * padding
        val chartHeight = height - 2 * padding

        // Draw horizontal grid lines
        for (i in 0..4) {
            val y = padding + (i.toFloat() / 4) * chartHeight
            canvas.drawLine(padding, y, padding + chartWidth, y, gridPaint)

            // Draw score labels
            val score = (100 - i * 25).toString()
            canvas.drawText(score, padding - dp(15f), y + dp(3f), textPaint)
        }

        // Draw vertical grid lines (every 7 days)
        val daysToShow = min(dailyScores.size, 90)
        val stepSize = max(1, daysToShow / 7)

        for (i in 0 until daysToShow step stepSize) {
            val x = padding + (i.toFloat() / (daysToShow - 1).toFloat()) * chartWidth
            canvas.drawLine(x, padding, x, padding + chartHeight, gridPaint)
        }
    }

    private fun drawTrendLine(canvas: Canvas) {
        if (points.size < 2) return

        val path = Path()
        path.moveTo(points[0].x, points[0].y)

        // Create smooth curve using quadratic bezier
        for (i in 1 until points.size) {
            val prevPoint = points[i - 1]
            val currentPoint = points[i]

            val midX = (prevPoint.x + currentPoint.x) / 2
            val midY = (prevPoint.y + currentPoint.y) / 2

            if (i == 1) {
                path.lineTo(midX, midY)
            } else {
                path.quadTo(prevPoint.x, prevPoint.y, midX, midY)
            }
        }

        // Final line to last point
        if (points.size > 1) {
            val lastPoint = points.last()
            val secondLastPoint = points[points.size - 2]
            path.quadTo(secondLastPoint.x, secondLastPoint.y, lastPoint.x, lastPoint.y)
        }

        linePaint.color = ContextCompat.getColor(context, R.color.blue_500)
        canvas.drawPath(path, linePaint)
    }

    private fun drawDataPoints(canvas: Canvas) {
        points.forEachIndexed { index, point ->
            val score = dailyScores[index]
            pointPaint.color = when (DailyScore.getCategory(score.score)) {
                DailyScore.ScoreCategory.EXCELLENT -> ContextCompat.getColor(context, R.color.green_500)
                DailyScore.ScoreCategory.GOOD -> ContextCompat.getColor(context, R.color.blue_500)
                DailyScore.ScoreCategory.AVERAGE -> ContextCompat.getColor(context, R.color.orange_500)
                DailyScore.ScoreCategory.POOR -> ContextCompat.getColor(context, R.color.red_500)
                else -> ContextCompat.getColor(context, R.color.grey_500)
            }

            canvas.drawCircle(point.x, point.y, dp(3f), pointPaint)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = dp(200f).toInt()
        setMeasuredDimension(width, height)
    }
}
