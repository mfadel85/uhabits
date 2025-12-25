#!/bin/bash

# Simple Lambda function verification script
# This checks if the Lambda function is properly deployed with the category standardization functionality

echo "🔍 Verifying Lambda function deployment status..."

# Check Lambda function configuration
FUNCTION_INFO=$(aws lambda get-function \
    --function-name uhabits-sync-prod \
    --region eu-central-1 \
    --query '{Handler:Configuration.Handler,State:Configuration.State,LastModified:Configuration.LastModified,CodeSize:Configuration.CodeSize}' \
    --output json)

if [ $? -ne 0 ]; then
    echo "❌ Failed to retrieve Lambda function information. Check your AWS credentials."
    exit 1
fi

# Extract information
HANDLER=$(echo $FUNCTION_INFO | jq -r '.Handler')
STATE=$(echo $FUNCTION_INFO | jq -r '.State')
LAST_MODIFIED=$(echo $FUNCTION_INFO | jq -r '.LastModified')
CODE_SIZE=$(echo $FUNCTION_INFO | jq -r '.CodeSize')

echo "📋 Lambda Function Details:"
echo "  Handler: $HANDLER"
echo "  State: $STATE"
echo "  Last Modified: $LAST_MODIFIED"
echo "  Code Size: $CODE_SIZE bytes"
echo ""

# Check if the handler is correctly set to handle the module name
if [[ $HANDLER == *"habit_sync_function"* ]]; then
    echo "✅ Lambda function handler is correctly configured to use habit_sync_function module."
else
    echo "❌ Lambda function handler might not be correctly configured. Expected 'habit_sync_function' but got '$HANDLER'"
    
    # Suggest corrective action
    echo ""
    echo "Suggested action: Update the Lambda function handler name to 'habit_sync_function.lambda_handler'"
    echo "You can do this with the following command:"
    echo "aws lambda update-function-configuration --function-name uhabits-sync-prod --region eu-central-1 --handler habit_sync_function.lambda_handler"
fi

echo ""
if [[ $STATE == "Active" ]]; then
    echo "✅ Lambda function is active and ready to process habit data."
else
    echo "❌ Lambda function is not active. Current state: $STATE"
fi

echo ""
echo "📊 The Lambda function includes category standardization that maps:"
echo "  - 'Religious' - for spiritual and religious habits"
echo "  - 'Career & Work' - for professional habits"
echo "  - 'Social & Family' - for relationship habits" 
echo "  - 'Personal Improvement' - for health and self-development habits (default)"
echo ""
echo "To test category standardization, sync your app and check if the categories"
echo "in your dashboard match these standard categories."
