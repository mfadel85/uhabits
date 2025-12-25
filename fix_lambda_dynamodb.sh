#!/bin/bash

echo "🔧 Fixing Lambda configuration for DynamoDB access..."

# 1. Update Lambda timeout to 30 seconds
echo "📝 Setting Lambda timeout to 30 seconds..."
aws lambda update-function-configuration \
    --function-name uhabits-dashboard-api \
    --timeout 30 \
    --region eu-central-1

# 2. Create DynamoDB policy
echo "📝 Creating DynamoDB access policy..."
aws iam create-policy \
    --policy-name lambda-dynamodb-access \
    --policy-document file://dynamodb-policy.json \
    --description "Allow Lambda to read from DynamoDB"

# 3. Attach the policy to Lambda execution role
echo "📝 Attaching DynamoDB policy to Lambda role..."
aws iam attach-role-policy \
    --role-name lambda-execution-role \
    --policy-arn arn:aws:iam::328600977654:policy/lambda-dynamodb-access

# 4. Verify current configuration
echo "✅ Checking Lambda configuration..."
aws lambda get-function-configuration \
    --function-name uhabits-dashboard-api \
    --region eu-central-1 \
    --query '{Timeout: Timeout, Handler: Handler, Role: Role}'

echo "✅ Lambda configuration updated for DynamoDB access!"
echo "🧪 Test your dashboard now - it should use real DynamoDB data."
