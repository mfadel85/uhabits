/*
 * Copyright (C) 2025 Enhanced by mfadel85
 * 
 * Analytics UI Components and Adapters
 */

package org.isoron.uhabits.activities.analytics

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import org.isoron.uhabits.R
import org.isoron.uhabits.core.models.analytics.AnalyticsEngine
import org.isoron.uhabits.core.utils.DateFormats
import kotlin.math.abs

/**
 * Sealed class representing different types of analytics items
 */
sealed class AnalyticsItem {
    data class Header(val title: String) : AnalyticsItem()
    data class WeeklyItem(val weeklyPerformance: AnalyticsEngine.WeeklyPerformance) : AnalyticsItem()
    data class MonthlyItem(val monthlyPerformance: AnalyticsEngine.MonthlyPerformance) : AnalyticsItem()
    data class TimePattern(val timePatterns: AnalyticsEngine.TimePatternAnalysis) : AnalyticsItem()
    data class TrendData(val trendAnalysis: AnalyticsEngine.TrendAnalysis) : AnalyticsItem()
    data class CorrelationItem(val correlation: AnalyticsEngine.HabitCorrelation) : AnalyticsItem()
    data class StreakData(val streakAnalysis: AnalyticsEngine.StreakAnalysis) : AnalyticsItem()
    data class HabitStreakItem(val habitName: String, val streakLength: Int) : AnalyticsItem()
    data class RecommendationItem(val recommendation: AnalyticsEngine.Recommendation) : AnalyticsItem()
    data class ChartPlaceholder(val title: String) : AnalyticsItem()
    data class EmptyState(val message: String) : AnalyticsItem()
}

/**
 * RecyclerView adapter for analytics items
 */
class AnalyticsAdapter(private val items: List<AnalyticsItem>) : RecyclerView.Adapter<AnalyticsViewHolder>() {
    
    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_WEEKLY = 1
        private const val TYPE_MONTHLY = 2
        private const val TYPE_TIME_PATTERN = 3
        private const val TYPE_TREND_DATA = 4
        private const val TYPE_CORRELATION = 5
        private const val TYPE_STREAK_DATA = 6
        private const val TYPE_HABIT_STREAK = 7
        private const val TYPE_RECOMMENDATION = 8
        private const val TYPE_CHART_PLACEHOLDER = 9
        private const val TYPE_EMPTY_STATE = 10
    }
    
    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is AnalyticsItem.Header -> TYPE_HEADER
            is AnalyticsItem.WeeklyItem -> TYPE_WEEKLY
            is AnalyticsItem.MonthlyItem -> TYPE_MONTHLY
            is AnalyticsItem.TimePattern -> TYPE_TIME_PATTERN
            is AnalyticsItem.TrendData -> TYPE_TREND_DATA
            is AnalyticsItem.CorrelationItem -> TYPE_CORRELATION
            is AnalyticsItem.StreakData -> TYPE_STREAK_DATA
            is AnalyticsItem.HabitStreakItem -> TYPE_HABIT_STREAK
            is AnalyticsItem.RecommendationItem -> TYPE_RECOMMENDATION
            is AnalyticsItem.ChartPlaceholder -> TYPE_CHART_PLACEHOLDER
            is AnalyticsItem.EmptyState -> TYPE_EMPTY_STATE
        }
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AnalyticsViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        
        return when (viewType) {
            TYPE_HEADER -> HeaderViewHolder(inflater.inflate(R.layout.item_analytics_header, parent, false))
            TYPE_WEEKLY -> WeeklyViewHolder(inflater.inflate(R.layout.item_weekly_performance, parent, false))
            TYPE_MONTHLY -> MonthlyViewHolder(inflater.inflate(R.layout.item_monthly_performance, parent, false))
            TYPE_TIME_PATTERN -> TimePatternViewHolder(inflater.inflate(R.layout.item_time_pattern, parent, false))
            TYPE_TREND_DATA -> TrendDataViewHolder(inflater.inflate(R.layout.item_trend_data, parent, false))
            TYPE_CORRELATION -> CorrelationViewHolder(inflater.inflate(R.layout.item_correlation, parent, false))
            TYPE_STREAK_DATA -> StreakDataViewHolder(inflater.inflate(R.layout.item_streak_data, parent, false))
            TYPE_HABIT_STREAK -> HabitStreakViewHolder(inflater.inflate(R.layout.item_habit_streak, parent, false))
            TYPE_RECOMMENDATION -> RecommendationViewHolder(inflater.inflate(R.layout.item_recommendation, parent, false))
            TYPE_CHART_PLACEHOLDER -> ChartPlaceholderViewHolder(inflater.inflate(R.layout.item_chart_placeholder, parent, false))
            TYPE_EMPTY_STATE -> EmptyStateViewHolder(inflater.inflate(R.layout.item_empty_state, parent, false))
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }
    
    override fun onBindViewHolder(holder: AnalyticsViewHolder, position: Int) {
        holder.bind(items[position])
    }
    
    override fun getItemCount(): Int = items.size
}

/**
 * Base view holder for analytics items
 */
abstract class AnalyticsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    abstract fun bind(item: AnalyticsItem)
}

/**
 * ViewHolder for section headers
 */
class HeaderViewHolder(itemView: View) : AnalyticsViewHolder(itemView) {
    private val titleText: TextView = itemView.findViewById(R.id.headerTitle)
    
    override fun bind(item: AnalyticsItem) {
        if (item is AnalyticsItem.Header) {
            titleText.text = item.title
        }
    }
}

/**
 * ViewHolder for weekly performance items
 */
class WeeklyViewHolder(itemView: View) : AnalyticsViewHolder(itemView) {
    private val weekDateText: TextView = itemView.findViewById(R.id.weekDateText)
    private val weekScoreText: TextView = itemView.findViewById(R.id.weekScoreText)
    private val weekScoreProgress: ProgressBar = itemView.findViewById(R.id.weekScoreProgress)
    private val bestHabitText: TextView = itemView.findViewById(R.id.bestHabitText)
    private val worstHabitText: TextView = itemView.findViewById(R.id.worstHabitText)
    
    override fun bind(item: AnalyticsItem) {
        if (item is AnalyticsItem.WeeklyItem) {
            val week = item.weeklyPerformance
            val dateFormat = DateFormats.getCSVDateFormat()
            
            weekDateText.text = "Week of ${dateFormat.format(week.weekStartDate.toJavaDate())}"
            weekScoreText.text = "${week.overallScore.toInt()}/100"
            weekScoreProgress.progress = week.overallScore.toInt()
            bestHabitText.text = "Best: ${week.bestHabit}"
            worstHabitText.text = "Needs work: ${week.worstHabit}"
            
            // Color code the score
            val color = when {
                week.overallScore >= 80 -> itemView.context.getColor(R.color.green_500)
                week.overallScore >= 60 -> itemView.context.getColor(R.color.orange_500)
                else -> itemView.context.getColor(R.color.red_500)
            }
            weekScoreText.setTextColor(color)
        }
    }
}

/**
 * ViewHolder for monthly performance items
 */
class MonthlyViewHolder(itemView: View) : AnalyticsViewHolder(itemView) {
    private val monthDateText: TextView = itemView.findViewById(R.id.monthDateText)
    private val monthScoreText: TextView = itemView.findViewById(R.id.monthScoreText)
    private val monthScoreProgress: ProgressBar = itemView.findViewById(R.id.monthScoreProgress)
    private val consistencyText: TextView = itemView.findViewById(R.id.consistencyText)
    private val improvementText: TextView = itemView.findViewById(R.id.improvementText)
    
    override fun bind(item: AnalyticsItem) {
        if (item is AnalyticsItem.MonthlyItem) {
            val month = item.monthlyPerformance
            val calendar = month.monthStartDate.toCalendar()
            val monthName = java.text.DateFormatSymbols().months[calendar.get(java.util.Calendar.MONTH)]
            val year = calendar.get(java.util.Calendar.YEAR)
            
            monthDateText.text = "$monthName $year"
            monthScoreText.text = "${month.overallScore.toInt()}/100"
            monthScoreProgress.progress = month.overallScore.toInt()
            consistencyText.text = "Consistency: ${month.consistency.toInt()}/100"
            
            val improvementSign = if (month.improvement >= 0) "+" else ""
            improvementText.text = "Change: $improvementSign${month.improvement.toInt()}%"
            
            // Color code improvement
            val improvementColor = when {
                month.improvement > 5 -> itemView.context.getColor(R.color.green_500)
                month.improvement < -5 -> itemView.context.getColor(R.color.red_500)
                else -> itemView.context.getColor(R.color.grey_600)
            }
            improvementText.setTextColor(improvementColor)
        }
    }
}

/**
 * ViewHolder for time pattern analysis
 */
class TimePatternViewHolder(itemView: View) : AnalyticsViewHolder(itemView) {
    private val bestDayText: TextView = itemView.findViewById(R.id.bestDayText)
    private val worstDayText: TextView = itemView.findViewById(R.id.worstDayText)
    private val weekendVsWeekdayText: TextView = itemView.findViewById(R.id.weekendVsWeekdayText)
    
    override fun bind(item: AnalyticsItem) {
        if (item is AnalyticsItem.TimePattern) {
            val patterns = item.timePatterns
            
            bestDayText.text = "Best day: ${patterns.bestPerformanceDayOfWeek}"
            worstDayText.text = "Challenging day: ${patterns.worstPerformanceDayOfWeek}"
            
            val (weekendScore, weekdayScore) = patterns.weekendVsWeekdayScore
            val comparison = when {
                weekendScore > weekdayScore + 5 -> "You perform better on weekends"
                weekdayScore > weekendScore + 5 -> "You perform better on weekdays"
                else -> "Weekend and weekday performance is similar"
            }
            weekendVsWeekdayText.text = comparison
        }
    }
}

/**
 * ViewHolder for trend data
 */
class TrendDataViewHolder(itemView: View) : AnalyticsViewHolder(itemView) {
    private val movingAvg7Text: TextView = itemView.findViewById(R.id.movingAvg7Text)
    private val movingAvg30Text: TextView = itemView.findViewById(R.id.movingAvg30Text)
    private val movingAvg90Text: TextView = itemView.findViewById(R.id.movingAvg90Text)
    private val volatilityText: TextView = itemView.findViewById(R.id.volatilityText)
    
    override fun bind(item: AnalyticsItem) {
        if (item is AnalyticsItem.TrendData) {
            val trend = item.trendAnalysis
            
            movingAvg7Text.text = "7-day average: ${trend.movingAverage7Days.toInt()}%"
            movingAvg30Text.text = "30-day average: ${trend.movingAverage30Days.toInt()}%"
            movingAvg90Text.text = "90-day average: ${trend.movingAverage90Days.toInt()}%"
            volatilityText.text = "Consistency: ${trend.volatility.toInt()}/100"
        }
    }
}

/**
 * ViewHolder for correlation items
 */
class CorrelationViewHolder(itemView: View) : AnalyticsViewHolder(itemView) {
    private val habitPairText: TextView = itemView.findViewById(R.id.habitPairText)
    private val correlationStrengthText: TextView = itemView.findViewById(R.id.correlationStrengthText)
    private val correlationDescriptionText: TextView = itemView.findViewById(R.id.correlationDescriptionText)
    private val correlationIndicator: View = itemView.findViewById(R.id.correlationIndicator)
    
    override fun bind(item: AnalyticsItem) {
        if (item is AnalyticsItem.CorrelationItem) {
            val correlation = item.correlation
            
            habitPairText.text = "${correlation.habit1} ↔ ${correlation.habit2}"
            correlationStrengthText.text = String.format("%.2f", correlation.correlationStrength)
            correlationDescriptionText.text = correlation.description
            
            // Color code correlation strength
            val color = when {
                correlation.correlationStrength > 0.5 -> itemView.context.getColor(R.color.green_500)
                correlation.correlationStrength < -0.5 -> itemView.context.getColor(R.color.red_500)
                else -> itemView.context.getColor(R.color.orange_500)
            }
            correlationIndicator.setBackgroundColor(color)
            correlationStrengthText.setTextColor(color)
        }
    }
}

/**
 * ViewHolder for streak data
 */
class StreakDataViewHolder(itemView: View) : AnalyticsViewHolder(itemView) {
    private val currentStreakText: TextView = itemView.findViewById(R.id.currentStreakText)
    private val bestStreakText: TextView = itemView.findViewById(R.id.bestStreakText)
    private val averageStreakText: TextView = itemView.findViewById(R.id.averageStreakText)
    private val recoveryRateText: TextView = itemView.findViewById(R.id.recoveryRateText)
    
    override fun bind(item: AnalyticsItem) {
        if (item is AnalyticsItem.StreakData) {
            val streak = item.streakAnalysis
            
            currentStreakText.text = "Current best: ${streak.longestCurrentStreak} days"
            bestStreakText.text = "All-time best: ${streak.longestOverallStreak} days"
            averageStreakText.text = "Average length: ${streak.averageStreakLength.toInt()} days"
            recoveryRateText.text = "Recovery rate: ${streak.streakRecoveryRate.toInt()}%"
        }
    }
}

/**
 * ViewHolder for individual habit streaks
 */
class HabitStreakViewHolder(itemView: View) : AnalyticsViewHolder(itemView) {
    private val habitNameText: TextView = itemView.findViewById(R.id.habitNameText)
    private val streakLengthText: TextView = itemView.findViewById(R.id.streakLengthText)
    private val streakProgress: ProgressBar = itemView.findViewById(R.id.streakProgress)
    
    override fun bind(item: AnalyticsItem) {
        if (item is AnalyticsItem.HabitStreakItem) {
            habitNameText.text = item.habitName
            streakLengthText.text = "${item.streakLength} days"
            
            // Visual representation of streak (max 100 for progress bar)
            val progress = minOf(item.streakLength, 100)
            streakProgress.progress = progress
            
            // Color code based on streak length
            val color = when {
                item.streakLength >= 30 -> itemView.context.getColor(R.color.green_500)
                item.streakLength >= 7 -> itemView.context.getColor(R.color.orange_500)
                else -> itemView.context.getColor(R.color.grey_600)
            }
            streakLengthText.setTextColor(color)
        }
    }
}

/**
 * ViewHolder for recommendations
 */
class RecommendationViewHolder(itemView: View) : AnalyticsViewHolder(itemView) {
    private val recommendationCard: CardView = itemView.findViewById(R.id.recommendationCard)
    private val priorityIndicator: View = itemView.findViewById(R.id.priorityIndicator)
    private val recommendationText: TextView = itemView.findViewById(R.id.recommendationText)
    private val recommendationTypeText: TextView = itemView.findViewById(R.id.recommendationTypeText)
    
    override fun bind(item: AnalyticsItem) {
        if (item is AnalyticsItem.RecommendationItem) {
            val recommendation = item.recommendation
            
            recommendationText.text = recommendation.message
            recommendationTypeText.text = recommendation.type.name.replace("_", " ").lowercase()
                .split(" ").joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
            
            // Color code by priority
            val color = when (recommendation.priority) {
                AnalyticsEngine.Priority.HIGH -> itemView.context.getColor(R.color.red_500)
                AnalyticsEngine.Priority.MEDIUM -> itemView.context.getColor(R.color.orange_500)
                AnalyticsEngine.Priority.LOW -> itemView.context.getColor(R.color.green_500)
            }
            priorityIndicator.setBackgroundColor(color)
        }
    }
}

/**
 * ViewHolder for chart placeholders
 */
class ChartPlaceholderViewHolder(itemView: View) : AnalyticsViewHolder(itemView) {
    private val chartTitleText: TextView = itemView.findViewById(R.id.chartTitleText)
    private val chartPlaceholder: View = itemView.findViewById(R.id.chartPlaceholder)
    
    override fun bind(item: AnalyticsItem) {
        if (item is AnalyticsItem.ChartPlaceholder) {
            chartTitleText.text = item.title
            // In a real implementation, this would contain actual charts
        }
    }
}

/**
 * ViewHolder for empty states
 */
class EmptyStateViewHolder(itemView: View) : AnalyticsViewHolder(itemView) {
    private val emptyMessageText: TextView = itemView.findViewById(R.id.emptyMessageText)
    
    override fun bind(item: AnalyticsItem) {
        if (item is AnalyticsItem.EmptyState) {
            emptyMessageText.text = item.message
        }
    }
}
