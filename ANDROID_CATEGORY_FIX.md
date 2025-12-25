# Android Category Standardization Guide for uHabits

This guide provides instructions for standardizing habit categories in the Android app.

## Problem Overview

The habit categories in the Android app may not be using consistent values, causing all habits to appear in one category (Personal Improvement) in the dashboard.

## Steps to Standardize Categories

### 1. Find the Habit Model Class

Look for the model class representing habits in the Android app. Common locations:
- `uhabits-core/src/main/java/org/isoron/uhabits/core/models/Habit.java`
- `uhabits-android/src/main/java/org/isoron/uhabits/models/Habit.java`

### 2. Check for Category Field

Look for the field that stores category information:

```java
public class Habit {
    // Possible field names:
    private String category;
    private String type;
    private String group;
    private int categoryId;
    
    // Corresponding getters and setters
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
}
```

### 3. Create Category Constants

Add standard category constants to the Habit class:

```java
public class Habit {
    // Standard category constants
    public static final String CATEGORY_RELIGIOUS = "Religious";
    public static final String CATEGORY_CAREER = "Career & Work";
    public static final String CATEGORY_SOCIAL = "Social & Family";
    public static final String CATEGORY_PERSONAL = "Personal Improvement";
    
    // Category field
    private String category;
    
    // Rest of the class...
}
```

### 4. Update UI Components

Look for activities or fragments that handle habit creation or editing. Common locations:
- `uhabits-android/src/main/java/org/isoron/uhabits/activities/habits/edit/`
- `uhabits-android/src/main/java/org/isoron/uhabits/ui/habits/`

Update dropdown menus or radio buttons to use the standard categories:

```java
// Example dropdown setup
private void setupCategorySpinner() {
    String[] categories = {
        Habit.CATEGORY_RELIGIOUS,
        Habit.CATEGORY_CAREER,
        Habit.CATEGORY_SOCIAL,
        Habit.CATEGORY_PERSONAL
    };
    
    ArrayAdapter<String> adapter = new ArrayAdapter<>(
        this, 
        android.R.layout.simple_spinner_item, 
        categories
    );
    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    categorySpinner.setAdapter(adapter);
}
```

### 5. Add Migration Code

For existing habits, add migration code that runs during app update:

```java
public void migrateCategories() {
    List<Habit> habits = habitList.getAll();
    
    for (Habit habit : habits) {
        String currentCategory = habit.getCategory();
        String standardizedCategory = standardizeCategory(currentCategory);
        
        if (!currentCategory.equals(standardizedCategory)) {
            habit.setCategory(standardizedCategory);
            habitList.update(habit);
        }
    }
}

private String standardizeCategory(String category) {
    if (category == null) return Habit.CATEGORY_PERSONAL;
    
    String lowerCategory = category.toLowerCase();
    
    if (lowerCategory.contains("religious") || 
        lowerCategory.contains("pray") || 
        lowerCategory.contains("faith")) {
        return Habit.CATEGORY_RELIGIOUS;
    }
    else if (lowerCategory.contains("career") || 
             lowerCategory.contains("work") || 
             lowerCategory.contains("job")) {
        return Habit.CATEGORY_CAREER;
    }
    else if (lowerCategory.contains("social") || 
             lowerCategory.contains("family") || 
             lowerCategory.contains("friend")) {
        return Habit.CATEGORY_SOCIAL;
    }
    else {
        return Habit.CATEGORY_PERSONAL;
    }
}
```

### 6. Update Database Handlers

If the app uses SQLite, update database handlers to use standardized categories:

```java
// When saving habits to the database
public void insert(Habit habit) {
    // Ensure category is standardized
    if (habit.getCategory() == null) {
        habit.setCategory(Habit.CATEGORY_PERSONAL);
    }
    
    // Rest of the insertion code...
}
```

### 7. Update Cloud Sync Code

If the app syncs with AWS, find the sync code and update it to use standardized categories:

```java
// When preparing habits for cloud sync
private JsonObject habitToJson(Habit habit) {
    JsonObject json = new JsonObject();
    
    // Ensure category is standardized before syncing
    String standardCategory = standardizeCategory(habit.getCategory());
    
    json.addProperty("name", habit.getName());
    json.addProperty("category", standardCategory);
    // Other properties...
    
    return json;
}
```

### 8. Testing

Test all scenarios to ensure categories are correctly stored and synced:
1. Create new habits with different categories
2. Edit existing habits and change categories
3. Verify categories are correct in the database
4. Verify categories are correct after syncing with the cloud

## Troubleshooting

If you're still having issues:

1. Add logging to track how categories are set and retrieved
2. Check the API endpoint responses to see how categories are being transmitted
3. Verify the dashboard is correctly interpreting the category values

For advanced debugging, add this code to log category information:

```java
public void logCategoryInfo() {
    Log.d("uHabits", "==== Category Information ====");
    
    List<Habit> habits = habitList.getAll();
    Map<String, Integer> categoryCounts = new HashMap<>();
    
    for (Habit habit : habits) {
        String category = habit.getCategory();
        Log.d("uHabits", "Habit: " + habit.getName() + ", Category: " + category);
        
        categoryCounts.put(category, categoryCounts.getOrDefault(category, 0) + 1);
    }
    
    Log.d("uHabits", "==== Category Distribution ====");
    for (Map.Entry<String, Integer> entry : categoryCounts.entrySet()) {
        Log.d("uHabits", entry.getKey() + ": " + entry.getValue());
    }
}
```
