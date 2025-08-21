/*
 * Copyright (C) 2025 Enhanced by mfadel85
 *
 * Daily Performance Widget View
 *
 * Visual component showing current day performance and remaining habits
 */

package org.isoron.uhabits.widgets.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.view.View
import androidx.core.content.ContextCompat
import org.isoron.uhabits.R
import org.isoron.uhabits.core.models.DailyScore
import org.isoron.uhabits.utils.StyledResources
import org.isoron.uhabits.utils.dp
import kotlin.math.roundToInt

class DailyPerformanceWidgetView(
    context: Context,
    private val backgroundAlpha: Int
) : View(context) {

    private var dailyScore: DailyScore? = null
    private val sres = StyledResources(context)

    private val backgroundPaint = Paint().apply {
        isAntiAlias = true
        color = sres.getColor(R.attr.cardBackgroundColor)
        alpha = backgroundAlpha
    }

    private val scorePaint = Paint().apply {
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
        textSize = dp(20f)
    }

    private val labelPaint = Paint().apply {
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        textSize = dp(10f)
        color = sres.getColor(R.attr.contrast60)
    }

    private val habitsPaint = Paint().apply {
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        textSize = dp(14f)
        typeface = Typeface.DEFAULT_BOLD
    }

    private val progressPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    private val progressBackgroundPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = sres.getColor(R.attr.contrast20)
        alpha = 50
    }

    fun refresh(score: DailyScore) {
        this.dailyScore = score

        // Set score color based on weighted category
        scorePaint.color = when (score.weightedCategory) {
            DailyScore.ScoreCategory.EXCELLENT -> ContextCompat.getColor(context, R.color.green_500)
            DailyScore.ScoreCategory.GOOD -> ContextCompat.getColor(context, R.color.blue_500)
            DailyScore.ScoreCategory.AVERAGE -> ContextCompat.getColor(context, R.color.orange_500)
            DailyScore.ScoreCategory.POOR -> ContextCompat.getColor(context, R.color.red_500)
        }

        // Set habits remaining color
        val remaining = score.totalHabits - score.completedHabits
        habitsPaint.color = if (remaining > 0) {
            ContextCompat.getColor(context, R.color.orange_600)
        } else {
            ContextCompat.getColor(context, R.color.green_600)
        }

        // Set progress bar color
        progressPaint.color = scorePaint.color

        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val score = dailyScore ?: return

        val cornerRadius = dp(8f)
        val margin = dp(4f)
        val rect = RectF(margin, margin, width - margin, height - margin)

        // Draw background
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, backgroundPaint)

        val centerX = width / 2f
        val topSection = height * 0.3f
        val middleSection = height * 0.5f
        val bottomSection = height * 0.7f

        // Draw weighted score
        val scoreText = "${score.weightedScore.roundToInt()}"
        canvas.drawText(scoreText, centerX, topSection, scorePaint)

        // Draw "Today" label
        canvas.drawText("Today", centerX, topSection + dp(12f), labelPaint)

        // Draw progress bar showing completion percentage
        val progressWidth = width - dp(20f)
        val progressHeight = dp(6f)
        val progressLeft = (width - progressWidth) / 2f
        val progressTop = middleSection - progressHeight / 2f
        val progressRight = progressLeft + progressWidth
        val progressBottom = progressTop + progressHeight

        // Progress background
        val progressRect = RectF(progressLeft, progressTop, progressRight, progressBottom)
        canvas.drawRoundRect(progressRect, progressHeight / 2, progressHeight / 2, progressBackgroundPaint)

        // Progress fill
        val completionRatio = if (score.totalHabits > 0) {
            score.completedHabits.toFloat() / score.totalHabits.toFloat()
        } else {
            0f
        }
        val fillRight = progressLeft + (progressWidth * completionRatio)
        val fillRect = RectF(progressLeft, progressTop, fillRight, progressBottom)
        canvas.drawRoundRect(fillRect, progressHeight / 2, progressHeight / 2, progressPaint)

        // Draw habits remaining
        val remaining = score.totalHabits - score.completedHabits
        val habitsText = if (remaining > 0) {
            "$remaining to do"
        } else {
            "All done!"
        }
        canvas.drawText(habitsText, centerX, bottomSection, habitsPaint)

        // Draw weighted category label
        val categoryText = when (score.weightedCategory) {
            DailyScore.ScoreCategory.EXCELLENT -> "Excellent"
            DailyScore.ScoreCategory.GOOD -> "Good"
            DailyScore.ScoreCategory.AVERAGE -> "Average"
            DailyScore.ScoreCategory.POOR -> "Poor"
        }
        canvas.drawText(categoryText, centerX, bottomSection + dp(12f), labelPaint)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = resolveSize(dp(150f).toInt(), widthMeasureSpec)
        val height = resolveSize(dp(100f).toInt(), heightMeasureSpec)
        setMeasuredDimension(width, height)
    }
}
