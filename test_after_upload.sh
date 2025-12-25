#!/bin/bash

echo "🧪 TESTING AFTER REAL DATA UPLOAD"
echo "================================="
echo ""

echo "Testing API response..."
RESPONSE=$(curl -s "https://bhg1kt9cf2.execute-api.eu-central-1.amazonaws.com/prod/api/groups" -H "x-api-key: Y1leD4smWeX3yfCRM9Sv8R1Jo7g9zusx08Xo4y7o")

echo "Data source:"
echo "$RESPONSE" | jq -r '.summary.data_source // "Field not found"'

echo ""
echo "Total habits found:"
echo "$RESPONSE" | jq -r '.summary.total_habits_analyzed // "Field not found"'

echo ""
echo "Status:"
echo "$RESPONSE" | jq -r '.status // "Field not found"'

echo ""
echo "Message:"
echo "$RESPONSE" | jq -r '.message // "Field not found"'

echo ""
if echo "$RESPONSE" | jq -e '.group_performance' > /dev/null 2>&1; then
    echo "✅ SUCCESS: Dashboard format detected!"
    echo "Groups found:"
    echo "$RESPONSE" | jq -r '.group_performance | keys[]'
else
    echo "❌ ISSUE: No group_performance data"
    echo "Raw response:"
    echo "$RESPONSE" | head -5
fi
