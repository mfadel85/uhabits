#!/bin/bash

# Clean deployment script for Frankfurt Lambda integration
echo "🚀 Clean SAM Deployment for Frankfurt"
echo "===================================="

REGION="eu-central-1"
STACK_NAME="uhabits-analytics-prod"
ENVIRONMENT="prod"

# Check if stack exists and handle accordingly
echo "1. Checking existing stack status..."
STACK_STATUS=$(aws cloudformation describe-stacks --stack-name "${STACK_NAME}" --region "${REGION}" --query 'Stacks[0].StackStatus' --output text 2>/dev/null || echo "NOT_EXISTS")

if [ "$STACK_STATUS" != "NOT_EXISTS" ]; then
    echo "   Stack exists with status: $STACK_STATUS"
    
    if [ "$STACK_STATUS" == "ROLLBACK_FAILED" ] || [ "$STACK_STATUS" == "CREATE_FAILED" ]; then
        echo "   🗑️ Deleting failed stack..."
        aws cloudformation delete-stack --stack-name "${STACK_NAME}" --region "${REGION}"
        
        echo "   ⏳ Waiting for deletion..."
        aws cloudformation wait stack-delete-complete --stack-name "${STACK_NAME}" --region "${REGION}"
        echo "   ✅ Stack deleted successfully"
    fi
else
    echo "   ✅ No existing stack - ready for fresh deployment"
fi

echo ""

# Build the function
echo "2. Building Lambda function..."
cd aws-lambda
sam build --template-file template.yaml

if [ $? -ne 0 ]; then
    echo "❌ Build failed"
    exit 1
fi

echo "✅ Build successful"
echo ""

# Deploy with proper S3 handling
echo "3. Deploying to AWS..."
sam deploy \
    --template-file .aws-sam/build/template.yaml \
    --stack-name "${STACK_NAME}" \
    --region "${REGION}" \
    --parameter-overrides Environment="${ENVIRONMENT}" \
    --capabilities CAPABILITY_IAM \
    --resolve-s3 \
    --no-fail-on-empty-changeset \
    --no-confirm-changeset

if [ $? -eq 0 ]; then
    echo ""
    echo "🎉 Deployment successful!"
    echo ""
    
    # Get deployment outputs
    echo "📋 Retrieving deployment information..."
    
    API_ENDPOINT=$(aws cloudformation describe-stacks \
        --stack-name "${STACK_NAME}" \
        --region "${REGION}" \
        --query 'Stacks[0].Outputs[?OutputKey==`APIGatewayURL`].OutputValue' \
        --output text)
    
    API_KEY_ID=$(aws cloudformation describe-stacks \
        --stack-name "${STACK_NAME}" \
        --region "${REGION}" \
        --query 'Stacks[0].Outputs[?OutputKey==`APIKey`].OutputValue' \
        --output text)
    
    TABLE_NAME=$(aws cloudformation describe-stacks \
        --stack-name "${STACK_NAME}" \
        --region "${REGION}" \
        --query 'Stacks[0].Outputs[?OutputKey==`DynamoDBTable`].OutputValue' \
        --output text)
    
    # Get the actual API key value
    if [ -n "$API_KEY_ID" ] && [ "$API_KEY_ID" != "None" ]; then
        API_KEY_VALUE=$(aws apigateway get-api-key \
            --api-key "${API_KEY_ID}" \
            --include-value \
            --region "${REGION}" \
            --query 'value' \
            --output text)
    else
        API_KEY_VALUE="Not created"
    fi
    
    echo ""
    echo "🎯 Deployment Results:"
    echo "====================="
    echo "✅ API Endpoint: ${API_ENDPOINT}/sync"
    echo "✅ API Key: ${API_KEY_VALUE}"
    echo "✅ DynamoDB Table: ${TABLE_NAME}"
    echo "✅ Region: ${REGION}"
    
    # Create Android configuration
    cd ..
    cat > android_config.json << EOF
{
    "api_endpoint": "${API_ENDPOINT}/sync",
    "api_key": "${API_KEY_VALUE}",
    "region": "${REGION}",
    "table_name": "${TABLE_NAME}",
    "environment": "${ENVIRONMENT}",
    "_note": "This file contains API Gateway credentials for Frankfurt region",
    "_security": "API key has limited permissions for sync endpoint only"
}
EOF
    
    echo ""
    echo "📱 Android App Configuration:"
    echo "   File created: android_config.json"
    echo "   Update your Android app with these Frankfurt endpoints."
    
    # Test the deployment
    echo ""
    echo "🧪 Testing deployment..."
    TEST_PAYLOAD='{
        "user_id": "user_primary",
        "sync_timestamp": '$(date +%s000)',
        "summary_metrics": {
            "total_habits": 1,
            "active_habits": 1,
            "weighted_success_rate": 1.0
        },
        "habits_data": [{
            "id": "test",
            "name": "Test Habit",
            "priority": "NORMAL",
            "weight": 1.0,
            "success_rate": 1.0,
            "weighted_success_rate": 1.0
        }]
    }'
    
    if [ "$API_KEY_VALUE" != "Not created" ]; then
        curl -X POST \
            -H "Content-Type: application/json" \
            -H "x-api-key: ${API_KEY_VALUE}" \
            -d "${TEST_PAYLOAD}" \
            "${API_ENDPOINT}/sync" \
            --silent \
            --show-error \
            && echo "✅ API test successful" \
            || echo "⚠️ API test failed (check logs)"
    else
        echo "⚠️ Skipping API test - API key not available"
    fi
    
    echo ""
    echo "🎯 Next Steps:"
    echo "============="
    echo "1. Update Android app with Frankfurt API endpoint"
    echo "2. Test cloud sync from mobile app"
    echo "3. Monitor usage in AWS Console"
    echo ""
    echo "💰 Expected cost: ~€0.01/month (within AWS free tier)"
    echo "🚀 Your habit analytics cloud is ready in Frankfurt!"
    
else
    echo "❌ Deployment failed"
    echo "Check the error messages above and try again"
    exit 1
fi
