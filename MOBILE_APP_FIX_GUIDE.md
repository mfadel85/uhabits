# Mobile-First Category Fix Guide

This guide focuses on fixing the category issue directly in the Android app, which will then propagate to AWS when data is synced.

## Overview of the Issue

The problem occurs because habit categories are not standardized in the Android app:
- Categories might use different formats, capitalizations, or spellings
- Categories might be missing or default to a single value
- When synced to AWS, these inconsistent categories cause dashboard display issues

## Fix Approach

Instead of fixing data in AWS directly (which would be overwritten by the app's next sync), we'll:

1. Standardize categories in the Android app
2. Add migration code to update existing habits
3. Ensure all future habits use standard categories
4. Let the app sync the fixed data to AWS automatically

## Implementation Steps

### 1. Locate the Habit Model

First, find the `Habit` class in your Android project. Common locations:
- `uhabits-core/src/main/java/org/isoron/uhabits/core/models/Habit.java`
- `uhabits-android/src/main/java/org/isoron/uhabits/models/Habit.java`

### 2. Add Standard Category Constants

Add these constants to your `Habit` class:

```java
// Standard category constants
public static final String CATEGORY_RELIGIOUS = "Religious";
public static final String CATEGORY_CAREER = "Career & Work";
public static final String CATEGORY_SOCIAL = "Social & Family";
public static final String CATEGORY_PERSONAL = "Personal Improvement";
```

### 3. Add a Category Standardization Method

Add this method to the `Habit` class:

```java
/**
 * Standardizes the category value to use one of the four standard categories
 */
public void standardizeCategory() {
    if (category == null) {
        category = CATEGORY_PERSONAL; // Default category
        return;
    }
    
    String lowerCategory = category.toLowerCase();
    
    // Map various formats to standard categories
    if (lowerCategory.contains("religi") || lowerCategory.contains("pray") || 
        lowerCategory.contains("faith") || lowerCategory.contains("spirit")) {
        category = CATEGORY_RELIGIOUS;
    } 
    else if (lowerCategory.contains("career") || lowerCategory.contains("work") || 
            lowerCategory.contains("job") || lowerCategory.contains("profess")) {
        category = CATEGORY_CAREER;
    }
    else if (lowerCategory.contains("social") || lowerCategory.contains("family") || 
            lowerCategory.contains("friend") || lowerCategory.contains("relation")) {
        category = CATEGORY_SOCIAL;
    }
    else {
        category = CATEGORY_PERSONAL;
    }
}
```

### 4. Add Migration Code

Find or create a class that manages habits (often called `HabitList` or `HabitManager`), and add this migration method:

```java
/**
 * Migrates all habits to use standardized categories
 * Call this method once during app startup after a version update
 */
public void migrateHabitCategories() {
    List<Habit> allHabits = getAll();
    boolean hasChanges = false;
    
    for (Habit habit : allHabits) {
        String oldCategory = habit.getCategory();
        habit.standardizeCategory();
        
        // If category changed, update the habit
        if (!habit.getCategory().equals(oldCategory)) {
            update(habit);
            hasChanges = true;
        }
    }
    
    if (hasChanges) {
        // Trigger cloud sync after migration
        triggerCloudSync();
    }
}
```

### 5. Update the UI Components

Find activities or fragments where habits are created or edited. Update the category selection UI:

```java
private void setupCategorySpinner() {
    categorySpinner = findViewById(R.id.category_spinner);
    
    // Use the standard categories from Habit class
    String[] categories = {
        Habit.CATEGORY_RELIGIOUS,
        Habit.CATEGORY_CAREER,
        Habit.CATEGORY_SOCIAL,
        Habit.CATEGORY_PERSONAL
    };
    
    ArrayAdapter<String> adapter = new ArrayAdapter<>(
        this, android.R.layout.simple_spinner_item, categories);
    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    categorySpinner.setAdapter(adapter);
}
```

### 6. Update Cloud Sync Code

Find the code that handles syncing with AWS and ensure it uses standardized categories:

```java
// When preparing habits for cloud sync
private JsonObject habitToJson(Habit habit) {
    JsonObject json = new JsonObject();
    
    // Ensure category is standardized before syncing
    habit.standardizeCategory();
    
    json.addProperty("name", habit.getName());
    json.addProperty("category", habit.getCategory());
    // ... other properties
    
    return json;
}
```

### 7. Trigger Migration on App Update

Find your `Application` class (or main activity) and add code to trigger the migration:

```java
// In your Application class onCreate() or similar initialization point
SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
int lastVersion = prefs.getInt("last_version", 0);
int currentVersion = BuildConfig.VERSION_CODE;

if (currentVersion > lastVersion) {
    // Run category migration
    habitList.migrateHabitCategories();
    
    // Update stored version
    prefs.edit().putInt("last_version", currentVersion).apply();
}
```

### 8. Test the Fix

1. Install the updated app on your device
2. Check the logs to verify categories are standardized
3. Create or edit a habit and verify it uses a standard category
4. Trigger a sync and check if AWS data is updated correctly
5. Refresh the dashboard to see if habits appear in the correct categories

## Additional Tips

### Add Debug Logging

Add this code to verify categories are being synced correctly:

```java
// Before and after sync
Log.d("uHabits", "==== Category Information ====");
List<Habit> habits = habitList.getAll();
for (Habit habit : habits) {
    Log.d("uHabits", "Habit: " + habit.getName() + ", Category: " + habit.getCategory());
}
```

### Force a Full Sync

If needed, you can add a button in your app's debug menu to force a full sync:

```java
private void forceSyncAllHabits() {
    // First standardize all categories
    List<Habit> allHabits = habitList.getAll();
    for (Habit habit : allHabits) {
        habit.standardizeCategory();
        habitList.update(habit);
    }
    
    // Then trigger sync
    triggerCloudSync();
}
```

## Reference Implementation

A complete reference implementation is available in the file `android_category_sync_fix.java` in this repository.

## After Implementation

Once you've implemented these changes and released an update:
1. Users will get their habits automatically migrated to standard categories
2. Their data will be synced correctly to AWS
3. The dashboard will display habits in the correct categories

This is a permanent solution as it fixes the issue at the source.
