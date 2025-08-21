/*
 * Copyright (C) 2025 Enhanced by mfadel85
 *
 * Daily Performance Widget Provider
 *
 * Provides daily performance widget for home screen
 */

package org.isoron.uhabits.widgets

import android.content.Context

class DailyPerformanceWidgetProvider : BaseWidgetProvider() {
    override fun getWidgetFromId(context: Context, id: Int): BaseWidget {
        return DailyPerformanceWidget(context, id)
    }
}
