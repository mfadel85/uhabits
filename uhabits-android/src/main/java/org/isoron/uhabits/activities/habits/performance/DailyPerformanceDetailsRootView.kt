/*
 * Copyright (C) 2025 Enhanced by mfadel85
 *
 * Daily Performance Details Root View
 *
 * Shows comprehensive daily performance analytics with monthly breakdowns
 */

package org.isoron.uhabits.activities.habits.performance

import android.content.Context
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.core.widget.NestedScrollView
import org.isoron.uhabits.R
import org.isoron.uhabits.activities.habits.performance.views.MonthlyPerformanceView
import org.isoron.uhabits.activities.habits.performance.views.PerformanceCalendarView
import org.isoron.uhabits.activities.habits.performance.views.PerformanceStatsView
import org.isoron.uhabits.activities.habits.performance.views.PerformanceTrendsView
import org.isoron.uhabits.core.models.DailyScore
import org.isoron.uhabits.core.models.DailyScoreCalculator
import org.isoron.uhabits.core.models.PaletteColor
import org.isoron.uhabits.core.tasks.TaskRunner
import org.isoron.uhabits.utils.addAtTop
import org.isoron.uhabits.utils.addBelow
import org.isoron.uhabits.utils.buildToolbar
import org.isoron.uhabits.utils.currentTheme
import org.isoron.uhabits.utils.dp
import org.isoron.uhabits.utils.setupToolbar
import org.isoron.uhabits.utils.sres

class DailyPerformanceDetailsRootView(
    context: Context,
    private val dailyScoreCalculator: DailyScoreCalculator,
    private val taskRunner: TaskRunner
) : FrameLayout(context) {

    private val toolbar = buildToolbar()
    private val scrollView = NestedScrollView(context)
    private val contentLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(0, dp(16f).toInt(), 0, dp(16f).toInt())
    }

    // Performance views
    private val performanceStatsView = PerformanceStatsView(context)
    private val performanceCalendarView = PerformanceCalendarView(context)
    private val monthlyPerformanceView = MonthlyPerformanceView(context)
    private val performanceTrendsView = PerformanceTrendsView(context)

    init {
        setupLayout()
        setupToolbar()
    }

    private fun setupLayout() {
        val rootLayout = RelativeLayout(context).apply {
            background = sres.getDrawable(R.attr.windowBackgroundColor)
            addAtTop(toolbar)
            addBelow(scrollView, toolbar, height = MATCH_PARENT)
        }

        // Add sections to content layout
        contentLayout.apply {
            addView(
                performanceStatsView,
                LinearLayout.LayoutParams(MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(dp(16f).toInt(), 0, dp(16f).toInt(), dp(16f).toInt())
                }
            )

            addView(
                performanceCalendarView,
                LinearLayout.LayoutParams(MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(dp(16f).toInt(), 0, dp(16f).toInt(), dp(16f).toInt())
                }
            )

            addView(
                performanceTrendsView,
                LinearLayout.LayoutParams(MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(dp(16f).toInt(), 0, dp(16f).toInt(), dp(16f).toInt())
                }
            )

            addView(
                monthlyPerformanceView,
                LinearLayout.LayoutParams(MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(dp(16f).toInt(), 0, dp(16f).toInt(), dp(24f).toInt())
                }
            )
        }

        scrollView.addView(contentLayout, MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        addView(rootLayout, MATCH_PARENT, MATCH_PARENT)
    }

    private fun setupToolbar() {
        setupToolbar(
            toolbar = toolbar,
            title = "Daily Performance Details",
            color = PaletteColor(17),
            displayHomeAsUpEnabled = true,
            theme = currentTheme()
        )

        toolbar.setNavigationOnClickListener {
            (context as? DailyPerformanceDetailsActivity)?.finish()
        }
    }

    fun setPerformanceData(
        dailyScores: List<DailyScore>,
        monthlyStats: List<MonthlyStats>,
        weeklyStats: List<DailyScore>
    ) {
        performanceStatsView.setData(dailyScores, monthlyStats)
        performanceCalendarView.setDailyScores(dailyScores)
        monthlyPerformanceView.setMonthlyStats(monthlyStats)
        performanceTrendsView.setDailyScores(dailyScores)
    }
}
