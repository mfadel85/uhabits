package org.isoron.uhabits.activities.analytics

import android.widget.Toast
import org.isoron.uhabits.core.models.HabitList
import org.isoron.uhabits.core.models.HabitGroup

class HabitGroupManager(
    private val habitList: HabitList
) {
    
    /**
     * Assign groups to habits based on name patterns (for existing habits without groups)
     * New habits will have groups manually selected by users
     */
    fun autoAssignGroups(): Int {
        var assignedCount = 0
        
        for (i in 0 until habitList.size()) {
            val habit = habitList.getByPosition(i)
            val recommendedGroup = HabitGroup.getRecommendedGroup(habit.name)
            
            // Only update if group is different from current
            if (habit.group != recommendedGroup) {
                habit.group = recommendedGroup
                assignedCount++
            }
        }
        
        return assignedCount
    }
    
    /**
     * Get group distribution statistics
     */
    fun getGroupDistribution(): Map<HabitGroup, Int> {
        val distribution = mutableMapOf<HabitGroup, Int>()
        
        // Initialize all groups
        HabitGroup.values().forEach { group ->
            distribution[group] = 0
        }
        
        // Count habits in each group
        for (i in 0 until habitList.size()) {
            val habit = habitList.getByPosition(i)
            if (!habit.isArchived) {
                val group = habit.group ?: HabitGroup.PERSONAL_IMPROVEMENT
                distribution[group] = distribution[group]!! + 1
            }
        }
        
        return distribution
    }
    
    /**
     * Get group performance analytics
     */
    fun getGroupPerformanceText(): String {
        val groups = mutableMapOf<HabitGroup, MutableList<Pair<String, Double>>>()
        
        // Initialize group lists
        HabitGroup.values().forEach { group ->
            groups[group] = mutableListOf()
        }
        
        // Categorize habits by group
        for (i in 0 until habitList.size()) {
            val habit = habitList.getByPosition(i)
            if (!habit.isArchived) {
                val group = habit.group ?: HabitGroup.PERSONAL_IMPROVEMENT
                val today = org.isoron.uhabits.core.utils.DateUtils.getTodayWithOffset()
                val score = habit.scores[today].value
                groups[group]?.add(Pair(habit.name, score))
            }
        }
        
        val stats = StringBuilder()
        stats.append("📊 Habit Group Performance:\n\n")
        
        groups.forEach { (group, habits) ->
            if (habits.isNotEmpty()) {
                val avgScore = habits.sumOf { it.second } / habits.size
                val gradeColor = when {
                    avgScore >= 0.85 -> "🌟"
                    avgScore >= 0.7 -> "✅"
                    avgScore >= 0.55 -> "⚠️"
                    else -> "❌"
                }
                
                stats.append("${group.icon} ${group.displayName}: ${habits.size} habits $gradeColor\n")
                stats.append("   Average Score: ${"%.1f".format(avgScore * 100)}%\n")
                
                // Show top 3 habits
                habits.sortedByDescending { it.second }.take(3).forEach { (name, score) ->
                    stats.append("   • $name: ${"%.0f".format(score * 100)}%\n")
                }
                if (habits.size > 3) {
                    stats.append("   • ... and ${habits.size - 3} more\n")
                }
                stats.append("\n")
            }
        }
        
        return stats.toString()
    }
    
    /**
     * Get recommendations for group optimization
     */
    fun getGroupRecommendations(): List<String> {
        val recommendations = mutableListOf<String>()
        val distribution = getGroupDistribution()
        
        // Check for empty groups
        distribution.forEach { (group, count) ->
            if (count == 0) {
                recommendations.add("Consider adding habits to ${group.displayName} group ${group.icon}")
            }
        }
        
        // Check for unbalanced distribution
        val totalHabits = distribution.values.sum()
        if (totalHabits > 0) {
            distribution.forEach { (group, count) ->
                val percentage = (count.toDouble() / totalHabits) * 100
                if (percentage > 60) {
                    recommendations.add("${group.displayName} group has many habits (${percentage.toInt()}%) - consider balancing")
                }
            }
        }
        
        return recommendations
    }
}
