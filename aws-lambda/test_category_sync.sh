#!/bin/bash

# Test script to verify categories are synced properly
# This script simulates the sync process and checks if categories are included

echo "🔍 Testing category syncing in habit data..."

# Generate unique ID for this test
TEST_ID=$(date +%s)

# Create test payload with habit data including categories
cat > test_sync_payload.json << EOL
{
  "user_id": "test_category_sync",
  "sync_timestamp": $TEST_ID,
  "sync_date": "2025-09-27",
  "summary_metrics": {
    "total_habits": 4,
    "completed_habits": 2
  },
  "habits_data": [
    {
      "id": "habit_religious_$TEST_ID",
      "name": "Daily Prayer",
      "category": "pray daily",
      "priority": 3,
      "weight": 1.0,
      "success_rate": 0.95,
      "performance_history": {
        "daily_data": [
          {
            "date": "2025-09-27",
            "completed": true
          }
        ]
      }
    },
    {
      "id": "habit_work_$TEST_ID",
      "name": "Check Emails",
      "category": "job tasks",
      "priority": 2,
      "weight": 1.0,
      "success_rate": 0.8,
      "performance_history": {
        "daily_data": [
          {
            "date": "2025-09-27",
            "completed": true
          }
        ]
      }
    },
    {
      "id": "habit_family_$TEST_ID",
      "name": "Call Parents",
      "category": "family time",
      "priority": 3,
      "weight": 1.0,
      "success_rate": 0.7,
      "performance_history": {
        "daily_data": [
          {
            "date": "2025-09-27",
            "completed": false
          }
        ]
      }
    },
    {
      "id": "habit_personal_$TEST_ID",
      "name": "Exercise",
      "category": "health routine",
      "priority": 3,
      "weight": 1.0,
      "success_rate": 0.6,
      "performance_history": {
        "daily_data": [
          {
            "date": "2025-09-27",
            "completed": false
          }
        ]
      }
    }
  ]
}
EOL

echo "☁️ Sending test data to AWS Lambda..."
aws lambda invoke \
  --function-name uhabits-sync-prod \
  --region eu-central-1 \
  --payload file://test_sync_payload.json \
  --cli-binary-format raw-in-base64-out \
  sync_response.json

# Check if sync was successful
if [ $? -ne 0 ]; then
  echo "❌ Failed to invoke Lambda function!"
  rm test_sync_payload.json
  exit 1
fi

echo "✅ Test data synced. Checking for standardized categories in response..."

# Parse response to see if categories were standardized
RESPONSE=$(cat sync_response.json)
echo "Lambda response: $RESPONSE"

# Check if standardized_categories is mentioned in the response
if [[ $RESPONSE == *"standardized_categories"* ]] || [[ $RESPONSE == *"Standardized"* ]]; then
  echo "✅ Category standardization was applied during sync!"
else
  echo "⚠️ Categories may not have been standardized. Check Lambda logs for details."
fi

echo "🔍 Verifying category data in DynamoDB..."

# Query DynamoDB to check if habit records have standardized categories
echo "Checking habit record categories..."
aws dynamodb query \
  --table-name uHabits-Analytics-prod \
  --key-condition-expression "PK = :pk AND begins_with(SK, :sk)" \
  --expression-attribute-values "{\":pk\": {\"S\": \"USER#test_category_sync\"}, \":sk\": {\"S\": \"HABIT#$TEST_ID\"}}" \
  --region eu-central-1 \
  --output json > habit_records.json

# Query DynamoDB to check if daily records have standardized categories
echo "Checking daily record categories..."
aws dynamodb query \
  --table-name uHabits-Analytics-prod \
  --key-condition-expression "PK = :pk AND begins_with(SK, :sk)" \
  --expression-attribute-values "{\":pk\": {\"S\": \"USER#test_category_sync\"}, \":sk\": {\"S\": \"DAILY#\"}}" \
  --region eu-central-1 \
  --output json > daily_records.json

# Check if the habits have standardized categories
if [ -s habit_records.json ]; then
  echo ""
  echo "📋 Habit Records with Standardized Categories:"
  cat habit_records.json | jq -r '.Items[] | "\(.habit_name.S): \(.category.S)"'

  # Verify that standard categories are being used
  RELIGIOUS=$(cat habit_records.json | jq -r '.Items[] | select(.category.S == "Religious") | .habit_id.S')
  CAREER=$(cat habit_records.json | jq -r '.Items[] | select(.category.S == "Career & Work") | .habit_id.S')
  SOCIAL=$(cat habit_records.json | jq -r '.Items[] | select(.category.S == "Social & Family") | .habit_id.S')
  PERSONAL=$(cat habit_records.json | jq -r '.Items[] | select(.category.S == "Personal Improvement") | .habit_id.S')
  
  echo ""
  echo "📊 Category Standardization Results:"
  echo "Religious category habits: ${RELIGIOUS:-None}"
  echo "Career & Work category habits: ${CAREER:-None}"
  echo "Social & Family category habits: ${SOCIAL:-None}"
  echo "Personal Improvement category habits: ${PERSONAL:-None}"
  
  if [[ -n "$RELIGIOUS" && -n "$CAREER" && -n "$SOCIAL" && -n "$PERSONAL" ]]; then
    echo "✅ All four standard categories were found in the synced data!"
  else
    echo "⚠️ Some standard categories were not found in the synced data."
  fi
else
  echo "❌ No habit records found in DynamoDB. Sync may have failed."
fi

# Clean up
echo "🧹 Cleaning up test files..."
rm -f test_sync_payload.json sync_response.json habit_records.json daily_records.json

echo "✨ Category sync test complete!"
