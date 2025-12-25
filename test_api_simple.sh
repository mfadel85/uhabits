#!/bin/bash
echo "Testing analytics API..."
echo "========================================="

# Test the API endpoint
echo "1. Testing API endpoint:"
response=$(curl -s -w "%{http_code}" -H "x-api-key: Y1leD4smWeX3yfCRM9Sv8R1Jo7g9zusx08Xo4y7o" "https://bhg1kt9cf2.execute-api.eu-central-1.amazonaws.com/prod/api/groups")

echo "Response: $response"
echo ""

echo "2. Testing base API:"
response2=$(curl -s -w "%{http_code}" -H "x-api-key: Y1leD4smWeX3yfCRM9Sv8R1Jo7g9zusx08Xo4y7o" "https://bhg1kt9cf2.execute-api.eu-central-1.amazonaws.com/prod/")

echo "Response: $response2"
echo ""

echo "========================================="
echo "Test complete"
