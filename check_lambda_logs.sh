#!/bin/bash

echo "🔍 Checking CloudWatch logs for Lambda function..."

# Get the latest log stream
LOG_STREAM=$(aws logs describe-log-streams \
    --log-group-name "/aws/lambda/uhabits-dashboard-api" \
    --order-by LastEventTime \
    --descending \
    --max-items 1 \
    --region eu-central-1 \
    --query 'logStreams[0].logStreamName' \
    --output text)

if [ "$LOG_STREAM" = "None" ] || [ -z "$LOG_STREAM" ]; then
    echo "❌ No log streams found. The Lambda function may not have been invoked yet."
    echo "🧪 Triggering a test call..."
    curl -s "https://bhg1kt9cf2.execute-api.eu-central-1.amazonaws.com/prod/api/groups" > /dev/null
    sleep 3
    
    # Try again
    LOG_STREAM=$(aws logs describe-log-streams \
        --log-group-name "/aws/lambda/uhabits-dashboard-api" \
        --order-by LastEventTime \
        --descending \
        --max-items 1 \
        --region eu-central-1 \
        --query 'logStreams[0].logStreamName' \
        --output text)
fi

if [ "$LOG_STREAM" != "None" ] && [ -n "$LOG_STREAM" ]; then
    echo "📝 Latest log stream: $LOG_STREAM"
    echo "📋 Recent log events:"
    echo "============================================="
    
    aws logs get-log-events \
        --log-group-name "/aws/lambda/uhabits-dashboard-api" \
        --log-stream-name "$LOG_STREAM" \
        --region eu-central-1 \
        --query 'events[-20:].message' \
        --output text
        
    echo "============================================="
    echo "✅ Check complete. Look for DynamoDB connection errors above."
else
    echo "❌ Still no log streams found. There may be an issue with the Lambda function."
    echo "💡 Try testing the API endpoint manually:"
    echo "   https://bhg1kt9cf2.execute-api.eu-central-1.amazonaws.com/prod/api/groups"
fi
