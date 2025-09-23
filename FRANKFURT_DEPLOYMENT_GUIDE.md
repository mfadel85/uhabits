# Frankfurt Lambda Integration Guide

## Current Status ✅
- **Lambda Function**: Deployed in Frankfurt (eu-central-1)
- **Runtime**: Python 3.12
- **Function Name**: uhabits-sync-prod (or similar)

## Next Steps

### 1. Configure AWS CLI (Required)
```bash
aws configure
```
**Important Settings:**
- **Region**: `eu-central-1` (Frankfurt)
- **Output**: `json`
- Use the same access keys from your VS Code setup

### 2. Verify Setup
```bash
./verify_frankfurt_setup.sh
```
This will check:
- AWS CLI connection
- Existing Lambda functions
- Current DynamoDB tables
- Deployment readiness

### 3. Deploy Complete Infrastructure
```bash
./deploy_cloud_analytics.sh
```

## What the Deployment Will Do

### ✅ **Keep Your Existing Lambda**
- Updates function configuration (environment variables)
- Adds DynamoDB permissions
- Connects to API Gateway

### ✅ **Create New Infrastructure**
- **DynamoDB Table**: `uHabits-Analytics-prod` in Frankfurt
- **API Gateway**: Regional endpoint for mobile app
- **API Keys**: Secure authentication
- **CloudWatch Logs**: Function monitoring

### ✅ **Generate Configuration Files**
- **`android_config.json`**: Mobile app settings with Frankfurt endpoints
- **`powerbi_connection.txt`**: PowerBI Pro integration details
- **`check_deployment.sh`**: Status monitoring script

## Expected Costs 💰
- **Monthly**: ~€0.01 (within AWS free tier)
- **DynamoDB**: Free tier covers 25GB + 200M requests
- **Lambda**: Free tier covers 1M requests + 400K GB-seconds
- **API Gateway**: Free tier covers 1M requests

## Post-Deployment

### Mobile App Integration
1. Use generated `android_config.json`
2. Update Android app with Frankfurt API endpoint
3. Test cloud sync functionality

### PowerBI Setup
1. Follow `powerbi_connection.txt` instructions
2. Connect to Frankfurt DynamoDB table
3. Create analytics dashboard

## Troubleshooting

### If Lambda Function Name Differs
Update the deployment script if your function has a different name:
```bash
# Edit deploy_cloud_analytics.sh
# Change FUNCTION_NAME to match your actual function name
```

### Region Mismatch
Ensure all tools use Frankfurt:
- AWS CLI: `eu-central-1`
- VS Code AWS Toolkit: `eu-central-1`
- Mobile app config: Frankfurt endpoints

### Permissions Issues
The deployment automatically adds required DynamoDB permissions to your Lambda function.

## Support
- Check logs: AWS Console → CloudWatch → Log Groups
- Monitor costs: AWS Console → Billing Dashboard
- Test API: Use generated endpoints in `android_config.json`
