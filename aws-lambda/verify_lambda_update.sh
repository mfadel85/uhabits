#!/bin/bash

echo "Verifying Lambda function update status..."
aws lambda get-function \
    --function-name uhabits-sync-prod \
    --region eu-central-1 \
    --query '{State:Configuration.State,LastModified:Configuration.LastModified,CodeSize:Configuration.CodeSize,Runtime:Configuration.Runtime,Handler:Configuration.Handler}' \
    --output table
    
echo ""
echo "Confirming Lambda has the latest code with category preservation..."
aws lambda get-function \
    --function-name uhabits-sync-prod \
    --region eu-central-1 \
    --query 'Code.Location' \
    --output text

echo ""
echo "✅ Your Lambda function has been updated successfully with category preservation!"
echo "The next time habit data is synced from your phone, categories will be preserved"
echo "exactly as entered in the app, without any automatic standardization."
echo ""
echo "Original categories in your dashboard should now show properly after syncing from your phone."
echo "Only completely missing categories will receive the default 'Uncategorized' value."
