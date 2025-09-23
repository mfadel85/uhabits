#!/bin/bash

# Safe Lambda deployment script with error handling
# Enhanced deployment for uHabits cloud sync function

set -e  # Exit on any error

FUNCTION_NAME="uhabits-sync-prod"
REGION="eu-central-1"
ZIP_FILE="function.zip"

echo "🚀 Starting Lambda function deployment..."

# Check if ZIP file exists
if [ ! -f "$ZIP_FILE" ]; then
    echo "❌ Error: $ZIP_FILE not found!"
    exit 1
fi

# Check file size (warn if > 50MB)
FILE_SIZE=$(stat -c%s "$ZIP_FILE" 2>/dev/null || stat -f%z "$ZIP_FILE" 2>/dev/null || echo "0")
if [ "$FILE_SIZE" -gt 52428800 ]; then
    echo "⚠️  Warning: Large deployment package (${FILE_SIZE} bytes)"
fi

echo "📦 Deploying ${ZIP_FILE} (${FILE_SIZE} bytes) to ${FUNCTION_NAME}..."

# Attempt deployment with error handling
if aws lambda update-function-code \
    --function-name "$FUNCTION_NAME" \
    --zip-file "fileb://$ZIP_FILE" \
    --region "$REGION" \
    --cli-read-timeout 120 \
    --cli-connect-timeout 60; then
    
    echo "✅ Lambda function updated successfully!"
    
    # Wait for update to complete
    echo "⏳ Waiting for function to be ready..."
    aws lambda wait function-updated \
        --function-name "$FUNCTION_NAME" \
        --region "$REGION"
    
    # Get function info
    echo "📊 Function status:"
    aws lambda get-function \
        --function-name "$FUNCTION_NAME" \
        --region "$REGION" \
        --query '{State:Configuration.State,LastModified:Configuration.LastModified,Runtime:Configuration.Runtime,CodeSize:Configuration.CodeSize}' \
        --output table
        
else
    echo "❌ Lambda deployment failed!"
    echo "🔍 Checking function status..."
    
    # Check if function exists
    if aws lambda get-function \
        --function-name "$FUNCTION_NAME" \
        --region "$REGION" \
        --query 'Configuration.State' \
        --output text 2>/dev/null; then
        echo "ℹ️  Function exists but update failed"
    else
        echo "❌ Function not found - check function name and region"
    fi
    
    exit 1
fi

echo "🎉 Deployment completed successfully!"
