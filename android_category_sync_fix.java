/**
 * Android Category Sync Fix for uHabits
 * 
 * This sample code demonstrates how to fix category values in the Android app
 * and ensure they are correctly synced to AWS.
 */

// Step 1: Add standard category constants to your Habit model class
public class Habit {
    // Standard category constants
    public static final String CATEGORY_RELIGIOUS = "Religious";
    public static final String CATEGORY_CAREER = "Career & Work";
    public static final String CATEGORY_SOCIAL = "Social & Family";
    public static final String CATEGORY_PERSONAL = "Personal Improvement";
    
    // Existing properties
    private String category;
    // ... other properties
    
    // Getters and setters
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    // New method to standardize categories
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
}

// Step 2: Add a category migration function to your HabitList manager
public class HabitList {
    // ... existing code
    
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
                
                // Log for debugging
                Log.d("uHabits", "Updated habit category: " + habit.getName() + 
                      " from " + oldCategory + " to " + habit.getCategory());
            }
        }
        
        if (hasChanges) {
            // Trigger cloud sync after migration
            triggerCloudSync();
        }
    }
    
    // ... existing code
}

// Step 3: Update the habit creation/edit UI to use standard categories
public class EditHabitActivity extends AppCompatActivity {
    // ... existing code
    
    private Spinner categorySpinner;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // ... existing setup code
        
        // Setup category dropdown
        setupCategorySpinner();
        
        // ... more existing code
    }
    
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
        
        // Set the current category for this habit if editing
        if (habit != null && habit.getCategory() != null) {
            for (int i = 0; i < categories.length; i++) {
                if (categories[i].equals(habit.getCategory())) {
                    categorySpinner.setSelection(i);
                    break;
                }
            }
        }
    }
    
    private void saveHabit() {
        // ... existing code
        
        // Save the selected category
        String selectedCategory = (String) categorySpinner.getSelectedItem();
        habit.setCategory(selectedCategory);
        
        // ... more existing save code
    }
    
    // ... existing code
}

// Step 4: Update your cloud sync code to ensure categories are standardized
public class HabitSyncManager {
    // ... existing code
    
    /**
     * Prepares habit data for syncing to AWS
     */
    private JsonObject habitToJson(Habit habit) {
        JsonObject json = new JsonObject();
        
        // Ensure category is standardized before syncing
        habit.standardizeCategory();
        
        json.addProperty("name", habit.getName());
        json.addProperty("category", habit.getCategory());
        // ... other properties
        
        return json;
    }
    
    /**
     * Process incoming habit data from AWS
     */
    private Habit jsonToHabit(JsonObject json) {
        Habit habit = new Habit();
        
        // ... existing property mapping
        
        // Ensure incoming category is standardized
        if (json.has("category")) {
            habit.setCategory(json.get("category").getAsString());
            habit.standardizeCategory();
        } else {
            habit.setCategory(Habit.CATEGORY_PERSONAL);
        }
        
        return habit;
    }
    
    // ... existing code
}

// Step 5: Add migration call to your application class
public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        
        // ... existing initialization
        
        // Check if this is a new version that needs migration
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        int lastVersion = prefs.getInt("last_version", 0);
        int currentVersion = BuildConfig.VERSION_CODE;
        
        if (currentVersion > lastVersion) {
            // Run migrations for new version
            migrateData(lastVersion, currentVersion);
            
            // Update stored version
            prefs.edit().putInt("last_version", currentVersion).apply();
        }
    }
    
    private void migrateData(int fromVersion, int toVersion) {
        // Get habit list from your dependency injection or singleton
        HabitList habitList = HabitList.getInstance();
        
        // Run category standardization
        habitList.migrateHabitCategories();
        
        // Add debug log
        Log.d("uHabits", "Migration completed from v" + fromVersion + " to v" + toVersion);
    }
}

// Step 6: Add debug logging to verify categories are being synced correctly
public class CloudSyncDebugger {
    /**
     * Call this before and after sync to verify category values
     */
    public static void logCategoryInfo(HabitList habitList) {
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
}
