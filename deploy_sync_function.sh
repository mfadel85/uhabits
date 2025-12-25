#!/bin/bash

echo "🔄 Deploying uHabits Sync Lambda Function..."

# Check if AWS CLI is configured
if ! aws sts get-caller-identity > /dev/null 2>&1; then
    echo "❌ AWS CLI not configured. Please run 'aws configure' first."
    exit 1
fi

# Change to the aws-lambda directory
cd aws-lambda

echo "📦 Creating deployment package..."

# Create a temporary directory for the deployment package
rm -rf /tmp/sync-deployment
mkdir -p /tmp/sync-deployment

# Copy the sync function
cp habit_sync_function.py /tmp/sync-deployment/

# Change to deployment directory
cd /tmp/sync-deployment

# Create the zip file
zip -r sync-function.zip .

echo "🚀 Updating Lambda function..."

# Update the function code
aws lambda update-function-code \
    --function-name uhabits-sync-prod \
    --zip-file fileb://sync-function.zip

if [ $? -eq 0 ]; then
    echo "✅ Successfully updated uhabits-sync-prod function"
    
    # Update function configuration to ensure correct settings
    echo "⚙️ Updating function configuration..."
    
    aws lambda update-function-configuration \
        --function-name uhabits-sync-prod \
        --runtime python3.10 \
        --handler habit_sync_function.lambda_handler \
        --timeout 30 \
        --memory-size 256 \
        --environment Variables='{TABLE_NAME=uHabits-Analytics-prod}'
    
    if [ $? -eq 0 ]; then
        echo "✅ Function configuration updated successfully"
        echo ""
        echo "🧪 Testing the updated function..."
        
        # Test the function
        echo '{"body": "{\"user_id\":\"test_user\",\"sync_timestamp\":1697654400000,\"habits_data\":[]}"}' > test-payload.json
        
        aws lambda invoke \
            --function-name uhabits-sync-prod \
            --payload fileb://test-payload.json \
            --cli-binary-format raw-in-base64-out \
            response.json
        
        echo "Response:"
        cat response.json
        echo ""
        
        # Check if the response indicates success
        if grep -q '"status":"success"' response.json; then
            echo "✅ Function test successful!"
        else
            echo "⚠️ Function test returned an error. Check the response above."
        fi
    else
        echo "❌ Failed to update function configuration"
        exit 1
    fi
else
    echo "❌ Failed to update function code"
    exit 1
fi

# Clean up
rm -rf /tmp/sync-deployment

echo ""
echo "🎉 Sync function deployment complete!"
echo ""
echo "📱 Your mobile app should now be able to sync successfully."
echo "🔗 API Endpoint: https://jodcprzip3.execute-api.eu-central-1.amazonaws.com/prod/sync"