#!/bin/bash

# Manual SAM deployment with S3 bucket creation
echo "🚀 Manual SAM Deployment for Frankfurt"
echo "====================================="

REGION="eu-central-1"
STACK_NAME="uhabits-analytics-prod"
ENVIRONMENT="prod"

# Step 1: Build the function
echo "1. Building Lambda function..."
cd aws-lambda
sam build --template-file template.yaml

if [ $? -ne 0 ]; then
    echo "❌ Build failed"
    exit 1
fi

echo "✅ Build successful"
echo ""

# Step 2: Deploy with guided setup (interactive)
echo "2. Deploying with guided setup..."
echo "When prompted, answer:"
echo "  - Stack Name: ${STACK_NAME}"
echo "  - AWS Region: ${REGION}"
echo "  - Confirm changes: y"
echo "  - Allow SAM CLI IAM role creation: y"
echo "  - Disable rollback: N"
echo "  - Save parameters to config file: y"
echo "  - SAM configuration file: samconfig.toml"
echo ""

read -p "Press Enter to start guided deployment..."

sam deploy --guided \
    --stack-name "${STACK_NAME}" \
    --region "${REGION}" \
    --parameter-overrides Environment="${ENVIRONMENT}"

if [ $? -eq 0 ]; then
    echo ""
    echo "🎉 Deployment successful!"
    echo ""
    
    # Get outputs
    echo "📋 Getting deployment outputs..."
    
    API_ENDPOINT=$(aws cloudformation describe-stacks \
        --stack-name "${STACK_NAME}" \
        --region "${REGION}" \
        --query 'Stacks[0].Outputs[?OutputKey==`APIGatewayURL`].OutputValue' \
        --output text)
    
    echo "🔗 API Endpoint: ${API_ENDPOINT}/sync"
    
    # Return to parent directory
    cd ..
    
    echo ""
    echo "✅ Check the complete setup with: ./check_deployment.sh"
    
else
    echo "❌ Deployment failed"
    exit 1
fi
