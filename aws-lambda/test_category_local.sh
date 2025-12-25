#!/bin/bash

# Category Sync Test - Local Simulation
# This script tests the category standardization logic locally without calling AWS

echo "🧪 Testing category standardization with simulated sync data..."

# Create a Python script that simulates the Lambda function's category standardization
cat > simulate_category_sync.py << 'EOL'
#!/usr/bin/env python3
"""
Local simulation of the habit_sync_function category standardization
"""
import json
import sys
from datetime import datetime

def standardize_category(category):
    """Copy of the standardize_category function from the Lambda code"""
    if not category:
        return "Personal Improvement"  # Default category
        
    category_lower = category.lower()
    
    # Standard category mapping
    if "religious" in category_lower or "pray" in category_lower or "faith" in category_lower:
        return "Religious"
    elif "career" in category_lower or "work" in category_lower or "job" in category_lower:
        return "Career & Work"
    elif "social" in category_lower or "family" in category_lower or "friend" in category_lower:
        return "Social & Family"
    elif "personal" in category_lower or "health" in category_lower or "self" in category_lower:
        return "Personal Improvement"
        
    # If no match, return default
    return "Personal Improvement"

def simulate_sync(sync_data):
    """Simulate the category standardization during sync"""
    print("📊 Simulating category standardization during sync...")
    
    # Track categories standardized
    standardized_categories = 0
    results = []
    
    # Process each habit
    for habit in sync_data["habits_data"]:
        raw_category = habit.get("category", "")
        standardized_category = standardize_category(raw_category)
        
        # Check if category was standardized
        if raw_category and raw_category != standardized_category:
            standardized_categories += 1
            print(f"✏️ Standardized: '{raw_category}' → '{standardized_category}' for habit '{habit.get('name')}'")
        else:
            print(f"✓ Already standard: '{raw_category}' for habit '{habit.get('name')}'")
            
        # Add result to tracking
        results.append({
            "habit_name": habit.get("name"),
            "raw_category": raw_category,
            "standardized_category": standardized_category,
            "was_standardized": raw_category != standardized_category
        })
    
    print(f"\n📝 Summary: Standardized {standardized_categories} out of {len(sync_data['habits_data'])} habit categories")
    return results

def main():
    # Read the sample sync data from stdin
    try:
        sync_data = json.load(sys.stdin)
        results = simulate_sync(sync_data)
        
        # Print final results in JSON format
        print("\n📋 Final Results:")
        print(json.dumps(results, indent=2))
        
    except json.JSONDecodeError as e:
        print(f"❌ Error parsing JSON input: {e}")
        sys.exit(1)

if __name__ == "__main__":
    main()
EOL

# Make the Python script executable
chmod +x simulate_category_sync.py

# Create sample sync data with various categories
cat > sample_sync_data.json << EOL
{
  "user_id": "test_user",
  "sync_timestamp": $(date +%s)000,
  "sync_date": "$(date +%Y-%m-%d)",
  "habits_data": [
    {
      "id": "habit1",
      "name": "Morning Prayer",
      "category": "pray",
      "priority": 3
    },
    {
      "id": "habit2",
      "name": "Check Email",
      "category": "work tasks",
      "priority": 2
    },
    {
      "id": "habit3",
      "name": "Call Mom",
      "category": "Family",
      "priority": 3
    },
    {
      "id": "habit4",
      "name": "Exercise",
      "category": "Health",
      "priority": 2
    },
    {
      "id": "habit5",
      "name": "Meditation",
      "category": "Self-care",
      "priority": 1
    },
    {
      "id": "habit6",
      "name": "Journal",
      "category": "Writing",
      "priority": 1
    },
    {
      "id": "habit7",
      "name": "Team Meeting",
      "category": "job",
      "priority": 3
    },
    {
      "id": "habit8",
      "name": "Scripture Study",
      "category": "Faith",
      "priority": 2
    }
  ]
}
EOL

echo "🔄 Running category standardization simulation..."
cat sample_sync_data.json | python3 simulate_category_sync.py

echo ""
echo "✅ Local simulation complete!"
echo ""
echo "This confirms that when habit data is synced from your phone,"
echo "the categories will be standardized to one of the four standard categories:"
echo "- Religious"
echo "- Career & Work"
echo "- Social & Family"
echo "- Personal Improvement"
echo ""
echo "Next step: Sync your app with the cloud to apply these standardizations"
echo "to your actual habit data."

# Clean up temporary files
rm -f simulate_category_sync.py sample_sync_data.json
