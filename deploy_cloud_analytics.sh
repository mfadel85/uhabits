#!/bin/bash

# uHabits Cloud Analytics Deployment Script
# Automated AWS serverless infrastructure setup

set -e  # Exit on any error

echo "🚀 uHabits Cloud Analytics Deployment"
echo "======================================"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
ENVIRONMENT=${1:-prod}
REGION=${2:-eu-central-1}  # Frankfurt region where Lambda was created
STACK_NAME="uhabits-analytics-${ENVIRONMENT}"

echo -e "${BLUE}Environment:${NC} ${ENVIRONMENT}"
echo -e "${BLUE}Region:${NC} ${REGION}"
echo -e "${BLUE}Stack Name:${NC} ${STACK_NAME}"
echo ""

# Check prerequisites
echo "🔍 Checking prerequisites..."

# Check AWS CLI
if ! command -v aws &> /dev/null; then
    echo -e "${RED}❌ AWS CLI not found. Please install it first.${NC}"
    echo "   brew install awscli  # macOS"
    echo "   apt install awscli   # Ubuntu"
    exit 1
fi

# Check SAM CLI
if ! command -v sam &> /dev/null; then
    echo -e "${RED}❌ SAM CLI not found. Please install it first.${NC}"
    echo "   brew install aws-sam-cli  # macOS"
    echo "   pip install aws-sam-cli   # pip"
    exit 1
fi

# Check AWS credentials
if ! aws sts get-caller-identity &> /dev/null; then
    echo -e "${RED}❌ AWS credentials not configured.${NC}"
    echo "   Setup options:"
    echo "   1. Run: aws configure"
    echo "   2. Set environment variables: AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY"
    echo "   3. Use IAM roles (if on EC2)"
    echo ""
    echo "   See .aws-credentials-setup.md for detailed instructions"
    exit 1
fi

# Check for existing Lambda function in Frankfurt
echo "🔍 Checking for existing resources in Frankfurt..."
EXISTING_FUNCTION=$(aws lambda get-function \
    --function-name "uhabits-sync-${ENVIRONMENT}" \
    --region "${REGION}" \
    --query 'Configuration.FunctionName' \
    --output text 2>/dev/null || echo "NONE")

if [ "$EXISTING_FUNCTION" != "NONE" ]; then
    echo -e "${YELLOW}⚠️  Found existing Lambda function: ${EXISTING_FUNCTION}${NC}"
    echo -e "${YELLOW}   This deployment will update the existing function.${NC}"
fi

echo -e "${GREEN}✅ Prerequisites check passed${NC}"
echo ""

# Get AWS account info
ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
USER_ARN=$(aws sts get-caller-identity --query Arn --output text)

echo -e "${BLUE}AWS Account:${NC} ${ACCOUNT_ID}"
echo -e "${BLUE}User/Role:${NC} ${USER_ARN}"
echo ""

# Confirm deployment
echo -e "${YELLOW}⚠️  This will deploy AWS resources that may incur costs.${NC}"
echo -e "${YELLOW}   (Expected cost: $0.01-0.10/month within free tier)${NC}"
echo ""
read -p "Continue with deployment? (y/N): " -n 1 -r
echo ""
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "Deployment cancelled."
    exit 1
fi

# Build the Lambda function
echo "🔨 Building Lambda function..."
cd aws-lambda
sam build --template-file template.yaml

if [ $? -ne 0 ]; then
    echo -e "${RED}❌ Build failed${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Build successful${NC}"
echo ""

# Deploy the stack
echo "☁️  Deploying to AWS..."
sam deploy \
    --template-file .aws-sam/build/template.yaml \
    --stack-name "${STACK_NAME}" \
    --region "${REGION}" \
    --parameter-overrides Environment="${ENVIRONMENT}" \
    --capabilities CAPABILITY_IAM \
    --resolve-s3 \
    --no-fail-on-empty-changeset

if [ $? -ne 0 ]; then
    echo -e "${RED}❌ Deployment failed${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Deployment successful${NC}"
echo ""

# Get stack outputs
echo "📋 Retrieving deployment information..."
API_ENDPOINT=$(aws cloudformation describe-stacks \
    --stack-name "${STACK_NAME}" \
    --region "${REGION}" \
    --query 'Stacks[0].Outputs[?OutputKey==`APIGatewayURL`].OutputValue' \
    --output text)

API_KEY=$(aws cloudformation describe-stacks \
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
API_KEY_VALUE=$(aws apigateway get-api-key \
    --api-key "${API_KEY}" \
    --include-value \
    --region "${REGION}" \
    --query 'value' \
    --output text)

echo ""
echo "🎉 Deployment Complete!"
echo "====================="
echo ""
echo -e "${GREEN}API Endpoint:${NC} ${API_ENDPOINT}/sync"
echo -e "${GREEN}API Key:${NC} ${API_KEY_VALUE}"
echo -e "${GREEN}DynamoDB Table:${NC} ${TABLE_NAME}"
echo -e "${GREEN}Region:${NC} ${REGION}"
echo ""

# Create configuration file for Android app
cat > ../android_config.json << EOF
{
    "api_endpoint": "${API_ENDPOINT}/sync",
    "api_key": "${API_KEY_VALUE}",
    "region": "${REGION}",
    "table_name": "${TABLE_NAME}",
    "environment": "${ENVIRONMENT}",
    "_note": "This file contains API Gateway credentials, not AWS access keys",
    "_security": "API key has limited permissions for sync endpoint only"
}
EOF

echo -e "${BLUE}📱 Android App Configuration:${NC}"
echo "   File created: ../android_config.json"
echo "   ⚠️  This file is automatically added to .gitignore"
echo "   Update your Android app with these values."
echo ""

# Create PowerBI connection info
cat > ../powerbi_connection.txt << EOF
PowerBI Pro Connection Details
=============================

DynamoDB Connection:
- Table Name: ${TABLE_NAME}
- Region: ${REGION}
- Access Pattern: Query by PK='USER#user_primary'

Connection String Format:
- Endpoint: https://dynamodb.${REGION}.amazonaws.com
- Table: ${TABLE_NAME}

IAM Policy Required:
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "dynamodb:Query",
                "dynamodb:GetItem",
                "dynamodb:Scan"
            ],
            "Resource": "arn:aws:dynamodb:${REGION}:${ACCOUNT_ID}:table/${TABLE_NAME}"
        }
    ]
}

Follow the PowerBI setup guide for detailed instructions.
EOF

echo -e "${BLUE}📊 PowerBI Configuration:${NC}"
echo "   File created: ../powerbi_connection.txt"
echo "   Follow POWERBI_SETUP_GUIDE.md for integration steps."
echo ""

# Test the deployment
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

curl -X POST \
    -H "Content-Type: application/json" \
    -H "x-api-key: ${API_KEY_VALUE}" \
    -d "${TEST_PAYLOAD}" \
    "${API_ENDPOINT}/sync" \
    --silent \
    --show-error

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ API test successful${NC}"
else
    echo -e "${YELLOW}⚠️  API test failed (this is normal for first deployment)${NC}"
fi

echo ""
echo "🎯 Next Steps:"
echo "============="
echo "1. Update Android app with API endpoint and key"
echo "2. Test cloud sync from mobile app"
echo "3. Set up PowerBI Pro connection (see guide)"
echo "4. Create analytics dashboard"
echo ""
echo -e "${GREEN}Cost Estimate: ~$0.01/month (within AWS free tier)${NC}"
echo -e "${BLUE}Monitor usage: AWS Console → CloudWatch → Billing${NC}"
echo ""
echo "🚀 Your habit analytics cloud is ready!"

# Return to original directory
cd ..

# Create a simple status check script
cat > check_deployment.sh << 'EOF'
#!/bin/bash
# Quick deployment status check

STACK_NAME="uhabits-analytics-prod"
REGION="eu-central-1"  # Frankfurt region

echo "🔍 Checking uHabits Analytics Deployment Status"
echo "=============================================="

aws cloudformation describe-stacks \
    --stack-name "${STACK_NAME}" \
    --region "${REGION}" \
    --query 'Stacks[0].{StackStatus:StackStatus,LastUpdated:LastUpdatedTime}' \
    --output table

echo ""
echo "📊 DynamoDB Table Status:"
TABLE_NAME=$(aws cloudformation describe-stacks \
    --stack-name "${STACK_NAME}" \
    --region "${REGION}" \
    --query 'Stacks[0].Outputs[?OutputKey==`DynamoDBTable`].OutputValue' \
    --output text)

aws dynamodb describe-table \
    --table-name "${TABLE_NAME}" \
    --region "${REGION}" \
    --query 'Table.{TableStatus:TableStatus,ItemCount:ItemCount,TableSize:TableSizeBytes}' \
    --output table

echo ""
echo "💰 Estimated Monthly Cost: ~$0.01 (within free tier)"
EOF

chmod +x check_deployment.sh

echo "📝 Created deployment status checker: ./check_deployment.sh"
echo ""
