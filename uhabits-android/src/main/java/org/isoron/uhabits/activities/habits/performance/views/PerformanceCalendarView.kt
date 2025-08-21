/*
 * Copyright (C) 2025 Enhanced by mfadel85
 *
 * Performance Calendar View
 *
 * Shows calendar view with daily performance scores overlaid
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
import org.isoron.uhabits.core.models.DailyScore
import org.isoron.uhabits.utils.dp
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

class PerformanceCalendarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private var dailyScores: List<DailyScore> = emptyList()

    init {
        orientation = VERTICAL
        LayoutInflater.from(context).inflate(R.layout.performance_calendar_view, this, true)
    }

    fun setDailyScores(scores: List<DailyScore>) {
        this.dailyScores = scores
        refreshData()
    }

    private fun refreshData() {
        // Update current month header
        val currentDate = Calendar.getInstance()
        val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        findViewById<TextView>(R.id.currentMonthText).text = monthFormat.format(currentDate.time)

        // Update calendar grid
        val calendarGrid = findViewById<CalendarGridView>(R.id.calendarGrid)
        calendarGrid.setDailyScores(dailyScores)
    }
}

/**
 * Custom view for displaying calendar grid with performance scores
 */
class CalendarGridView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = dp(12f)
        textAlign = Paint.Align.CENTER
    }

    private var dailyScores: List<DailyScore> = emptyList()
    private val scoreMap = mutableMapOf<String, DailyScore>()

    fun setDailyScores(scores: List<DailyScore>) {
        this.dailyScores = scores
        scoreMap.clear()
        scores.forEach { score ->
            val calendar = score.timestamp.toCalendar()
            val key = "${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.MONTH)}-${calendar.get(Calendar.DAY_OF_MONTH)}"
            scoreMap[key] = score
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val cellSize = width / 7f
        val rows = 6 // Max weeks in a month

        val today = Calendar.getInstance()
        val currentMonth = today.get(Calendar.MONTH)
        val currentYear = today.get(Calendar.YEAR)

        // Start from first day of month
        val firstDay = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.MONTH, currentMonth)
            set(Calendar.YEAR, currentYear)
        }

        // Get first day of week (0 = Sunday)
        val startDayOfWeek = firstDay.get(Calendar.DAY_OF_WEEK) - 1

        // Draw calendar cells
        for (week in 0 until rows) {
            for (day in 0..6) {
                val x = day * cellSize
                val y = week * cellSize

                val cellDate = Calendar.getInstance().apply {
                    time = firstDay.time
                    add(Calendar.DAY_OF_MONTH, week * 7 + day - startDayOfWeek)
                }

                val rect = RectF(x, y, x + cellSize, y + cellSize)

                // Only draw days in current month
                if (cellDate.get(Calendar.MONTH) == currentMonth) {
                    val dayOfMonth = cellDate.get(Calendar.DAY_OF_MONTH)
                    val key = "${cellDate.get(Calendar.YEAR)}-${cellDate.get(Calendar.MONTH)}-$dayOfMonth"
                    val score = scoreMap[key]

                    // Draw cell background based on score
                    cellPaint.color = if (score != null) {
                        when (DailyScore.getCategory(score.score)) {
                            DailyScore.ScoreCategory.EXCELLENT -> ContextCompat.getColor(context, R.color.green_200)
                            DailyScore.ScoreCategory.GOOD -> ContextCompat.getColor(context, R.color.blue_200)
                            DailyScore.ScoreCategory.AVERAGE -> ContextCompat.getColor(context, R.color.orange_200)
                            DailyScore.ScoreCategory.POOR -> ContextCompat.getColor(context, R.color.red_200)
                            else -> ContextCompat.getColor(context, R.color.grey_100)
                        }
                    } else {
                        ContextCompat.getColor(context, R.color.grey_100)
                    }

                    canvas.drawRoundRect(rect, dp(4f), dp(4f), cellPaint)

                    // Draw day number
                    textPaint.color = ContextCompat.getColor(context, android.R.color.black)
                    canvas.drawText(
                        dayOfMonth.toString(),
                        x + cellSize / 2,
                        y + cellSize / 2 + dp(4f),
                        textPaint
                    )

                    // Draw score if available
                    if (score != null) {
                        textPaint.textSize = dp(8f)
                        textPaint.color = ContextCompat.getColor(context, android.R.color.black)
                        canvas.drawText(
                            score.score.roundToInt().toString(),
                            x + cellSize / 2,
                            y + cellSize - dp(4f),
                            textPaint
                        )
                        textPaint.textSize = dp(12f)
                    }
                }
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val cellSize = width / 7f
        val height = (cellSize * 6).toInt() // 6 weeks max
        setMeasuredDimension(width, height)
    }
}
