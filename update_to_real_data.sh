#!/bin/bash

echo "🔄 UPDATING LAMBDA TO USE REAL DATA"
echo "==================================="
echo ""
echo "📦 New zip file: minimal_lambda_real_data.zip (3.1KB)"
echo "🗄️  Now connects to: uHabits-Analytics-prod DynamoDB table"
echo "📊 Will process your 2,315+ real habit records"
echo ""
echo "🔧 UPLOAD STEPS:"
echo "1. Go to AWS Lambda Console: uhabits-dashboard-api"
echo "2. Upload: minimal_lambda_real_data.zip"
echo "3. Handler: minimal_api.lambda_handler (same as before)"
echo "4. Click 'Deploy'"
echo ""
echo "⚡ IMPORTANT: Lambda needs DynamoDB permissions!"
echo "If you get permission errors, add this policy to Lambda role:"
echo ""
echo '{'
echo '  "Version": "2012-10-17",'
echo '  "Statement": ['
echo '    {'
echo '      "Effect": "Allow",'
echo '      "Action": ['
echo '        "dynamodb:Scan",'
echo '        "dynamodb:Query",'
echo '        "dynamodb:GetItem"'
echo '      ],'
echo '      "Resource": "arn:aws:dynamodb:eu-central-1:*:table/uHabits-Analytics-prod"'
echo '    }'
echo '  ]'
echo '}'
echo ""
echo "🧪 WHAT THE NEW FUNCTION DOES:"
echo "• Scans uHabits-Analytics-prod table"
echo "• Processes up to 1,000 records (to avoid timeout)"
echo "• Groups habits by category automatically"
echo "• Calculates real performance scores"
echo "• Falls back to mock data if no real data found"
echo ""
echo "🔍 TESTING AFTER UPLOAD:"
curl -s "https://bhg1kt9cf2.execute-api.eu-central-1.amazonaws.com/prod/api/groups" -H "x-api-key: Y1leD4smWeX3yfCRM9Sv8R1Jo7g9zusx08Xo4y7o" | jq '.summary.data_source // "Upload not complete yet"'
echo ""
echo "✅ Look for: 'Real DynamoDB Data' instead of 'Mock Data'"
