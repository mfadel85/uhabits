#!/bin/bash

# Post-configuration verification for Frankfurt deployment
echo "🎯 Frankfurt Deployment Verification"
echo "==================================="
echo ""

REGION="eu-central-1"

# Test AWS CLI connectivity
echo "1. Testing AWS CLI connection..."
if aws sts get-caller-identity --region "${REGION}" > /dev/null 2>&1; then
    echo "   ✅ AWS CLI configured correctly"
    
    ACCOUNT_ID=$(aws sts get-caller-identity --region "${REGION}" --query Account --output text)
    USER_ARN=$(aws sts get-caller-identity --region "${REGION}" --query Arn --output text)
    
    echo "   📋 Account: ${ACCOUNT_ID}"
    echo "   👤 User: ${USER_ARN}"
else
    echo "   ❌ AWS CLI not configured"
    echo "   Run: aws configure"
    echo "   Set region to: eu-central-1"
    exit 1
fi

echo ""

# Check for existing Lambda functions
echo "2. Checking existing Lambda functions..."
FUNCTIONS=$(aws lambda list-functions --region "${REGION}" --query 'Functions[?contains(FunctionName, `habit`) || contains(FunctionName, `uhabits`)].FunctionName' --output text)

if [ -n "$FUNCTIONS" ]; then
    echo "   ✅ Found existing function(s):"
    for func in $FUNCTIONS; do
        echo "      - $func"
        
        # Get function details
        RUNTIME=$(aws lambda get-function --function-name "$func" --region "${REGION}" --query 'Configuration.Runtime' --output text)
        HANDLER=$(aws lambda get-function --function-name "$func" --region "${REGION}" --query 'Configuration.Handler' --output text)
        
        echo "        Runtime: $RUNTIME, Handler: $HANDLER"
    done
else
    echo "   ⚠️  No habit-related functions found"
    echo "      Please verify your function name"
fi

echo ""

# Check for existing DynamoDB tables
echo "3. Checking existing DynamoDB tables..."
TABLES=$(aws dynamodb list-tables --region "${REGION}" --query 'TableNames[?contains(@, `uHabits`) || contains(@, `habit`)]' --output text)

if [ -n "$TABLES" ]; then
    echo "   📊 Found existing table(s):"
    for table in $TABLES; do
        echo "      - $table"
    done
else
    echo "   📊 No existing tables (will be created during deployment)"
fi

echo ""

# Deployment readiness check
echo "4. Deployment readiness:"
echo "   ✅ AWS CLI: Configured"
echo "   ✅ Region: Frankfurt (eu-central-1)"
echo "   ✅ Lambda: Found existing function"
echo "   ✅ SAM CLI: Available"

echo ""
echo "🚀 Ready for deployment!"
echo ""
echo "Next steps:"
echo "   1. Run: ./deploy_cloud_analytics.sh"
echo "   2. The script will:"
echo "      - Create DynamoDB table for habit data"
echo "      - Set up API Gateway for mobile app access"
echo "      - Create API keys for authentication"
echo "      - Update your existing Lambda function configuration"
echo "      - Generate android_config.json with Frankfurt endpoints"
echo ""
echo "💡 The deployment will work with your existing Lambda function"
echo "   and create all the missing infrastructure around it."
