#!/bin/bash

# Cloud Sync Test Script
# Tests connectivity to AWS API Gateway endpoint for uHabits sync

echo "===== uHabits Cloud Sync Connection Test ====="
echo "Testing connectivity to AWS API Gateway endpoint..."
echo ""

# API Gateway endpoint (default endpoint, update if needed)
API_ENDPOINT="https://jodcprzip3.execute-api.eu-central-1.amazonaws.com/prod/sync"

# API Key (default test key, update if needed)
API_KEY="Y1leD4smWeX3yfCRM9Sv8R1Jo7g9zusx08Xo4y7o"

# First check basic internet connectivity
echo "Testing internet connectivity..."
if ping -c 1 google.com &> /dev/null; then
    echo "✓ Internet connection is working"
else
    echo "✗ Internet connection failed! Please check your network settings."
    exit 1
fi

echo ""
echo "Testing AWS API Gateway connectivity..."

# Create a minimal test payload
TEST_PAYLOAD='{
  "user_id": "test_user",
  "sync_timestamp": '$(date +%s%3N)',
  "sync_date": "'$(date +%Y-%m-%d)'",
  "summary_metrics": {
    "total_habits": 1,
    "active_habits": 1
  },
  "device_info": {
    "platform": "Test Script",
    "app_version": "test",
    "sync_version": "1.0"
  },
  "metadata": {
    "sync_type": "test",
    "test": true
  },
  "habits_data": []
}'

# Make the API call
echo "Sending test request to: $API_ENDPOINT"
echo "Please wait..."

HTTP_CODE=$(curl -s -o /tmp/api_response.txt -w "%{http_code}" \
  -X POST \
  -H "Content-Type: application/json" \
  -H "X-Api-Key: $API_KEY" \
  -d "$TEST_PAYLOAD" \
  "$API_ENDPOINT")

echo ""
echo "Response HTTP code: $HTTP_CODE"

if [ "$HTTP_CODE" -ge 200 ] && [ "$HTTP_CODE" -lt 300 ]; then
    echo "✓ Connection successful! API endpoint is reachable."
    echo "Response:"
    cat /tmp/api_response.txt
    echo ""
    echo "Your cloud sync should work correctly. If you're still having issues,"
    echo "please check your device's network settings or firewall configuration."
else
    echo "✗ Connection failed with HTTP code $HTTP_CODE"
    echo "Response:"
    cat /tmp/api_response.txt
    echo ""
    
    if [ "$HTTP_CODE" -eq 403 ] || [ "$HTTP_CODE" -eq 401 ]; then
        echo "Error: Authentication failed. Your API key may be invalid or expired."
    elif [ "$HTTP_CODE" -eq 404 ]; then
        echo "Error: API endpoint not found. The Lambda function may not be deployed correctly."
    elif [ "$HTTP_CODE" -ge 500 ]; then
        echo "Error: Server error. The AWS Lambda function may have encountered an error."
    else
        echo "Error: Unknown error occurred. Please check your network connection and API endpoint."
    fi
fi

echo ""
echo "===== Connection Test Complete ====="
