#!/bin/bash

echo "Testing API Gateway Connection..."
echo "Attempting to reach the API endpoint used by the app"

# Test basic connectivity with curl
API_URL="https://jodcprzip3.execute-api.eu-central-1.amazonaws.com/prod"
API_KEY="Y1leD4smWeX3yfCRM9Sv8R1Jo7g9zusx08Xo4y7o"

echo "Testing API endpoint: $API_URL"
echo "Using API key: ${API_KEY:0:5}...${API_KEY: -5}"

# First, try a simple GET to check if endpoint exists
echo "Checking API Gateway status..."
HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$API_URL")

if [ $HTTP_STATUS -eq 403 ]; then
  echo "✅ API Gateway responded with 403 Forbidden - This is actually good! It means the API exists but requires authentication"
elif [ $HTTP_STATUS -eq 401 ]; then
  echo "✅ API Gateway responded with 401 Unauthorized - This is actually good! It means the API exists but requires authentication"
elif [ $HTTP_STATUS -eq 200 ]; then
  echo "✅ API Gateway responded with 200 OK - The API is accessible!"
else
  echo "❌ API Gateway responded with unexpected status code: $HTTP_STATUS"
  echo "The API endpoint might not exist or might be incorrectly configured."
fi

# Now try with a minimal POST request using the API key
echo ""
echo "Testing API connectivity with authentication..."
RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" -X POST \
  -H "Content-Type: application/json" \
  -H "X-Api-Key: $API_KEY" \
  -d '{"test": true}' \
  "$API_URL/sync")

if [ $RESPONSE -eq 200 ]; then
  echo "✅ API accepted the test request with API key!"
elif [ $RESPONSE -eq 403 ] || [ $RESPONSE -eq 401 ]; then
  echo "⚠️ Authentication issue: Received $RESPONSE response. The API key might be invalid or expired."
else
  echo "❌ Connection failed with status code: $RESPONSE"
  echo "This indicates there might be connectivity issues with the API endpoint."
fi

echo ""
echo "Recommendations:"
echo "1. Make sure your AWS Lambda function in eu-central-1 region is properly deployed and active."
echo "2. Verify that the API Gateway configuration is correct and the endpoint is accessible."
echo "3. The API key might need to be refreshed if it has expired."

chmod +x test_api.sh
echo ""
echo "API connection test script created. Run it with: ./test_api.sh"
