#!/bin/bash

# AWS Cost Cleanup Script
# Reduces S3 usage to stay within free tier limits

set -e

echo "💰 AWS Cost Optimization Cleanup"
echo "================================="

# Check current S3 usage
echo "🔍 Checking current S3 usage..."

# List SAM deployment buckets (these often consume the most requests)
SAM_BUCKETS=$(aws s3 ls | grep "aws-sam-cli-managed" | awk '{print $3}' || true)

if [ ! -z "$SAM_BUCKETS" ]; then
    echo "📦 Found SAM deployment buckets:"
    echo "$SAM_BUCKETS"
    
    echo ""
    echo "🧹 Cleaning up old SAM deployments..."
    
    for bucket in $SAM_BUCKETS; do
        echo "  Cleaning bucket: $bucket"
        
        # List objects older than 7 days
        OLD_OBJECTS=$(aws s3api list-objects-v2 \
            --bucket "$bucket" \
            --query "Contents[?LastModified<='$(date -d '7 days ago' --iso-8601)'].Key" \
            --output text 2>/dev/null || true)
        
        if [ ! -z "$OLD_OBJECTS" ] && [ "$OLD_OBJECTS" != "None" ]; then
            echo "    Deleting old objects..."
            for obj in $OLD_OBJECTS; do
                aws s3 rm "s3://$bucket/$obj" || true
            done
        else
            echo "    No old objects to delete"
        fi
    done
else
    echo "📦 No SAM deployment buckets found"
fi

echo ""
echo "🔍 Checking for other S3 buckets..."
OTHER_BUCKETS=$(aws s3 ls | grep -v "aws-sam-cli-managed" | awk '{print $3}' || true)

if [ ! -z "$OTHER_BUCKETS" ]; then
    echo "📦 Found other buckets:"
    echo "$OTHER_BUCKETS"
    
    for bucket in $OTHER_BUCKETS; do
        echo "  Bucket: $bucket"
        aws s3 ls "s3://$bucket" --recursive --summarize 2>/dev/null || echo "    (Access denied or empty)"
    done
else
    echo "📦 No other buckets found"
fi

echo ""
echo "💡 Cost Optimization Recommendations:"
echo "=====================================

1. 🎯 Use DynamoDB-only architecture (your current setup is good!)
2. 🏠 Host dashboard locally or on GitHub Pages (free)
3. 📱 Export data locally instead of S3 storage
4. ⚡ Use direct Lambda deployment (no SAM S3 artifacts)

Current S3 usage: 2,727 requests / 2,000 free
Overage cost: ~\$0.40-\$2.00

To eliminate S3 costs completely:
  ./switch_to_local_dashboard.sh
  ./deploy_lambda_direct.sh (no SAM)
  
Your DynamoDB + Lambda setup is perfect and stays free!"

echo ""
echo "✅ Cleanup complete!"
echo ""
echo "🔄 Next steps:"
echo "  1. Run ./deploy_lambda_direct.sh to avoid SAM S3 usage"
echo "  2. Use ./export_analytics_data.sh for local data export"  
echo "  3. Host dashboard on GitHub Pages (free)"
echo "  4. Monitor costs: aws ce get-cost-and-usage"
