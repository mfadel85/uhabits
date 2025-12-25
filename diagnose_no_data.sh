#!/bin/bash

echo "🔍 DASHBOARD SHOWS NO DATA - DIAGNOSTIC"
echo "======================================"
echo ""

echo "Current API Response:"
RESPONSE=$(curl -s "https://bhg1kt9cf2.execute-api.eu-central-1.amazonaws.com/prod/api/groups" -H "x-api-key: Y1leD4smWeX3yfCRM9Sv8R1Jo7g9zusx08Xo4y7o")
echo "$RESPONSE" | jq '.' 2>/dev/null || echo "$RESPONSE"

echo ""
echo "🔍 ANALYSIS:"
if echo "$RESPONSE" | grep -q '"test"'; then
    echo "❌ PROBLEM: Lambda is still running TEST version"
    echo "   You need to upload the REAL DATA version"
    echo "   File to upload: minimal_lambda_real_data.zip"
elif echo "$RESPONSE" | grep -q '"data_source".*"Real DynamoDB Data"'; then
    echo "✅ Lambda using real data, but might be empty"
    echo "   Check if DynamoDB has processable records"
elif echo "$RESPONSE" | grep -q '"data_source".*"Mock"'; then
    echo "⚠️  Lambda falling back to mock data"
    echo "   Likely DynamoDB permissions or empty results"
elif echo "$RESPONSE" | grep -q '"message".*"Internal server error"'; then
    echo "❌ Lambda still has 502 error"
    echo "   Check CloudWatch logs for details"
else
    echo "❓ Unclear response format"
fi

echo ""
echo "📋 NEXT STEPS:"
echo "1. Upload: minimal_lambda_real_data.zip"
echo "2. Handler: minimal_api.lambda_handler"
echo "3. Deploy and test"
echo ""
