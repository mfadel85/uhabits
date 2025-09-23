/*
 * Copyright (C) 2025 Enhanced by mfadel85
 *
 * Habit Group system for category-based analytics
 */

package org.isoron.uhabits.core.models

/**
 * Represents different life categories/groups that habits belong to
 * Used for group-based analytics and performance tracking
 * 
 * Four main life areas for comprehensive habit tracking:
 * - RELIGIOUS: Spiritual and religious practices
 * - CAREER_WORK: Professional development and work-related habits
 * - SOCIAL_FAMILY: Relationships and social interactions
 * - PERSONAL_IMPROVEMENT: Health, learning, and self-development
 */
enum class HabitGroup(
    val displayName: String,
    val icon: String,
    val colorCode: String,
    val description: String,
    val keywords: List<String>
) {
    RELIGIOUS(
        "Religious", 
        "🕌", 
        "#8E24AA", 
        "Spiritual and religious practices",
        listOf("quran", "prayer", "salah", "dua", "islamic", "mosque", "fasting", "hajj", "ramadan", "spiritual", "meditation")
    ),
    
    CAREER_WORK(
        "Career & Work", 
        "💼", 
        "#1976D2", 
        "Professional development and work tasks",
        listOf("work", "job", "career", "project", "task", "meeting", "study", "learn", "skill", "course", "training", "business", "professional")
    ),
    
    SOCIAL_FAMILY(
        "Social & Family", 
        "👨‍👩‍👧‍👦", 
        "#388E3C", 
        "Family time and social relationships",
        listOf("family", "social", "friend", "relationship", "call", "visit", "date", "gathering", "party", "community", "volunteer", "help")
    ),
    
    PERSONAL_IMPROVEMENT(
        "Personal Improvement", 
        "🌟", 
        "#F57C00", 
        "Health, fitness, and self-development",
        listOf("exercise", "health", "fitness", "diet", "nutrition", "sleep", "read", "book", "hobby", "creative", "art", "music", "journal", "mindfulness")
    );

    companion object {
        /**
         * Auto-detect habit group based on habit name pattern matching
         */
        fun getRecommendedGroup(habitName: String): HabitGroup {
            val name = habitName.lowercase().trim()
            
            // Religious patterns (highest priority for Muslim users)
            if (RELIGIOUS.keywords.any { name.contains(it) }) {
                return RELIGIOUS
            }
            
            // Career & Work patterns
            if (CAREER_WORK.keywords.any { name.contains(it) }) {
                return CAREER_WORK
            }
            
            // Social & Family patterns
            if (SOCIAL_FAMILY.keywords.any { name.contains(it) }) {
                return SOCIAL_FAMILY
            }
            
            // Personal Improvement patterns (default for health/learning)
            if (PERSONAL_IMPROVEMENT.keywords.any { name.contains(it) }) {
                return PERSONAL_IMPROVEMENT
            }
            
            // Default to Personal Improvement for unknown habits
            return PERSONAL_IMPROVEMENT
        }

        /**
         * Get group distribution statistics
         */
        fun getGroupDistribution(habits: List<Pair<HabitGroup, Double>>): Map<HabitGroup, GroupStats> {
            val distribution = mutableMapOf<HabitGroup, GroupStats>()
            
            values().forEach { group ->
                val groupHabits = habits.filter { it.first == group }
                val count = groupHabits.size
                val averageScore = if (count > 0) {
                    groupHabits.sumOf { it.second } / count
                } else 0.0
                
                distribution[group] = GroupStats(
                    count = count,
                    averageSuccessRate = averageScore,
                    totalScore = groupHabits.sumOf { it.second }
                )
            }
            
            return distribution
        }

        /**
         * Calculate group performance scores with weighting
         */
        fun calculateGroupPerformance(
            habits: List<Triple<HabitGroup, HabitPriority, Double>>
        ): Map<HabitGroup, GroupPerformance> {
            val performance = mutableMapOf<HabitGroup, GroupPerformance>()
            
            values().forEach { group ->
                val groupHabits = habits.filter { it.first == group }
                
                if (groupHabits.isNotEmpty()) {
                    val totalWeight = groupHabits.sumOf { it.second.weight }
                    val weightedScore = groupHabits.sumOf { (_, priority, score) -> 
                        priority.weight * score 
                    }
                    
                    val avgWeightedScore = if (totalWeight > 0) weightedScore / totalWeight else 0.0
                    val simpleAverage = groupHabits.sumOf { it.third } / groupHabits.size
                    
                    performance[group] = GroupPerformance(
                        habitCount = groupHabits.size,
                        simpleAverage = simpleAverage,
                        weightedAverage = avgWeightedScore,
                        totalWeight = totalWeight,
                        priorityDistribution = groupHabits.groupBy { it.second }.mapValues { it.value.size }
                    )
                } else {
                    performance[group] = GroupPerformance(
                        habitCount = 0,
                        simpleAverage = 0.0,
                        weightedAverage = 0.0,
                        totalWeight = 0.0,
                        priorityDistribution = emptyMap()
                    )
                }
            }
            
            return performance
        }
    }
}

/**
 * Basic group statistics
 */
data class GroupStats(
    val count: Int,
    val averageSuccessRate: Double,
    val totalScore: Double
)

/**
 * Advanced group performance metrics with priority weighting
 */
data class GroupPerformance(
    val habitCount: Int,
    val simpleAverage: Double,
    val weightedAverage: Double,
    val totalWeight: Double,
    val priorityDistribution: Map<HabitPriority, Int>
) {
    /**
     * Get performance grade based on weighted average
     */
    fun getPerformanceGrade(): String {
        return when {
            weightedAverage >= 0.9 -> "A+"
            weightedAverage >= 0.85 -> "A"
            weightedAverage >= 0.8 -> "A-"
            weightedAverage >= 0.75 -> "B+"
            weightedAverage >= 0.7 -> "B"
            weightedAverage >= 0.65 -> "B-"
            weightedAverage >= 0.6 -> "C+"
            weightedAverage >= 0.55 -> "C"
            weightedAverage >= 0.5 -> "C-"
            weightedAverage >= 0.4 -> "D"
            else -> "F"
        }
    }
    
    /**
     * Get status indicator
     */
    fun getStatusIcon(): String {
        return when (getPerformanceGrade()) {
            "A+", "A", "A-" -> "🌟"
            "B+", "B", "B-" -> "✅"
            "C+", "C", "C-" -> "⚠️"
            "D" -> "🔸"
            else -> "❌"
        }
    }
}
