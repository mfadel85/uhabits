/*
 * Copyright (C) 2025 Enhanced by mfadel85
 *
 * Global Daily Performance View for main habits screen
 *
 * Shows overall daily performance across all habits
 */

package org.isoron.uhabits.activities.habits.list.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import org.isoron.uhabits.R
import org.isoron.uhabits.core.models.DailyScore
import org.isoron.uhabits.core.utils.DateUtils
import org.isoron.uhabits.utils.StyledResources
import org.isoron.uhabits.utils.dp
import kotlin.math.roundToInt

class GlobalDailyPerformanceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private lateinit var todayScoreText: TextView
    private lateinit var todayScoreCategory: TextView
    private lateinit var todayCompletedText: TextView
    private lateinit var yesterdayComparisonText: TextView
    private lateinit var weeklyAverageText: TextView
    private lateinit var miniChart: MiniChartView

    private var dailyScores: List<DailyScore> = emptyList()
    private var todayScore: DailyScore? = null
    private var yesterdayScore: DailyScore? = null
    private var weeklyAverage: Double = 0.0

    init {
        orientation = VERTICAL
        LayoutInflater.from(context).inflate(R.layout.global_daily_performance, this, true)
        setupViews()
        refreshData()
    }

    private fun setupViews() {
        todayScoreText = findViewById(R.id.todayScoreText)
        todayScoreCategory = findViewById(R.id.todayScoreCategory)
        todayCompletedText = findViewById(R.id.todayCompletedText)
        yesterdayComparisonText = findViewById(R.id.yesterdayComparisonText)
        weeklyAverageText = findViewById(R.id.weeklyAverageText)

        // Setup View Details click handler
        val viewDetailsText = findViewById<TextView>(R.id.viewDetailsText)
        viewDetailsText.setOnClickListener {
            openDetailedPerformanceView()
        }

        // Create and replace mini chart view
        val chartContainer = findViewById<View>(R.id.miniChart).parent as LinearLayout
        miniChart = MiniChartView(context)
        chartContainer.removeView(findViewById(R.id.miniChart))
        chartContainer.addView(
            miniChart,
            LinearLayout.LayoutParams(dp(120f).toInt(), dp(40f).toInt()).apply {
                topMargin = dp(4f).toInt()
            }
        )

        miniChart.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
    }

    fun setDailyScores(scores: List<DailyScore>) {
        this.dailyScores = scores.take(7) // Last 7 days for mini chart

        val today = DateUtils.getTodayWithOffset()
        val yesterday = today.minus(1)

        todayScore = scores.find { it.timestamp == today }
        yesterdayScore = scores.find { it.timestamp == yesterday }

        // Calculate weekly average
        weeklyAverage = if (scores.isNotEmpty()) {
            scores.take(7).map { it.score }.average()
        } else {
            0.0
        }

        refreshData()
    }

    fun refreshPerformanceData() {
        // Force refresh of the display
        refreshData()
    }

    private fun refreshData() {
        // Update today's score
        todayScore?.let { score ->
            todayScoreText.text = "${score.weightedScore.roundToInt()}"
            todayScoreCategory.text = getCategoryText(score.weightedCategory)
            todayScoreCategory.setTextColor(getCategoryColor(score.weightedCategory))

            todayCompletedText.text = "${score.completedHabits}/${score.totalHabits}"
        } ?: run {
            todayScoreText.text = "---"
            todayScoreCategory.text = "No data"
            todayCompletedText.text = "0/0"
        }

        // Update yesterday comparison
        val comparison = getYesterdayComparison()
        yesterdayComparisonText.text = comparison.first
        yesterdayComparisonText.setTextColor(comparison.second)

        // Update weekly average
        weeklyAverageText.text = "${weeklyAverage.roundToInt()}"

        // Update mini chart
        miniChart.invalidate()
    }

    private fun getYesterdayComparison(): Pair<String, Int> {
        val today = todayScore?.score ?: 0.0
        val yesterday = yesterdayScore?.score ?: 0.0

        return when {
            today > yesterday -> {
                val diff = today - yesterday
                "↗ +${diff.roundToInt()}" to ContextCompat.getColor(context, R.color.green_500)
            }
            today < yesterday -> {
                val diff = yesterday - today
                "↘ -${diff.roundToInt()}" to ContextCompat.getColor(context, R.color.red_500)
            }
            else -> {
                "→ 0" to ContextCompat.getColor(context, R.color.grey_500)
            }
        }
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

    private fun openDetailedPerformanceView() {
        val intent = android.content.Intent(context, org.isoron.uhabits.activities.habits.performance.DailyPerformanceDetailsActivity::class.java)
        context.startActivity(intent)
    }

    /**
     * Mini chart view for showing 7-day trend
     */
    inner class MiniChartView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
    ) : View(context, attrs, defStyleAttr) {

        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)

        init {
            textPaint.textSize = dp(10f)
            textPaint.typeface = Typeface.DEFAULT
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)

            if (dailyScores.isEmpty()) return

            val padding = dp(8f)
            val chartWidth = width - 2 * padding
            val chartHeight = height - 2 * padding

            if (chartWidth <= 0 || chartHeight <= 0) return

            // Draw bars for each day
            val barWidth = chartWidth / dailyScores.size.toFloat()
            val maxScore = 100.0

            dailyScores.forEachIndexed { index, score ->
                val barHeight = (chartHeight * (score.weightedScore / maxScore)).toFloat()
                val left = padding + index * barWidth + barWidth * 0.2f
                val right = padding + (index + 1) * barWidth - barWidth * 0.2f
                val top = padding + chartHeight - barHeight
                val bottom = padding + chartHeight

                // Set color based on weighted score category
                paint.color = getCategoryColor(score.weightedCategory)
                paint.alpha = 180

                val rect = RectF(left, top, right, bottom)
                canvas.drawRoundRect(rect, dp(2f), dp(2f), paint)

                // Draw score text
                val res = StyledResources(context)
                textPaint.color = res.getColor(R.attr.contrast80)
                textPaint.textAlign = Paint.Align.CENTER
                val scoreText = score.weightedScore.roundToInt().toString()
                canvas.drawText(
                    scoreText,
                    left + (right - left) / 2,
                    top - dp(4f),
                    textPaint
                )
            }
        }

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            val width = MeasureSpec.getSize(widthMeasureSpec)
            val height = dp(60f).toInt()
            setMeasuredDimension(width, height)
        }
    }
}
