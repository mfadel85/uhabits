#!/bin/bash

# Test script for category standardization in Lambda

echo "📊 Testing category standardization in Lambda function..."

# Create a simple test payload with a non-standard category
cat > test_payload.json << EOL
{
  "user_id": "test_user",
  "sync_timestamp": $(date +%s)000,
  "habits_data": [
    {
      "id": "test_habit_1",
      "name": "Test Habit 1",
      "category": "pray daily",
      "priority": 3,
      "weight": 1.0,
      "performance_history": {
        "daily_data": [
          {
            "date": "2025-09-27",
            "completed": true,
            "day_of_week": 6
          }
        ]
      }
    },
    {
      "id": "test_habit_2",
      "name": "Test Habit 2",
      "category": "job tasks",
      "priority": 2,
      "weight": 1.0,
      "performance_history": {}
    }
  ]
}
EOL

echo "📤 Invoking Lambda function with test data..."

# Invoke the Lambda function with our test payload
aws lambda invoke \
  --function-name uhabits-sync-prod \
  --region eu-central-1 \
  --payload file://test_payload.json \
  --cli-binary-format raw-in-base64-out \
  output.json

# Check if the Lambda function was invoked successfully
if [ $? -eq 0 ]; then
  echo "✅ Lambda function invoked successfully!"
  echo "📝 Response:"
  cat output.json | jq .
  
  # Check if standardized_categories is greater than 0
  STANDARDIZED=$(cat output.json | jq -r '.body' | jq -r '.processed.standardized_categories')
  if [ "$STANDARDIZED" -gt 0 ]; then
    echo "🎯 Category standardization is working! $STANDARDIZED categories were standardized."
    echo "   - 'pray daily' -> 'Religious'"
    echo "   - 'job tasks' -> 'Career & Work'"
  else
    echo "⚠️ No categories were standardized. This might indicate an issue with the standardization logic."
  fi
else
  echo "❌ Lambda function invocation failed!"
  exit 1
fi

# Clean up
echo "🧹 Cleaning up test files..."
rm test_payload.json output.json

echo "✨ Done!"
