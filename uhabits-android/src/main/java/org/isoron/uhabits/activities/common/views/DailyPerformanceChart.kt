/*
 * Copyright (C) 2025 Enhanced by mfadel85
 *
 * Daily Performance Chart for uHabits Android
 *
 * Visualizes daily performance scores and habit completion trends
 */

package org.isoron.uhabits.activities.common.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import org.isoron.uhabits.R
import org.isoron.uhabits.core.models.DailyScore
import org.isoron.uhabits.utils.InterfaceUtils.dpToPixels
import org.isoron.uhabits.utils.StyledResources
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max
import kotlin.math.min

class DailyPerformanceChart : ScrollableChart {

    private var paint: Paint? = null
    private var linePaint: Paint? = null
    private var fillPaint: Paint? = null
    private var textPaint: Paint? = null
    private var gridPaint: Paint? = null

    private var dailyScores: List<DailyScore> = emptyList()
    private var primaryColor = 0
    private var secondaryColor = 0
    private var textColor = 0
    private var gridColor = 0
    private var backgroundColor = 0

    private var baseSize = 0
    private var columnWidth = 0f
    private var columnHeight = 0
    private var nColumns = 0
    private var maxScore = 100.0
    private var minScore = 0.0

    private val rect = RectF()
    private val path = Path()
    private val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
    private val scoreFormat = DecimalFormat("#.0")

    private var showTrendLine = true
    private var showGrid = true
    private var showTooltips = false

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    private fun init() {
        initPaints()
        initColors()
        baseSize = resources.getDimensionPixelSize(R.dimen.baseSize)
    }

    private fun initPaints() {
        paint = Paint().apply {
            isAntiAlias = true
        }

        linePaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = dpToPixels(context, 2f)
        }

        fillPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
        }

        textPaint = Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        gridPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = dpToPixels(context, 0.5f)
        }
    }

    private fun initColors() {
        val res = StyledResources(context)
        primaryColor = res.getColor(R.attr.contrast80)
        secondaryColor = res.getColor(R.attr.contrast60)
        textColor = res.getColor(R.attr.contrast80)
        gridColor = res.getColor(R.attr.contrast20)
        backgroundColor = res.getColor(R.attr.cardBackgroundColor)
    }

    fun setDailyScores(scores: List<DailyScore>) {
        this.dailyScores = scores
        if (scores.isNotEmpty()) {
            maxScore = max(100.0, scores.maxOfOrNull { it.score } ?: 100.0)
            minScore = min(0.0, scores.minOfOrNull { it.score } ?: 0.0)
        }
        postInvalidate()
    }

    fun setColor(color: Int) {
        this.primaryColor = color
        postInvalidate()
    }

    fun setShowTrendLine(show: Boolean) {
        this.showTrendLine = show
        postInvalidate()
    }

    fun setShowGrid(show: Boolean) {
        this.showGrid = show
        postInvalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (dailyScores.isEmpty()) {
            drawEmptyState(canvas)
            return
        }

        // Draw background
        fillPaint!!.color = backgroundColor
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), fillPaint!!)

        // Draw grid
        if (showGrid) {
            drawGrid(canvas)
        }

        // Draw score area chart
        drawScoreArea(canvas)

        // Draw score line
        drawScoreLine(canvas)

        // Draw trend line
        if (showTrendLine) {
            drawTrendLine(canvas)
        }

        // Draw data points
        drawDataPoints(canvas)

        // Draw labels
        drawLabels(canvas)

        // Draw legend
        drawLegend(canvas)
    }

    private fun drawEmptyState(canvas: Canvas) {
        textPaint!!.color = secondaryColor
        textPaint!!.textSize = baseSize * 0.8f

        val message = "No data available"
        val textWidth = textPaint!!.measureText(message)
        canvas.drawText(
            message,
            width / 2f,
            height / 2f,
            textPaint!!
        )
    }

    private fun drawGrid(canvas: Canvas) {
        gridPaint!!.color = gridColor

        val padding = baseSize.toFloat()
        val chartHeight = height - padding * 2
        val chartWidth = width - padding * 2

        // Horizontal grid lines (score levels)
        val scoreSteps = 5
        for (i in 0..scoreSteps) {
            val y = padding + (chartHeight * i / scoreSteps)
            canvas.drawLine(padding, y, width - padding, y, gridPaint!!)

            // Score labels
            val score = maxScore - (maxScore - minScore) * i / scoreSteps
            textPaint!!.color = textColor
            textPaint!!.textSize = baseSize * 0.4f
            textPaint!!.textAlign = Paint.Align.RIGHT
            canvas.drawText(
                scoreFormat.format(score),
                padding - 8,
                y + textPaint!!.textSize / 3,
                textPaint!!
            )
        }

        // Vertical grid lines (dates)
        if (dailyScores.size > 1) {
            val step = max(1, dailyScores.size / 6) // Show ~6 date labels max
            for (i in dailyScores.indices step step) {
                val x = padding + (chartWidth * i / (dailyScores.size - 1))
                canvas.drawLine(x, padding, x, height - padding, gridPaint!!)
            }
        }
    }

    private fun drawScoreArea(canvas: Canvas) {
        if (dailyScores.size < 2) return

        val padding = baseSize.toFloat()
        val chartHeight = height - padding * 2
        val chartWidth = width - padding * 2

        path.reset()

        // Start from bottom left
        path.moveTo(padding, height - padding)

        // Draw to first point
        val firstY = padding + chartHeight * (1 - (dailyScores[0].weightedScore - minScore) / (maxScore - minScore)).toFloat()
        path.lineTo(padding, firstY)

        // Draw curve through all points
        for (i in dailyScores.indices) {
            val x = padding + (chartWidth * i / (dailyScores.size - 1))
            val y = padding + chartHeight * (1 - (dailyScores[i].weightedScore - minScore) / (maxScore - minScore)).toFloat()

            if (i == 0) {
                path.lineTo(x, y)
            } else {
                // Smooth curve
                val prevX = padding + (chartWidth * (i - 1) / (dailyScores.size - 1))
                val prevY = padding + chartHeight * (1 - (dailyScores[i - 1].weightedScore - minScore) / (maxScore - minScore)).toFloat()

                val controlX1 = prevX + (x - prevX) * 0.3f
                val controlX2 = prevX + (x - prevX) * 0.7f

                path.cubicTo(controlX1, prevY, controlX2, y, x, y)
            }
        }

        // Close path to bottom
        val lastX = padding + chartWidth
        path.lineTo(lastX, height - padding)
        path.close()

        // Fill with gradient effect
        fillPaint!!.color = Color.argb(50, Color.red(primaryColor), Color.green(primaryColor), Color.blue(primaryColor))
        canvas.drawPath(path, fillPaint!!)
    }

    private fun drawScoreLine(canvas: Canvas) {
        if (dailyScores.size < 2) return

        val padding = baseSize.toFloat()
        val chartHeight = height - padding * 2
        val chartWidth = width - padding * 2

        linePaint!!.color = primaryColor
        linePaint!!.strokeWidth = dpToPixels(context, 3f)

        path.reset()

        for (i in dailyScores.indices) {
            val x = padding + (chartWidth * i / (dailyScores.size - 1))
            val y = padding + chartHeight * (1 - (dailyScores[i].weightedScore - minScore) / (maxScore - minScore)).toFloat()

            if (i == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }

        canvas.drawPath(path, linePaint!!)
    }

    private fun drawTrendLine(canvas: Canvas) {
        if (dailyScores.size < 3) return

        // Calculate linear regression for trend
        var sumX = 0.0
        var sumY = 0.0
        var sumXY = 0.0
        var sumXX = 0.0
        val n = dailyScores.size

        for (i in dailyScores.indices) {
            val x = i.toDouble()
            val y = dailyScores[i].score
            sumX += x
            sumY += y
            sumXY += x * y
            sumXX += x * x
        }

        val slope = (n * sumXY - sumX * sumY) / (n * sumXX - sumX * sumX)
        val intercept = (sumY - slope * sumX) / n

        val padding = baseSize.toFloat()
        val chartHeight = height - padding * 2
        val chartWidth = width - padding * 2

        val startY = intercept
        val endY = slope * (n - 1) + intercept

        val startYPixel = padding + chartHeight * (1 - (startY - minScore) / (maxScore - minScore)).toFloat()
        val endYPixel = padding + chartHeight * (1 - (endY - minScore) / (maxScore - minScore)).toFloat()

        linePaint!!.color = secondaryColor
        linePaint!!.strokeWidth = dpToPixels(context, 1.5f)
        linePaint!!.pathEffect = android.graphics.DashPathEffect(floatArrayOf(10f, 5f), 0f)

        canvas.drawLine(padding, startYPixel, padding + chartWidth, endYPixel, linePaint!!)

        // Reset path effect
        linePaint!!.pathEffect = null
    }

    private fun drawDataPoints(canvas: Canvas) {
        val padding = baseSize.toFloat()
        val chartHeight = height - padding * 2
        val chartWidth = width - padding * 2
        val pointRadius = dpToPixels(context, 4f)

        for (i in dailyScores.indices) {
            val x = padding + (chartWidth * i / max(1, dailyScores.size - 1))
            val y = padding + chartHeight * (1 - (dailyScores[i].weightedScore - minScore) / (maxScore - minScore)).toFloat()

            // Color based on weighted score category
            val pointColor = when (dailyScores[i].weightedCategory) {
                DailyScore.ScoreCategory.EXCELLENT -> Color.parseColor("#4CAF50") // Green
                DailyScore.ScoreCategory.GOOD -> Color.parseColor("#8BC34A") // Light Green
                DailyScore.ScoreCategory.AVERAGE -> Color.parseColor("#FF9800") // Orange
                DailyScore.ScoreCategory.POOR -> Color.parseColor("#F44336") // Red
            }

            fillPaint!!.color = pointColor
            canvas.drawCircle(x, y, pointRadius, fillPaint!!)

            // White border
            fillPaint!!.color = Color.WHITE
            canvas.drawCircle(x, y, pointRadius - 1, fillPaint!!)
            fillPaint!!.color = pointColor
            canvas.drawCircle(x, y, pointRadius - 2, fillPaint!!)
        }
    }

    private fun drawLabels(canvas: Canvas) {
        if (dailyScores.isEmpty()) return

        val padding = baseSize.toFloat()
        val chartWidth = width - padding * 2

        textPaint!!.color = textColor
        textPaint!!.textSize = baseSize * 0.35f
        textPaint!!.textAlign = Paint.Align.CENTER

        // Date labels at bottom
        val step = max(1, dailyScores.size / 4) // Show ~4 date labels
        for (i in dailyScores.indices step step) {
            val x = padding + (chartWidth * i / max(1, dailyScores.size - 1))
            val dateText = dateFormat.format(dailyScores[i].timestamp.toJavaDate())
            canvas.drawText(dateText, x, height - padding / 2, textPaint!!)
        }
    }

    private fun drawLegend(canvas: Canvas) {
        val legendY = baseSize * 0.5f
        val legendItemWidth = baseSize * 3f
        var legendX = width - baseSize * 8f

        textPaint!!.textSize = baseSize * 0.3f
        textPaint!!.textAlign = Paint.Align.LEFT

        // Legend items
        val legendItems = listOf(
            Pair("Excellent", Color.parseColor("#4CAF50")),
            Pair("Good", Color.parseColor("#8BC34A")),
            Pair("Average", Color.parseColor("#FF9800")),
            Pair("Poor", Color.parseColor("#F44336"))
        )

        for ((label, color) in legendItems) {
            // Color indicator
            fillPaint!!.color = color
            canvas.drawCircle(legendX, legendY, baseSize * 0.1f, fillPaint!!)

            // Label
            textPaint!!.color = textColor
            canvas.drawText(label, legendX + baseSize * 0.3f, legendY + baseSize * 0.1f, textPaint!!)

            legendX += legendItemWidth
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = max(baseSize * 8, MeasureSpec.getSize(heightMeasureSpec))
        setMeasuredDimension(width, height)
    }

    /**
     * Get tooltip text for a specific point
     */
    fun getTooltipText(x: Float, y: Float): String? {
        if (dailyScores.isEmpty()) return null

        val padding = baseSize.toFloat()
        val chartWidth = width - padding * 2

        // Find closest data point
        var closestIndex = 0
        var minDistance = Float.MAX_VALUE

        for (i in dailyScores.indices) {
            val pointX = padding + (chartWidth * i / max(1, dailyScores.size - 1))
            val distance = kotlin.math.abs(x - pointX)

            if (distance < minDistance) {
                minDistance = distance
                closestIndex = i
            }
        }

        if (minDistance < baseSize) {
            val score = dailyScores[closestIndex]
            return "${dateFormat.format(score.timestamp.toJavaDate())}\n" +
                "Score: ${scoreFormat.format(score.weightedScore)}\n" +
                "Completed: ${score.completedHabits}/${score.totalHabits}\n" +
                "Category: ${score.weightedCategory.name.lowercase().replaceFirstChar { it.uppercase() }}"
        }

        return null
    }
}
