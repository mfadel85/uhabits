/*
 * Copyright (C) 2025 Enhanced by mfadel85
 *
 * Habit Priority system for weighted scoring
 */

package org.isoron.uhabits.core.models

/**
 * Represents the priority/importance level of a habit
 * Used for weighted scoring in daily performance calculations
 * 
 * Flexible weighting system for core KPIs vs minor habits:
 * - CRITICAL: Core life habits (Quran, Tasks completion, Health)
 * - HIGH: Important daily habits (Exercise, Study, Work)
 * - NORMAL: Standard habits (Reading, Journaling)
 * - LOW: Nice-to-have habits (Social media limits, minor routines)
 */
enum class HabitPriority(
    val weight: Double,
    val displayName: String,
    val icon: String,
    val colorCode: String,
    val description: String
) {
    CRITICAL(4.0, "Critical", "⭐", "#D32F2F", "Core life habits & KPIs"), // Deep Red - Mission critical
    HIGH(2.5, "High", "🔥", "#FF5722", "Important daily habits"), // Red/Orange - High importance
    NORMAL(1.0, "Normal", "📝", "#2196F3", "Standard habits"), // Blue - Normal importance
    LOW(0.5, "Low", "🌱", "#4CAF50", "Nice-to-have habits"); // Green - Low importance

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

        /**
         * Get total weight multiplier for better performance calculation
         */
        fun getTotalWeightMultiplier(priorities: List<HabitPriority>): Double {
            return priorities.sumOf { it.weight }
        }

        /**
         * Calculate weighted average for mixed priority habits
         */
        fun calculateWeightedScore(
            habits: List<Pair<HabitPriority, Double>>
        ): Double {
            if (habits.isEmpty()) return 0.0
            
            val totalWeightedScore = habits.sumOf { (priority, score) -> 
                priority.weight * score 
            }
            val totalWeight = habits.sumOf { it.first.weight }
            
            return if (totalWeight > 0) totalWeightedScore / totalWeight else 0.0
        }

        /**
         * Get recommended priority for habit name pattern matching
         */
        fun getRecommendedPriority(habitName: String): HabitPriority {
            val name = habitName.lowercase()
            return when {
                // Critical patterns (Core KPIs)
                name.contains("quran") || name.contains("prayer") || name.contains("salah") -> CRITICAL
                name.contains("task") || name.contains("todo") || name.contains("work") -> CRITICAL
                name.contains("health") || name.contains("medicine") || name.contains("vitamin") -> CRITICAL
                name.contains("sleep") || name.contains("exercise") -> CRITICAL
                
                // High priority patterns
                name.contains("study") || name.contains("learn") -> HIGH
                name.contains("read") || name.contains("book") -> HIGH
                name.contains("diet") || name.contains("nutrition") -> HIGH
                name.contains("family") || name.contains("relationship") -> HIGH
                
                // Low priority patterns
                name.contains("social media") || name.contains("entertainment") -> LOW
                name.contains("game") || name.contains("tv") -> LOW
                name.contains("hobby") || name.contains("leisure") -> LOW
                
                // Default to normal
                else -> NORMAL
            }
        }
    }
}
