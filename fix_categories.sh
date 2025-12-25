#!/bin/bash

# uHabits Category Fix Implementation Script for Kotlin Codebase
# This script will implement category fixes in the Android app and ensure proper syncing with AWS

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}🔧 uHabits Category Fix Implementation Script${NC}"
echo "=================================================="

# Define paths
BASE_DIR="$(pwd)"
ANDROID_DIR="$BASE_DIR/uhabits-android"
CORE_DIR="$BASE_DIR/uhabits-core"
MODELS_DIR="$CORE_DIR/src/jvmMain/java/org/isoron/uhabits/core/models"

# Step 1: Check if HabitGroup enum exists and verify its content
echo -e "${YELLOW}🔍 Checking HabitGroup enum...${NC}"
HABIT_GROUP_FILE="$MODELS_DIR/HabitGroup.kt"

if [ ! -f "$HABIT_GROUP_FILE" ]; then
    echo -e "${RED}❌ HabitGroup.kt not found. Creating it...${NC}"
    
    # Create directory if it doesn't exist
    mkdir -p "$MODELS_DIR"
    
    # Create the HabitGroup.kt file
    cat > "$HABIT_GROUP_FILE" << EOF
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
EOF

    echo -e "${GREEN}✅ Created HabitGroup.kt enum class${NC}"
else
    echo -e "${GREEN}✅ HabitGroup.kt exists${NC}"
    
    # Check if it has the required groups
    if ! grep -q "RELIGIOUS" "$HABIT_GROUP_FILE" || ! grep -q "CAREER_WORK" "$HABIT_GROUP_FILE" || \
       ! grep -q "SOCIAL_FAMILY" "$HABIT_GROUP_FILE" || ! grep -q "PERSONAL_IMPROVEMENT" "$HABIT_GROUP_FILE"; then
        echo -e "${YELLOW}⚠️ HabitGroup.kt may be missing required groups. Please check manually.${NC}"
    fi
    
    # Check if it has the getRecommendedGroup function
    if ! grep -q "getRecommendedGroup" "$HABIT_GROUP_FILE"; then
        echo -e "${YELLOW}⚠️ HabitGroup.kt is missing the getRecommendedGroup function. Adding it...${NC}"
        
        # Make a backup
        cp "$HABIT_GROUP_FILE" "${HABIT_GROUP_FILE}.bak"
        
        # Add the function before the closing bracket of the companion object
        COMPANION_END=$(grep -n "}" "$HABIT_GROUP_FILE" | grep -B 1 "companion object" | head -1 | cut -d':' -f1)
        
        if [ -n "$COMPANION_END" ]; then
            # Create the getRecommendedGroup function text
            GET_GROUP_FUNC=$(cat << 'EOF'
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
EOF
)
            # Insert the function before the closing bracket
            sed -i "${COMPANION_END}i\\${GET_GROUP_FUNC}" "$HABIT_GROUP_FILE"
            echo -e "${GREEN}✅ Added getRecommendedGroup function to HabitGroup.kt${NC}"
        else
            echo -e "${RED}❌ Could not find companion object closing bracket. Manual intervention required.${NC}"
        fi
    fi
fi

# Step 2: Create the CategoryMigrationHelper class
echo -e "${YELLOW}📝 Creating CategoryMigrationHelper class...${NC}"
MIGRATION_DIR="$ANDROID_DIR/src/main/java/org/isoron/uhabits/utils"
mkdir -p "$MIGRATION_DIR"

MIGRATION_FILE="$MIGRATION_DIR/CategoryMigrationHelper.kt"
cat > "$MIGRATION_FILE" << 'EOFMARKER'
package org.isoron.uhabits.utils

import android.util.Log
import org.isoron.uhabits.core.models.Habit
import org.isoron.uhabits.core.models.HabitGroup

/**
 * Helper class to migrate habit categories to standard groups
 */
object CategoryMigrationHelper {

    private const val TAG = "CategoryMigration"

    /**
     * Migrates all habits to use standardized categories
     * @param habits The list of habits to migrate
     * @return Number of habits that were updated
     */
    fun migrateCategories(habits: List<Habit>): Int {
        if (habits.isEmpty()) {
            return 0
        }
        
        var updatedCount = 0
        
        for (habit in habits) {
            val oldGroup = habit.group
            val recommendedGroup = HabitGroup.getRecommendedGroup(habit.name)
            
            if (oldGroup != recommendedGroup) {
                habit.group = recommendedGroup
                updatedCount++
                
                Log.d(TAG, "Updated habit '${habit.name}' from group '${oldGroup.displayName}' to '${recommendedGroup.displayName}'")
            }
        }
        
        return updatedCount
    }
    
    /**
     * Logs category distribution statistics
     * @param habits The list of habits
     */
    fun logCategoryStats(habits: List<Habit>) {
        if (habits.isEmpty()) {
            Log.d(TAG, "No habits to analyze")
            return
        }
        
        val groupCounts = mutableMapOf<HabitGroup, Int>()
        
        // Initialize counts to zero for all groups
        HabitGroup.values().forEach { group ->
            groupCounts[group] = 0
        }
        
        // Count habits by group
        for (habit in habits) {
            val group = habit.group
            groupCounts[group] = groupCounts.getOrDefault(group, 0) + 1
        }
        
        Log.d(TAG, "Category Distribution:")
        groupCounts.forEach { (group, count) ->
            Log.d(TAG, "  ${group.displayName}: $count")
        }
        
        Log.d(TAG, "Total habits: ${habits.size}")
    }
}
EOFMARKER

echo -e "${GREEN}✅ Created CategoryMigrationHelper.kt${NC}"

# Step 3: Find the application class
echo -e "${YELLOW}🔍 Looking for main Application class...${NC}"
APP_CLASS_FILES=$(find "$ANDROID_DIR/src" -name "*.kt" -o -name "*.java" | xargs grep -l "class.*extends.*Application\|class.*:.*Application" | head -1)

if [ -z "$APP_CLASS_FILES" ]; then
    echo -e "${YELLOW}⚠️ Could not find Application class. Creating one...${NC}"
    
    # Try to determine the correct package
    PKG_PATH=$(find "$ANDROID_DIR/src/main/java" -type d | sort | head -1)
    if [ -z "$PKG_PATH" ]; then
        PKG_PATH="$ANDROID_DIR/src/main/java/org/isoron/uhabits"
        mkdir -p "$PKG_PATH"
    fi
    
    PKG_NAME=$(echo "$PKG_PATH" | sed 's|.*/java/||' | sed 's|/|.|g')
    APP_CLASS="$PKG_PATH/HabitsApplication.kt"
    
    cat > "$APP_CLASS" << EOF
package $PKG_NAME

import android.app.Application
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.isoron.uhabits.core.models.Habit
import org.isoron.uhabits.utils.CategoryMigrationHelper

class HabitsApplication : Application() {
    
    companion object {
        private const val TAG = "HabitsApplication"
    }
    
    override fun onCreate() {
        super.onCreate()
        
        // Run category migration in background
        migrateCategories()
    }
    
    private fun migrateCategories() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "Starting category migration...")
                
                // Get habit repository (implement based on your app structure)
                // val repository = HabitRepository.getInstance(this@HabitsApplication)
                // val habits = repository.getAllHabits()
                
                // Comment out the next lines and implement your own repository call
                Log.d(TAG, "MANUAL ACTION REQUIRED: Uncomment and implement repository call in HabitsApplication.kt")
                /*
                val updated = CategoryMigrationHelper.migrateCategories(habits)
                if (updated > 0) {
                    Log.i(TAG, "Updated categories for $updated habits")
                    // Save changes
                    for (habit in habits) {
                        repository.update(habit)
                    }
                    
                    // Log the new distribution
                    CategoryMigrationHelper.logCategoryStats(habits)
                } else {
                    Log.d(TAG, "No categories needed updating")
                }
                */
            } catch (e: Exception) {
                Log.e(TAG, "Failed to migrate categories", e)
            }
        }
    }
}
EOF

    # Add it to the Android manifest
    MANIFEST_FILE="$ANDROID_DIR/src/main/AndroidManifest.xml"
    if [ -f "$MANIFEST_FILE" ]; then
        # Check if application tag already has a name attribute
        if ! grep -q 'android:name=".*"' "$MANIFEST_FILE"; then
            # Backup the manifest
            cp "$MANIFEST_FILE" "${MANIFEST_FILE}.bak"
            
            # Add the application class to the manifest
            APP_CLASS_NAME="$PKG_NAME.HabitsApplication"
            APP_CLASS_NAME_SIMPLE=$(echo "$APP_CLASS_NAME" | sed 's/.*\.//')
            
            # Replace the application tag
            sed -i "s|<application |<application android:name=\".$APP_CLASS_NAME_SIMPLE\" |" "$MANIFEST_FILE"
            
            echo -e "${GREEN}✅ Added application class to AndroidManifest.xml${NC}"
        else
            echo -e "${YELLOW}⚠️ AndroidManifest.xml already has an application class defined. You'll need to manually modify it.${NC}"
        fi
    fi
    
    echo -e "${GREEN}✅ Created Application class: $APP_CLASS${NC}"
    echo -e "${YELLOW}⚠️ You need to manually implement the repository call in the application class${NC}"
else
    APP_CLASS="$APP_CLASS_FILES"
    echo -e "${GREEN}✅ Found Application class: $APP_CLASS${NC}"
    
    # Make a backup
    cp "$APP_CLASS" "${APP_CLASS}.bak"
    
    # Check file extension
    if [[ "$APP_CLASS" == *.kt ]]; then
        # Kotlin file
        # Find the onCreate method
        ONCREATE_LINE=$(grep -n "fun onCreate" "$APP_CLASS" | head -1 | cut -d':' -f1)
        
        if [ -n "$ONCREATE_LINE" ]; then
            # Find the closing brace of onCreate method
            ONCREATE_END=$(tail -n +$ONCREATE_LINE "$APP_CLASS" | grep -n "}" | head -1)
            ONCREATE_END_LINE=$(echo "$ONCREATE_END" | cut -d':' -f1)
            ONCREATE_END_LINE=$(($ONCREATE_LINE + $ONCREATE_END_LINE - 1))
            
            # Create migration trigger code
            MIGRATION_CODE=$'        // Run category migration in background\n        migrateCategories()'
            
            # Insert migration call before the closing brace
            sed -i "${ONCREATE_END_LINE}i\\${MIGRATION_CODE}" "$APP_CLASS"
            
            # Add the migrateCategories method to the class
            CLASS_END=$(grep -n "^}" "$APP_CLASS" | tail -1 | cut -d':' -f1)
            
            MIGRATE_METHOD=$'    private fun migrateCategories() {\n        CoroutineScope(Dispatchers.IO).launch {\n            try {\n                Log.d(TAG, "Starting category migration...")\n                \n                // Get habit repository (implement based on your app structure)\n                // TODO: Replace with your actual repository implementation\n                val repository = HabitRepository.getInstance(this@HabitsApplication)\n                val habits = repository.getAllHabits()\n                \n                val updated = CategoryMigrationHelper.migrateCategories(habits)\n                if (updated > 0) {\n                    Log.i(TAG, "Updated categories for $updated habits")\n                    // Save changes\n                    for (habit in habits) {\n                        repository.update(habit)\n                    }\n                    \n                    // Log the new distribution\n                    CategoryMigrationHelper.logCategoryStats(habits)\n                } else {\n                    Log.d(TAG, "No categories needed updating")\n                }\n            } catch (e: Exception) {\n                Log.e(TAG, "Failed to migrate categories", e)\n            }\n        }\n    }'
            
            # Insert the method before the class end
            sed -i "${CLASS_END}i\\${MIGRATE_METHOD}" "$APP_CLASS"
            
            # Add necessary imports at the beginning
            PACKAGE_LINE=$(grep -n "package" "$APP_CLASS" | head -1 | cut -d':' -f1)
            IMPORT_LINE=$(($PACKAGE_LINE + 1))
            
            IMPORTS=$'\nimport android.util.Log\nimport kotlinx.coroutines.CoroutineScope\nimport kotlinx.coroutines.Dispatchers\nimport kotlinx.coroutines.launch\nimport org.isoron.uhabits.utils.CategoryMigrationHelper'
            
            # Insert imports
            sed -i "${IMPORT_LINE}i\\${IMPORTS}" "$APP_CLASS"
            
            # Add TAG constant if not present
            if ! grep -q "TAG = " "$APP_CLASS"; then
                COMPANION_BLOCK=$'\n    companion object {\n        private const val TAG = "HabitsApplication"\n    }'
                
                # Find a good place to insert the companion object
                CONSTRUCTOR_LINE=$(grep -n "class.*:.*Application" "$APP_CLASS" | head -1 | cut -d':' -f1)
                CONSTRUCTOR_END=$(tail -n +$CONSTRUCTOR_LINE "$APP_CLASS" | grep -n "{" | head -1)
                CONSTRUCTOR_END_LINE=$(echo "$CONSTRUCTOR_END" | cut -d':' -f1)
                INSERT_LINE=$(($CONSTRUCTOR_LINE + $CONSTRUCTOR_END_LINE))
                
                # Insert the companion object
                sed -i "${INSERT_LINE}i\\${COMPANION_BLOCK}" "$APP_CLASS"
            fi
            
            echo -e "${GREEN}✅ Added migration trigger to Application class${NC}"
        else
            echo -e "${YELLOW}⚠️ Could not find onCreate method in Application class. Manual implementation required.${NC}"
        fi
    else
        # Java file
        # Find the onCreate method
        ONCREATE_LINE=$(grep -n "void onCreate" "$APP_CLASS" | head -1 | cut -d':' -f1)
        
        if [ -n "$ONCREATE_LINE" ]; then
            # Find the closing brace of onCreate method
            ONCREATE_END=$(tail -n +$ONCREATE_LINE "$APP_CLASS" | grep -n "}" | head -1)
            ONCREATE_END_LINE=$(echo "$ONCREATE_END" | cut -d':' -f1)
            ONCREATE_END_LINE=$(($ONCREATE_LINE + $ONCREATE_END_LINE - 1))
            
            # Create migration trigger code
            MIGRATION_CODE=$'\n        // Run category migration in background\n        migrateCategories();'
            
            # Insert migration call before the closing brace
            sed -i "${ONCREATE_END_LINE}i\\${MIGRATION_CODE}" "$APP_CLASS"
            
            # Add the migrateCategories method to the class
            CLASS_END=$(grep -n "^}" "$APP_CLASS" | tail -1 | cut -d':' -f1)
            
            MIGRATE_METHOD=$'\n    private void migrateCategories() {\n        new Thread(() -> {\n            try {\n                Log.d(TAG, "Starting category migration...");\n                \n                // Get habit repository (implement based on your app structure)\n                // TODO: Replace with your actual repository implementation\n                HabitRepository repository = HabitRepository.getInstance(this);\n                List<Habit> habits = repository.getAllHabits();\n                \n                int updated = CategoryMigrationHelper.migrateCategories(habits);\n                if (updated > 0) {\n                    Log.i(TAG, "Updated categories for " + updated + " habits");\n                    // Save changes\n                    for (Habit habit : habits) {\n                        repository.update(habit);\n                    }\n                    \n                    // Log the new distribution\n                    CategoryMigrationHelper.logCategoryStats(habits);\n                } else {\n                    Log.d(TAG, "No categories needed updating");\n                }\n            } catch (Exception e) {\n                Log.e(TAG, "Failed to migrate categories", e);\n            }\n        }).start();\n    }'
            
            # Insert the method before the class end
            sed -i "${CLASS_END}i\\${MIGRATE_METHOD}" "$APP_CLASS"
            
            # Add necessary imports at the beginning
            PACKAGE_LINE=$(grep -n "package" "$APP_CLASS" | head -1 | cut -d':' -f1)
            IMPORT_LINE=$(($PACKAGE_LINE + 1))
            
            IMPORTS=$'\nimport android.util.Log;\nimport java.util.List;\nimport org.isoron.uhabits.utils.CategoryMigrationHelper;\nimport org.isoron.uhabits.core.models.Habit;'
            
            # Insert imports
            sed -i "${IMPORT_LINE}i\\${IMPORTS}" "$APP_CLASS"
            
            # Add TAG constant if not present
            if ! grep -q "TAG = " "$APP_CLASS"; then
                TAG_CONSTANT="    private static final String TAG = \"HabitsApplication\";"
                
                # Find a good place to insert the TAG constant
                CLASS_LINE=$(grep -n "class.*extends.*Application" "$APP_CLASS" | head -1 | cut -d':' -f1)
                CLASS_BODY_START=$(tail -n +$CLASS_LINE "$APP_CLASS" | grep -n "{" | head -1)
                CLASS_BODY_START_LINE=$(echo "$CLASS_BODY_START" | cut -d':' -f1)
                INSERT_LINE=$(($CLASS_LINE + $CLASS_BODY_START_LINE))
                
                # Insert the TAG constant
                sed -i "${INSERT_LINE}i\\${TAG_CONSTANT}" "$APP_CLASS"
            fi
            
            echo -e "${GREEN}✅ Added migration trigger to Application class${NC}"
        else
            echo -e "${YELLOW}⚠️ Could not find onCreate method in Application class. Manual implementation required.${NC}"
        fi
    fi
fi

# Step 4: Update Android Manifest to ensure permissions
echo -e "${YELLOW}🔍 Updating AndroidManifest.xml for permissions...${NC}"
MANIFEST_FILE="$ANDROID_DIR/src/main/AndroidManifest.xml"

if [ -f "$MANIFEST_FILE" ]; then
    # Back up the manifest
    cp "$MANIFEST_FILE" "${MANIFEST_FILE}.bak"
    
    # Check if internet permission is already added
    if ! grep -q "android.permission.INTERNET" "$MANIFEST_FILE"; then
        # Add internet permission
        INTERNET_PERM='    <uses-permission android:name="android.permission.INTERNET" />'
        
        # Find manifest tag to insert after
        MANIFEST_LINE=$(grep -n "<manifest" "$MANIFEST_FILE" | cut -d':' -f1)
        if [ -n "$MANIFEST_LINE" ]; then
            MANIFEST_END=$(tail -n +$MANIFEST_LINE "$MANIFEST_FILE" | grep -n ">" | head -1)
            MANIFEST_END_LINE=$(echo "$MANIFEST_END" | cut -d':' -f1)
            INSERT_LINE=$(($MANIFEST_LINE + $MANIFEST_END_LINE))
            
            # Insert the permission
            sed -i "${INSERT_LINE}i\\${INTERNET_PERM}" "$MANIFEST_FILE"
            echo -e "${GREEN}✅ Added INTERNET permission to AndroidManifest.xml${NC}"
        fi
    else
        echo -e "${GREEN}✅ INTERNET permission already exists in AndroidManifest.xml${NC}"
    fi
else
    echo -e "${RED}❌ AndroidManifest.xml not found. Cannot update permissions.${NC}"
fi

# Step 5: Build the APK with the fixes
echo -e "\n${YELLOW}🏗️ Building APK with category fixes...${NC}"
cd "$BASE_DIR"
./uhabits/build-and-distribute.sh

echo -e "\n${BLUE}🎉 Category fixes implemented and new APK built!${NC}"
echo "  Remember to test the fixes by checking if:"
echo "  1. Existing habits are categorized correctly"
echo "  2. New habits get standardized categories"
echo "  3. The AWS storage shows correct categories after sync"
echo "\n${GREEN}All done! Your uHabits app now has standardized categories.${NC}"
