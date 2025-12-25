#!/bin/bash

echo "=== QUICK FIX FOR INTERNAL SERVER ERROR ==="
echo ""
echo "The 502 internal server error is likely caused by:"
echo "1. Lambda function timeout (current: 3 seconds)"
echo "2. Memory issues (current: 128 MB)"
echo "3. Code crashes when processing large dataset"
echo ""
echo "IMMEDIATE SOLUTION:"
echo "Deploy the minimal working version first to verify API works"
echo ""
echo "MANUAL STEPS:"
echo "1. Go to AWS Lambda Console"
echo "2. Find function: uhabits-dashboard-api"
echo "3. Upload: minimal_lambda.zip"
echo "4. Set handler: minimal_api.lambda_handler"
echo "5. Increase timeout: 30 seconds"
echo "6. Increase memory: 256 MB"
echo ""
echo "TEST COMMAND:"
echo 'curl -H "x-api-key: Y1leD4smWeX3yfCRM9Sv8R1Jo7g9zusx08Xo4y7o" "https://bhg1kt9cf2.execute-api.eu-central-1.amazonaws.com/prod/api/groups"'
echo ""

# Test the current API to show the error
echo "CURRENT API STATUS:"
curl -s -w "HTTP Status: %{http_code}\n" -H "x-api-key: Y1leD4smWeX3yfCRM9Sv8R1Jo7g9zusx08Xo4y7o" "https://bhg1kt9cf2.execute-api.eu-central-1.amazonaws.com/prod/api/groups"

echo ""
echo "After deploying minimal_lambda.zip, this should return 200 with mock data"
echo "Then we can build up to the real data version step by step"
