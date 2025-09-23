#!/bin/bash

# Check existing Lambda function in Frankfurt
echo "🔍 Checking existing Lambda function in Frankfurt..."
echo "================================================="

REGION="eu-central-1"
FUNCTION_NAME="uhabits-sync-prod"

# Check if AWS CLI is configured
if ! aws sts get-caller-identity --region "${REGION}" &> /dev/null; then
    echo "❌ AWS CLI not configured. Run: aws configure"
    echo "   Set region to: eu-central-1"
    exit 1
fi

echo "✅ AWS CLI configured"
echo ""

# Check Lambda function
echo "📋 Lambda Functions in Frankfurt:"
aws lambda list-functions \
    --region "${REGION}" \
    --query 'Functions[?contains(FunctionName, `uhabits`) || contains(FunctionName, `habit`)].{Name:FunctionName,Runtime:Runtime,LastModified:LastModified}' \
    --output table

echo ""

# Check specific function if it exists
if aws lambda get-function --function-name "${FUNCTION_NAME}" --region "${REGION}" &> /dev/null; then
    echo "✅ Found existing function: ${FUNCTION_NAME}"
    
    # Get function details
    aws lambda get-function \
        --function-name "${FUNCTION_NAME}" \
        --region "${REGION}" \
        --query 'Configuration.{FunctionName:FunctionName,Runtime:Runtime,Handler:Handler,Environment:Environment}' \
        --output table
        
    echo ""
    echo "🚀 Ready to deploy complete infrastructure around existing function!"
    echo "   Run: ./deploy_cloud_analytics.sh"
    
else
    echo "ℹ️  Function '${FUNCTION_NAME}' not found."
    echo "   Available functions listed above."
    echo ""
    echo "If your function has a different name, update the deployment script:"
    echo "   FUNCTION_NAME in deploy_cloud_analytics.sh"
fi

echo ""
echo "💡 Tip: The deployment will create/update:"
echo "   - DynamoDB table for data storage"
echo "   - API Gateway for secure mobile access"
echo "   - API keys for authentication"
echo "   - Configuration files for Android app"
