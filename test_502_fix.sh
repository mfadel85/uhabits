#!/bin/bash

echo "=== 502 BAD GATEWAY FIX STATUS ==="
echo ""
echo "Current error: Lambda function is crashing (502)"
echo "API Gateway: bhg1kt9cf2 ✅ (exists)"
echo "Endpoint: /api/groups ✅ (exists)" 
echo "Lambda: uhabits-dashboard-api ❌ (needs code update)"
echo ""
echo "SOLUTION: Update Lambda function with minimal_lambda.zip"
echo ""
echo "Steps in AWS Lambda Console:"
echo "1. Find function: uhabits-dashboard-api"
echo "2. Upload: minimal_lambda.zip"
echo "3. Set handler: minimal_api.lambda_handler"
echo "4. Increase timeout: 30 seconds"
echo "5. Increase memory: 256 MB"
echo ""
echo "Testing current API status:"

# Test the API
response=$(curl -s -w "HTTPSTATUS:%{http_code}" -H "x-api-key: Y1leD4smWeX3yfCRM9Sv8R1Jo7g9zusx08Xo4y7o" "https://bhg1kt9cf2.execute-api.eu-central-1.amazonaws.com/prod/api/groups")

# Extract HTTP status code
http_code=$(echo $response | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')

# Extract response body
body=$(echo $response | sed -e 's/HTTPSTATUS:.*//g')

echo "Response: $body"
echo "HTTP Status: $http_code"
echo ""

if [ "$http_code" = "502" ]; then
    echo "✅ CONFIRMED: 502 Bad Gateway error"
    echo "   Lambda function needs code update"
    echo "   Deploy minimal_lambda.zip to fix this"
elif [ "$http_code" = "200" ]; then
    echo "✅ SUCCESS: API is working!"
    echo "   Dashboard should load real data now"
else
    echo "⚠️  Unexpected status: $http_code"
    echo "   Check API Gateway configuration"
fi

echo ""
echo "After updating Lambda, this should return 200 with mock data"
