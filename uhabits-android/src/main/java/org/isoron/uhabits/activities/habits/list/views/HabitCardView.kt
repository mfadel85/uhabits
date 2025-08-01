/*
 * Copyright (C) 2016-2021 Álinson Santos Xavier <git@axavier.org>
 *
 * This file is part of Loop Habit Tracker.
 *
 * Loop Habit Tracker is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Loop Habit Tracker is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.isoron.uhabits.activities.habits.list.views

import android.content.Context
import android.graphics.PointF
import android.graphics.text.LineBreaker.BREAK_STRATEGY_BALANCED
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import org.isoron.platform.gui.toInt
import org.isoron.uhabits.R
import org.isoron.uhabits.activities.common.views.RingView
import org.isoron.uhabits.core.models.Habit
import org.isoron.uhabits.core.models.ModelObservable
import org.isoron.uhabits.core.models.Timestamp
import org.isoron.uhabits.core.ui.screens.habits.list.ListHabitsBehavior
import org.isoron.uhabits.core.utils.DateUtils
import org.isoron.uhabits.inject.ActivityContext
import org.isoron.uhabits.utils.currentTheme
import org.isoron.uhabits.utils.dp
import org.isoron.uhabits.utils.sres
import javax.inject.Inject

class HabitCardViewFactory
@Inject constructor(
    @ActivityContext val context: Context,
    private val checkmarkPanelFactory: CheckmarkPanelViewFactory,
    private val numberPanelFactory: NumberPanelViewFactory,
    private val behavior: ListHabitsBehavior
) {
    fun create() = HabitCardView(context, checkmarkPanelFactory, numberPanelFactory, behavior)
}

class HabitCardView(
    @ActivityContext context: Context,
    checkmarkPanelFactory: CheckmarkPanelViewFactory,
    numberPanelFactory: NumberPanelViewFactory,
    private val behavior: ListHabitsBehavior
) : FrameLayout(context),
    ModelObservable.Listener {

    var buttonCount
        get() = checkmarkPanel.buttonCount
        set(value) {
            checkmarkPanel.buttonCount = value
            numberPanel.buttonCount = value
        }

    var dataOffset = 0
        set(value) {
            field = value
            checkmarkPanel.dataOffset = value
            numberPanel.dataOffset = value
        }

    var habit: Habit? = null
        set(newHabit) {
            if (isAttachedToWindow) {
                field?.observable?.removeListener(this)
                newHabit?.observable?.addListener(this)
            }
            field = newHabit
            if (newHabit != null) copyAttributesFrom(newHabit)
        }

    var score
        get() = scoreRing.getPercentage().toDouble()
        set(value) {
            scoreRing.setPercentage(value.toFloat())
            scoreRing.setPrecision(1.0f / 16)
        }

    var unit
        get() = numberPanel.units
        set(value) {
            numberPanel.units = value
        }

    var values
        get() = checkmarkPanel.values
        set(values) {
            checkmarkPanel.values = values
            numberPanel.values = values.map { it / 1000.0 }.toDoubleArray()
        }

    var threshold: Double
        get() = numberPanel.threshold
        set(value) {
            numberPanel.threshold = value
        }

    var notes
        get() = checkmarkPanel.notes
        set(values) {
            checkmarkPanel.notes = values
            numberPanel.notes = values
        }

    var checkmarkPanel: CheckmarkPanelView
    private var numberPanel: NumberPanelView
    private var innerFrame: LinearLayout
    private var label: TextView
    private var scoreRing: RingView

    private var currentToggleTaskId = 0

    init {
        scoreRing = RingView(context).apply {
            val thickness = dp(3f)
            val margin = dp(8f).toInt()
            val ringSize = dp(15f).toInt()
            layoutParams = LinearLayout.LayoutParams(ringSize, ringSize).apply {
                setMargins(margin, 0, margin, 0)
                gravity = Gravity.CENTER
            }
            setThickness(thickness)
        }

        label = TextView(context).apply {
            maxLines = 2
            ellipsize = TextUtils.TruncateAt.END
            layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
            if (SDK_INT >= Build.VERSION_CODES.Q) {
                breakStrategy = BREAK_STRATEGY_BALANCED
            }
        }

        checkmarkPanel = checkmarkPanelFactory.create().apply {
            onToggle = { timestamp, value, notes ->
                triggerRipple(timestamp)
                val location = getAbsoluteButtonLocation(timestamp)
                habit?.let {
                    behavior.onToggle(
                        it,
                        timestamp,
                        value,
                        notes,
                        location.x,
                        location.y
                    )
                }
            }
            onEdit = { timestamp ->
                triggerRipple(timestamp)
                val location = getAbsoluteButtonLocation(timestamp)
                habit?.let { behavior.onEdit(it, timestamp, location.x, location.y) }
            }
        }

        numberPanel = numberPanelFactory.create().apply {
            visibility = GONE
            onEdit = { timestamp ->
                triggerRipple(timestamp)
                val location = getAbsoluteButtonLocation(timestamp)
                habit?.let { behavior.onEdit(it, timestamp, location.x, location.y) }
            }
        }

        innerFrame = LinearLayout(context).apply {
            gravity = Gravity.CENTER_VERTICAL
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
            elevation = dp(1f)

            addView(scoreRing)
            addView(label)
            addView(checkmarkPanel)
            addView(numberPanel)

            setOnTouchListener { v, event ->
                v.background.setHotspot(event.x, event.y)
                false
            }
        }

        clipToPadding = false
        layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        val margin = dp(3f).toInt()
        setPadding(margin, 0, margin, margin)
        addView(innerFrame)
    }

    override fun onModelChange() {
        Handler(Looper.getMainLooper()).post {
            habit?.let { copyAttributesFrom(it) }
        }
    }

    override fun setSelected(isSelected: Boolean) {
        super.setSelected(isSelected)
        updateBackground(isSelected)
    }

    fun triggerRipple(timestamp: Timestamp) {
        val location = getRelativeButtonLocation(timestamp)
        triggerRipple(location.x, location.y)
    }

    private fun getRelativeButtonLocation(timestamp: Timestamp): PointF {
        val today = DateUtils.getTodayWithOffset()
        val offset = timestamp.daysUntil(today) - dataOffset
        val panel = when (habit!!.isNumerical) {
            true -> numberPanel
            false -> checkmarkPanel
        }
        val button = panel.buttons[offset]
        val y = button.height / 2.0f
        val x = panel.x + button.x + (button.width / 2).toFloat()
        return PointF(x, y)
    }

    private fun getAbsoluteButtonLocation(timestamp: Timestamp): PointF {
        val containerLocation = IntArray(2)
        this.getLocationInWindow(containerLocation)
        val relButtonLocation = getRelativeButtonLocation(timestamp)
        val windowInsets = rootWindowInsets
        val xInset = windowInsets?.displayCutout?.safeInsetLeft ?: 0
        val yInset = if (SDK_INT <= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            windowInsets?.systemWindowInsetTop ?: 0
        } else {
            0
        }
        return PointF(
            containerLocation[0].toFloat() + relButtonLocation.x - xInset,
            containerLocation[1].toFloat() + relButtonLocation.y - yInset
        )
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        habit?.observable?.addListener(this)
    }

    override fun onDetachedFromWindow() {
        habit?.observable?.removeListener(this)
        super.onDetachedFromWindow()
    }

    private fun copyAttributesFrom(h: Habit) {
        fun getActiveColor(habit: Habit): Int {
            return when (habit.isArchived) {
                true -> sres.getColor(R.attr.contrast60)
                false -> currentTheme().color(habit.color).toInt()
            }
        }

        val c = getActiveColor(h)
        label.apply {
            text = h.name
            setTextColor(c)
        }
        scoreRing.apply {
            setColor(c)
        }
        checkmarkPanel.apply {
            color = c
            visibility = when (h.isNumerical) {
                true -> View.GONE
                false -> View.VISIBLE
            }
        }
        numberPanel.apply {
            color = c
            units = h.unit
            targetType = h.targetType
            threshold = h.targetValue
            visibility = when (h.isNumerical) {
                true -> View.VISIBLE
                false -> View.GONE
            }
        }
    }

    private fun triggerRipple(x: Float, y: Float) {
        val background = innerFrame.background
        background.setHotspot(x, y)
        background.state = intArrayOf(
            android.R.attr.state_pressed,
            android.R.attr.state_enabled
        )
        Handler().postDelayed({ background.state = intArrayOf() }, 25)
    }

    private fun updateBackground(isSelected: Boolean) {
        val background = when (isSelected) {
            true -> R.drawable.selected_box
            false -> R.drawable.ripple
        }
        innerFrame.setBackgroundResource(background)
    }

    companion object {
        fun (() -> Unit).delay(delayInMillis: Long) {
            Handler(Looper.getMainLooper()).postDelayed(this, delayInMillis)
        }
    }
}
