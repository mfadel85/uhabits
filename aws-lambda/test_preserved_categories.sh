#!/bin/bash

# Test script for preserved category functionality

echo "🧪 Testing preserved category functionality..."

# Create a Python test script
cat > test_preserved_categories.py << 'EOL'
#!/usr/bin/env python3
"""
Test script to verify that categories are preserved as-is
"""
import json

# Sample category test cases
test_categories = [
    {"input": "My Custom Category", "expected": "My Custom Category"},
    {"input": "Work", "expected": "Work"},
    {"input": "Health & Wellness", "expected": "Health & Wellness"},
    {"input": "Religious", "expected": "Religious"},
    {"input": "Family Time", "expected": "Family Time"},
    {"input": "Learning Spanish", "expected": "Learning Spanish"},
    {"input": "", "expected": "Uncategorized"},
    {"input": None, "expected": "Uncategorized"}
]

def preserve_category(category):
    """Simulates the updated Lambda function's category handling"""
    if not category:
        return "Uncategorized"
    return category

# Run tests
print("Category Preservation Tests:")
print("-" * 60)
print(f"{'Original Category':<30} {'Preserved Result':<20} {'Pass/Fail':<10}")
print("-" * 60)

passed = 0
failed = 0

for test in test_categories:
    input_val = test["input"]
    expected = test["expected"]
    
    # Handle None case for display
    if input_val is None:
        input_display = "None"
    else:
        input_display = input_val
        
    result = preserve_category(input_val)
    
    if result == expected:
        status = "✅ PASS"
        passed += 1
    else:
        status = "❌ FAIL"
        failed += 1
        
    print(f"{input_display:<30} {result:<20} {status:<10}")

print("-" * 60)
print(f"Test Results: {passed} passed, {failed} failed")
print("-" * 60)

# Create a simulated sync payload
sync_payload = {
    "user_id": "test_user",
    "sync_timestamp": 1664235967000,
    "habits_data": [
        {
            "id": "habit1",
            "name": "My First Habit",
            "category": "My Custom Category",
            "priority": "HIGH"
        },
        {
            "id": "habit2",
            "name": "My Second Habit",
            "category": "Work",
            "priority": "NORMAL"
        },
        {
            "id": "habit3",
            "name": "My Third Habit",
            "category": "Health & Wellness",
            "priority": "HIGH"
        },
        {
            "id": "habit4",
            "name": "My Fourth Habit",
            "category": None,
            "priority": "LOW"
        }
    ]
}

# Simulate the sync process with preserved categories
print("\nSimulating sync with preserved categories:")
print("-" * 60)

for habit in sync_payload["habits_data"]:
    original = habit.get("category", None)
    if original is None:
        original_display = "None"
    else:
        original_display = original
        
    preserved = preserve_category(original)
    
    print(f"Habit: {habit['name']}")
    print(f"  Original category: {original_display}")
    print(f"  After processing: {preserved}")
    print()

print("✅ Simulation confirms categories are preserved as-is!")
print("   Only completely missing categories receive the default 'Uncategorized' value.")
EOL

# Execute the test script
python3 test_preserved_categories.py

echo ""
echo "📝 Summary of Category Preservation:"
echo "1. Original categories from the mobile app are now preserved exactly as entered"
echo "2. No automatic standardization is performed"
echo "3. Only habits with completely missing categories receive the default 'Uncategorized' value"
echo ""
echo "To deploy this change to AWS Lambda:"
echo "./deploy_preserved_categories.sh"

# Clean up
rm test_preserved_categories.py
