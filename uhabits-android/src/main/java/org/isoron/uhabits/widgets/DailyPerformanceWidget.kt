/*
 * Copyright (C) 2025 Enhanced by mfadel85
 *
 * Daily Performance Widget
 *
 * Shows current day performance score and remaining habits to complete
 */

package org.isoron.uhabits.widgets

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.view.View
import org.isoron.uhabits.HabitsApplication
import org.isoron.uhabits.activities.habits.list.ListHabitsActivity
import org.isoron.uhabits.core.models.DailyScoreCalculator
import org.isoron.uhabits.core.models.HabitList
import org.isoron.uhabits.core.utils.DateUtils
import org.isoron.uhabits.widgets.views.DailyPerformanceWidgetView

class DailyPerformanceWidget(
    context: Context,
    id: Int
) : BaseWidget(context, id, false) {

    private val habitList: HabitList
    private val dailyScoreCalculator: DailyScoreCalculator

    init {
        val app = context.applicationContext as HabitsApplication
        habitList = app.component.habitList
        dailyScoreCalculator = DailyScoreCalculator(habitList)
    }

    override fun getOnClickPendingIntent(context: Context): PendingIntent? {
        return PendingIntent.getActivity(
            context,
            0,
            Intent(context, ListHabitsActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    override fun refreshData(widgetView: View) {
        val view = widgetView as DailyPerformanceWidgetView
        val today = DateUtils.getTodayWithOffset()
        val todayScore = dailyScoreCalculator.calculateDailyScore(today)
        view.refresh(todayScore)
    }

    override fun buildView(): View {
        return DailyPerformanceWidgetView(context, preferedBackgroundAlpha)
    }

    override val defaultHeight: Int = 100
    override val defaultWidth: Int = 150
}
