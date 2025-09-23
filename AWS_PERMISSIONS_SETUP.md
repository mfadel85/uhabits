# AWS IAM Permissions Setup for uHabits Deployment

## Quick Setup (Recommended)

### Option 1: AWS Console (Easiest)
1. **Go to AWS IAM Console**: https://console.aws.amazon.com/iam/
2. **Navigate to Users** → Find your user `vscode-developer`
3. **Click on the user** → Go to "Permissions" tab
4. **Click "Add permissions"** → "Attach policies directly"
5. **Search and attach these AWS managed policies**:
   - `AWSLambda_FullAccess`
   - `AmazonDynamoDBFullAccess` 
   - `AmazonAPIGatewayAdministrator`
   - `AWSCloudFormationFullAccess`
   - `IAMFullAccess` (needed for SAM to create roles)

### Option 2: Custom Policy (More Secure)
1. **Go to IAM Console** → **Policies** → **Create Policy**
2. **Switch to JSON tab**
3. **Copy the contents** of `aws-deployment-policy.json` (created above)
4. **Create the policy** with name: `uHabitsDeploymentPolicy`
5. **Attach to your user**: Users → `vscode-developer` → Add permissions → Attach existing policies → Select `uHabitsDeploymentPolicy`

### Option 3: AWS CLI (Advanced)
```bash
# Create the policy
aws iam create-policy \
    --policy-name uHabitsDeploymentPolicy \
    --policy-document file://aws-deployment-policy.json \
    --region eu-central-1

# Attach to your user (replace with your actual policy ARN)
aws iam attach-user-policy \
    --user-name vscode-developer \
    --policy-arn arn:aws:iam::328600977654:policy/uHabitsDeploymentPolicy \
    --region eu-central-1
```

## What These Permissions Enable

### ✅ **Lambda Functions**
- List, create, update existing functions
- Configure environment variables
- Set up API Gateway triggers

### ✅ **DynamoDB Tables**
- Create tables for habit data storage
- Configure indexes and TTL
- Read/write data operations

### ✅ **API Gateway**
- Create REST APIs for mobile app
- Set up API keys and usage plans
- Configure CORS and authentication

### ✅ **CloudFormation**
- Deploy infrastructure as code
- Update existing stacks
- Monitor deployment progress

### ✅ **S3 & IAM (SAM Requirements)**
- S3: Store deployment artifacts
- IAM: Create service roles for Lambda/API Gateway

## Security Notes

### 🔒 **Production Recommendations**
- Use **Option 2 (Custom Policy)** for minimal required permissions
- The custom policy is scoped to only what's needed for deployment
- Consider using temporary elevated permissions for deployment only

### 🛡️ **Alternative: Deployment Role**
If you prefer not to expand user permissions:
1. Create a deployment role with these permissions
2. Use `aws sts assume-role` during deployment
3. Revert to minimal permissions afterward

## After Adding Permissions

### Test the Setup
```bash
# This should work now
./verify_frankfurt_setup.sh

# Then proceed with deployment
./deploy_cloud_analytics.sh
```

### Expected Output
- ✅ List Lambda functions (find your existing function)
- ✅ List DynamoDB tables 
- ✅ Deploy complete infrastructure
- ✅ Generate mobile app configuration

## Quick Permission Check Commands

```bash
# Test Lambda permissions
aws lambda list-functions --region eu-central-1 --max-items 5

# Test DynamoDB permissions  
aws dynamodb list-tables --region eu-central-1

# Test CloudFormation permissions
aws cloudformation list-stacks --region eu-central-1 --max-items 5
```

If these commands work, you're ready for deployment! 🚀
