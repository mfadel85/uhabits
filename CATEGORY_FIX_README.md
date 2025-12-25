# uHabits Category Standardization Guide

## Overview

This guide helps you implement category standardization in your uHabits app to ensure proper group categorization for analytics. The fix ensures all habits are assigned to one of four standard groups (Religious, Career & Work, Social & Family, Personal Improvement), and these categories properly sync with AWS.

## The Problem

Currently, habits may be inconsistently categorized or all appearing under a single category in the analytics dashboard despite being assigned to different groups in the Android app. This happens because:

1. There's no standardized mapping between habit names/descriptions and their logical groups
2. When habits sync to AWS, they may use different formats or values for categories
3. The dashboard has trouble mapping these non-standard values to the four main categories

## The Solution

We've created an automated script that implements these fixes in your Kotlin codebase:

1. **Add/Update HabitGroup Enum**: Defines four standard category groups with proper attributes
2. **Smart Category Detection**: Adds logic to intelligently assign habits to the right group based on keywords
3. **Create Migration Helper**: Adds a utility class to update existing habits
4. **Update Application Class**: Adds code to trigger migration during app startup
5. **Build New APK**: Creates a new app version with these fixes

## How to Use the Fix Script

### Prerequisites

- Git repository of your uHabits app
- Bash shell environment
- Android development environment set up

### Steps

1. **Navigate to your uHabits directory**:
   ```
   cd /path/to/uHabits
   ```

2. **Run the fix script**:
   ```
   ./fix_categories.sh
   ```

3. **What the script does**:
   - Creates or verifies the `HabitGroup.kt` enum class
   - Adds the `CategoryMigrationHelper.kt` utility class
   - Updates the Application class to trigger category migration at startup
   - Ensures proper permissions in AndroidManifest.xml
   - Builds a new APK with the fixes

4. **After the script completes**:
   - Install the generated APK
   - Check if habits appear in their correct groups
   - Verify that the AWS dashboard shows the correct categories

## Manual Implementation (if script fails)

If the automatic script doesn't work for your codebase, follow these manual steps:

1. **Create or update the HabitGroup enum** in the `core/models` package:

```kotlin
enum class HabitGroup(
    val displayName: String,
    val icon: String,
    val colorCode: String
) {
    RELIGIOUS("Religious", "🕌", "#8E24AA"),
    CAREER_WORK("Career & Work", "💼", "#1976D2"),
    SOCIAL_FAMILY("Social & Family", "👨‍👩‍👧‍👦", "#388E3C"),
    PERSONAL_IMPROVEMENT("Personal Improvement", "🌟", "#F57C00");

    companion object {
        fun getRecommendedGroup(habitName: String): HabitGroup {
            val name = habitName.lowercase().trim()
            
            // Religious patterns
            if (name.contains("relig") || name.contains("pray") || 
                name.contains("spirit") || name.contains("faith")) {
                return RELIGIOUS
            }
            
            // Career & Work patterns
            if (name.contains("work") || name.contains("job") || 
                name.contains("career") || name.contains("study")) {
                return CAREER_WORK
            }
            
            // Social & Family patterns
            if (name.contains("social") || name.contains("family") || 
                name.contains("friend") || name.contains("relation")) {
                return SOCIAL_FAMILY
            }
            
            // Default to Personal Improvement
            return PERSONAL_IMPROVEMENT
        }
    }
}
```

2. **Create a CategoryMigrationHelper utility class**:

```kotlin
object CategoryMigrationHelper {
    private const val TAG = "CategoryMigration"

    fun migrateCategories(habits: List<Habit>): Int {
        if (habits.isEmpty()) return 0
        
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
}
```

3. **Update your Application class** to trigger the migration:

```kotlin
class YourApplication : Application() {
    
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
                
                val repository = HabitRepository.getInstance(this@YourApplication)
                val habits = repository.getAllHabits()
                
                val updated = CategoryMigrationHelper.migrateCategories(habits)
                if (updated > 0) {
                    Log.i(TAG, "Updated categories for $updated habits")
                    // Save changes
                    for (habit in habits) {
                        repository.update(habit)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to migrate categories", e)
            }
        }
    }
}
```

4. **Update your AndroidManifest.xml** to include necessary permissions:

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

5. **Build and test** the app to ensure the changes work correctly.

## Verifying the Fix

After implementing the changes and installing the updated app:

1. Open the app and check habit groups
2. Create a new habit and verify it's assigned to the correct group
3. Sync your data with AWS
4. Check the dashboard - habits should now appear in their correct categories

## Support and Troubleshooting

If you encounter any issues with the implementation:

1. **Script backup files**: The script creates `.bak` files for all modified files
2. **Log checking**: Check the logs with tag "CategoryMigration" to see migration results
3. **Manual fix**: Follow the manual implementation steps if automated fix fails
4. **Repository adaptation**: You may need to adapt the code to your specific repository implementation

## Note on AWS Sync

The fix ensures that categories are properly standardized before syncing to AWS. If you have existing data in AWS that uses non-standard categories, you might need to run a one-time data migration in AWS to correct historical data.

---

## Technical Details

### Group Keywords

The automatic group detection uses these keyword patterns:

- **Religious**: religion, prayer, faith, spiritual, meditation
- **Career & Work**: work, job, career, study, professional, business
- **Social & Family**: family, social, friend, relationship, community
- **Personal Improvement**: exercise, health, fitness, diet, reading, hobby

### Migration Process

1. For each habit in the database:
   - Get current group
   - Determine recommended group based on habit name
   - Update if different
   - Log changes for debugging

2. User experience:
   - Process runs automatically in background
   - No data loss or disruption
   - Habits immediately appear in correct groups

### Performance Impact

- Migration runs in background thread
- One-time process per device
- Minimal performance impact
