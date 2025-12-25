#!/bin/bash

# Deploy Lambda function with preserved category handling
# This script updates the Lambda function to preserve original categories from the mobile app

echo "📦 Preparing Lambda function with preserved category handling..."

# Create zip package
echo "🗜️ Creating deployment package..."
zip -r preserved_categories_function.zip habit_sync_function.py
ls -lh preserved_categories_function.zip

echo "☁️ Deploying to AWS Lambda..."
aws lambda update-function-code \
    --function-name uhabits-sync-prod \
    --zip-file "fileb://preserved_categories_function.zip" \
    --region eu-central-1 \
    --publish

if [ $? -eq 0 ]; then
    echo "✅ Lambda function updated successfully to preserve original categories!"
    
    # Get updated function info
    echo "📋 Updated Lambda function details:"
    aws lambda get-function \
        --function-name uhabits-sync-prod \
        --region eu-central-1 \
        --query '{State:Configuration.State,LastModified:Configuration.LastModified,CodeSize:Configuration.CodeSize,Handler:Configuration.Handler}' \
        --output table
    
    echo ""
    echo "🔄 Category Handling Changes:"
    echo " - Original categories from the mobile app are now preserved exactly as entered"
    echo " - No automatic standardization is performed"
    echo " - Only habits with completely missing categories will receive the default 'Uncategorized' value"
    echo ""
    echo "📱 Next steps:"
    echo " 1. Sync your app to upload habits with your original categories"
    echo " 2. Check your dashboard to confirm categories appear as entered in the app"
    echo " 3. Edit categories directly in the app if you need to make changes"
else
    echo "❌ Lambda function update failed!"
    echo "Please check your AWS credentials and try again."
    exit 1
fi
