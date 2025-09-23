#!/bin/bash

# Simple Lambda code update script
# Bypasses hanging issues with direct base64 encoding

echo "🔄 Updating Lambda function code using base64 encoding..."

cd /home/muosman/uHabits/uhabits/aws-lambda

# Convert zip to base64
echo "📦 Converting function.zip to base64..."
BASE64_CODE=$(base64 -w 0 function.zip)

echo "☁️ Updating Lambda function via base64..."

# Use base64 instead of file upload
aws lambda update-function-code \
    --function-name uhabits-sync-prod \
    --zip-file "base64://$BASE64_CODE" \
    --region eu-central-1 \
    --cli-connect-timeout 30 \
    --cli-read-timeout 60

if [ $? -eq 0 ]; then
    echo "✅ Lambda function updated successfully!"
    
    # Get updated function info
    aws lambda get-function \
        --function-name uhabits-sync-prod \
        --region eu-central-1 \
        --query '{State:Configuration.State,LastModified:Configuration.LastModified,CodeSize:Configuration.CodeSize}' \
        --output table
else
    echo "❌ Lambda update failed!"
    exit 1
fi
