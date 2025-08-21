/*
 * Copyright (C) 2025 Enhanced by mfadel85
 *
 * Habit Priority system for weighted scoring
 */

package org.isoron.uhabits.core.models

/**
 * Represents the priority/importance level of a habit
 * Used for weighted scoring in daily performance calculations
 */
enum class HabitPriority(
    val weight: Double,
    val displayName: String,
    val icon: String,
    val colorCode: String
) {
    HIGH(3.0, "High", "🔥", "#FF5722"), // Red/Orange - High importance
    NORMAL(1.0, "Normal", "📝", "#2196F3"), // Blue - Normal importance
    LOW(0.5, "Low", "🌱", "#4CAF50"); // Green - Low importance

    companion object {
        fun fromWeight(weight: Double): HabitPriority {
            return values().find { it.weight == weight } ?: NORMAL
        }

        fun fromDisplayName(name: String): HabitPriority {
            return values().find { it.displayName == name } ?: NORMAL
        }

        fun fromOrdinal(ordinal: Int): HabitPriority {
            return values().getOrNull(ordinal) ?: NORMAL
        }
    }
}
