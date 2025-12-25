#!/bin/bash

echo "🔍 Analyzing DynamoDB data structure..."

# Get a sample item and extract just the keys
aws dynamodb scan \
    --table-name uHabits-Analytics-prod \
    --region eu-central-1 \
    --max-items 1 \
    --query 'Items[0]' > sample_item.json

echo "📋 Field names in DynamoDB:"
cat sample_item.json | jq 'keys[]' | sort

echo ""
echo "🎯 Looking for habit-related fields..."
cat sample_item.json | jq 'to_entries[] | select(.key | contains("habit") or contains("name") or contains("success") or contains("category")) | .key'