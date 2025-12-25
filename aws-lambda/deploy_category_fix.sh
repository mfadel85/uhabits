#!/bin/bash

# Category Standardization Lambda Function Update
# This script deploys the enhanced habit sync function with category standardization

echo "🔍 Checking AWS configuration..."
aws sts get-caller-identity > /dev/null 2>&1

if [ $? -ne 0 ]; then
    echo "❌ AWS credentials not configured. Please run aws configure first."
    exit 1
fi

echo "📦 Preparing Lambda function with category standardization..."

# Copy Python file with proper name
cp habit_sync_function.py lambda_function.py

# Create deployment package
echo "🗜️ Creating deployment package..."
zip -r enhanced_habit_sync_function.zip lambda_function.py
rm lambda_function.py

echo "📊 Lambda package summary:"
ls -lh enhanced_habit_sync_function.zip

# Convert zip to base64
echo "🔄 Converting package to base64 for deployment..."
BASE64_CODE=$(base64 -w 0 enhanced_habit_sync_function.zip)

echo "☁️ Deploying to AWS Lambda..."
aws lambda update-function-code \
    --function-name uhabits-sync-prod \
    --zip-file "fileb://enhanced_habit_sync_function.zip" \
    --region eu-central-1 \
    --publish

if [ $? -eq 0 ]; then
    echo "✅ Lambda function updated successfully with category standardization!"
    
    # Get updated function info
    echo "📋 Updated Lambda function details:"
    aws lambda get-function \
        --function-name uhabits-sync-prod \
        --region eu-central-1 \
        --query '{State:Configuration.State,LastModified:Configuration.LastModified,CodeSize:Configuration.CodeSize,Handler:Configuration.Handler}' \
        --output table
        
    echo "🎯 Category standardization is now active! Categories will be standardized as:"
    echo "   - 'Religious' - for spiritual and religious habits"
    echo "   - 'Career & Work' - for professional habits" 
    echo "   - 'Social & Family' - for relationship habits"
    echo "   - 'Personal Improvement' - for health and self-development habits"
    
    echo "📱 Next steps:"
    echo "   1. Sync your app to upload any updated categories"
    echo "   2. Check your dashboard to confirm categories are showing correctly"
else
    echo "❌ Lambda update failed!"
    echo "   Try manual upload through AWS Console:"
    echo "   - Go to AWS Lambda Console"
    echo "   - Select function 'uhabits-sync-prod'"
    echo "   - Upload the file 'enhanced_habit_sync_function.zip'"
    exit 1
fi
