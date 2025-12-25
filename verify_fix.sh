#!/bin/bash

echo "🧪 Testing API after Lambda update..."
echo "====================================="

echo ""
echo "1. Basic connectivity test:"
curl -s -w "Status: %{http_code}\n" "https://bhg1kt9cf2.execute-api.eu-central-1.amazonaws.com/prod/api/groups" -H "x-api-key: Y1leD4smWeX3yfCRM9Sv8R1Jo7g9zusx08Xo4y7o"

echo ""
echo "2. Check if we get JSON response:"
RESPONSE=$(curl -s "https://bhg1kt9cf2.execute-api.eu-central-1.amazonaws.com/prod/api/groups" -H "x-api-key: Y1leD4smWeX3yfCRM9Sv8R1Jo7g9zusx08Xo4y7o")
echo "$RESPONSE"

echo ""
echo "3. Look for success indicators:"
if echo "$RESPONSE" | grep -q "group_performance"; then
    echo "✅ SUCCESS: Found group_performance data"
elif echo "$RESPONSE" | grep -q "Lambda is working"; then
    echo "✅ PARTIAL: Test function is working"
elif echo "$RESPONSE" | grep -q "Internal server error"; then
    echo "❌ STILL BROKEN: Getting 502 error"
else
    echo "❓ UNKNOWN: Unexpected response"
fi

echo ""
echo "4. Count habits if successful:"
echo "$RESPONSE" | jq '.summary.total_habits_analyzed // "No habits found"' 2>/dev/null || echo "No valid JSON"

echo ""
echo "📊 Dashboard should now work if you see 'group_performance' above!"
