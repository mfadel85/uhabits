# 💰 AWS Cost Optimization Guide

## ⚠️ Current Issue: S3 Free Tier Exceeded
- **Free Tier Limit**: 2,000 S3 requests for 12 months
- **Current Usage**: 2,727 requests 
- **Overage**: 727 requests (~$0.40-$2.00 depending on request type)

## 🎯 Cost Optimization Solutions

### Option 1: Reduce S3 Usage (Immediate)
```bash
# Check current S3 usage
aws s3api list-buckets --query 'Buckets[].Name'
aws s3 ls s3://your-bucket-name --recursive --summarize

# Remove unnecessary files
aws s3 rm s3://your-bucket-name/old-files/ --recursive

# Set lifecycle policies to auto-delete old files
aws s3api put-bucket-lifecycle-configuration \
    --bucket your-bucket-name \
    --lifecycle-configuration file://lifecycle-policy.json
```

### Option 2: Switch to DynamoDB Only (Recommended)
Your current system uses DynamoDB for data storage, which is more cost-effective:

**DynamoDB Free Tier:**
- 25 GB storage (free forever)
- 25 read/write capacity units (free forever)
- Perfect for habit tracking data

**Current Lambda + DynamoDB Architecture:**
```
📱 Android App → 🌐 API Gateway → ⚡ Lambda → 📊 DynamoDB
```

### Option 3: Local-First Architecture (Cost-Free)
```bash
# Export data locally instead of cloud storage
./export_analytics_data.sh

# Use local dashboard
open advanced_dashboard.html

# Sync only when needed (manual export)
```

### Option 4: GitHub Pages Dashboard (Free)
Host your dashboard on GitHub Pages for free static hosting:

```bash
# Build static dashboard
mkdir docs
cp advanced_dashboard.html docs/index.html
cp test_group_selection.html docs/groups.html

# Enable GitHub Pages in repository settings
# Access at: https://mfadel85.github.io/uhabits/
```

## 📊 Current Architecture Analysis

### What's Using S3:
1. **CloudFormation Deployments** - Template storage
2. **Lambda Deployment Packages** - Code storage  
3. **SAM CLI Operations** - Build artifacts
4. **Backup Operations** - Data exports

### Cost-Effective Alternatives:

#### For CloudFormation:
```yaml
# Use inline templates instead of S3 storage
Resources:
  MyLambda:
    Type: AWS::Lambda::Function
    Properties:
      Code:
        ZipFile: |
          # Inline code here
```

#### For Data Export:
```bash
# Export directly to local instead of S3
aws dynamodb scan --table-name uHabits-Analytics-prod > local_export.json
```

#### For Dashboard Hosting:
```html
<!-- Use GitHub Pages or local file:// URLs -->
<script>
const API_URL = 'https://your-api-gateway-url.amazonaws.com/prod';
// No S3 storage needed for static assets
</script>
```

## 🔧 Implementation Steps

### Step 1: Clean Up S3 (Immediate Cost Reduction)
```bash
# List all S3 buckets
aws s3 ls

# Check SAM deployment bucket
aws s3 ls s3://aws-sam-cli-managed-default-samclisourcebucket-* --recursive

# Clean up old deployments
aws s3 rm s3://aws-sam-cli-managed-default-samclisourcebucket-* --recursive
```

### Step 2: Optimize Lambda Deployments
```bash
# Use smaller deployment packages
cd aws-lambda
zip -r function.zip live_dashboard_api.py
# Instead of using SAM which creates S3 artifacts

# Direct Lambda update (no S3)
aws lambda update-function-code \
    --function-name your-function-name \
    --zip-file fileb://function.zip
```

### Step 3: Enable Local-First Mode
```bash
# Create local analytics script
./create_local_analytics.sh

# Export data when needed
./export_analytics_data.sh

# View dashboard locally
open file:///path/to/advanced_dashboard.html
```

## 💡 Cost Monitoring Setup

### CloudWatch Billing Alerts:
```bash
# Set up billing alert for $1 threshold
aws cloudwatch put-metric-alarm \
    --alarm-name "AWS-Billing-Alert" \
    --alarm-description "Alert when AWS charges exceed $1" \
    --metric-name EstimatedCharges \
    --namespace AWS/Billing \
    --statistic Maximum \
    --period 86400 \
    --threshold 1.0 \
    --comparison-operator GreaterThanThreshold
```

### Monthly Cost Report:
```bash
# Check current month costs
aws ce get-cost-and-usage \
    --time-period Start=2025-09-01,End=2025-09-30 \
    --granularity MONTHLY \
    --metrics BlendedCost
```

## 🎯 Recommended Solution

**For your use case, I recommend Option 2 + 4:**

1. **Keep DynamoDB + Lambda** (within free tier)
2. **Remove S3 dependencies** (eliminate costs)
3. **Use GitHub Pages** for dashboard hosting (free)
4. **Export data locally** when needed (free)

This gives you:
- ✅ Real-time dashboard (GitHub Pages)
- ✅ Group analytics (Lambda + DynamoDB)
- ✅ $0 monthly cost (within free tiers)
- ✅ No S3 requests

Would you like me to implement this cost-free architecture?
