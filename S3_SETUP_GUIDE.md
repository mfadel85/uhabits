# S3 Permissions Setup for SAM CLI Deployment

## Why S3 is Needed for SAM Deployment

SAM CLI uses S3 to:
1. **Upload Lambda code packages** (your Python function code)
2. **Store CloudFormation templates** (processed versions)
3. **Cache deployment artifacts** (for faster subsequent deployments)

## Setup Options

### Option 1: AWS Console (Recommended)
1. **Go to IAM Console**: https://console.aws.amazon.com/iam/
2. **Navigate to Users** → `vscode-developer` → **Permissions** tab
3. **Add permissions** → **Attach policies directly**
4. **Search for and attach**: `AmazonS3FullAccess`

### Option 2: Custom S3 Policy (More Secure)
1. **Create custom policy** using `s3-deployment-policy.json`
2. **In IAM Console** → **Policies** → **Create Policy** → **JSON**
3. **Copy contents** of `s3-deployment-policy.json`
4. **Name**: `SAMDeploymentS3Policy`
5. **Attach to user**: `vscode-developer`

### Option 3: AWS CLI
```bash
# Create the S3 policy
aws iam create-policy \
    --policy-name SAMDeploymentS3Policy \
    --policy-document file://s3-deployment-policy.json \
    --region eu-central-1

# Attach to your user
aws iam attach-user-policy \
    --user-name vscode-developer \
    --policy-arn arn:aws:iam::328600977654:policy/SAMDeploymentS3Policy
```

## What These S3 Permissions Allow

### ✅ **SAM CLI Operations**
- Create deployment buckets (SAM manages these automatically)
- Upload Lambda function code as ZIP files
- Store and retrieve CloudFormation templates
- Cache artifacts for faster deployments

### ✅ **Bucket Naming Pattern**
SAM creates buckets with names like:
- `aws-sam-cli-managed-default-samclisourcebucket-xxxxx`
- `sam-deployment-bucket-xxxxx`

### 🔒 **Security Scope**
The custom policy only allows access to SAM-related buckets, not all S3 buckets.

## Test S3 Permissions

After adding permissions, test with:
```bash
# List buckets (should work)
aws s3 ls --region eu-central-1

# Test SAM bucket creation (dry run)
sam deploy --guided --dry-run
```

## Common S3 Error Messages

### ❌ "Access Denied" when creating bucket
**Solution**: Add `s3:CreateBucket` permission

### ❌ "Access Denied" when uploading artifacts  
**Solution**: Add `s3:PutObject` permission

### ❌ "Cannot list buckets"
**Solution**: Add `s3:ListAllMyBuckets` permission

## SAM Bucket Management

### 🗂️ **Automatic Bucket Creation**
- SAM creates deployment buckets automatically
- One bucket per region per AWS account
- Buckets are reused across deployments

### 🧹 **Cleanup (Optional)**
After deployment, you can keep or delete SAM buckets:
```bash
# List SAM buckets
aws s3 ls | grep sam

# Delete a specific SAM bucket (if needed)
aws s3 rb s3://bucket-name --force
```

## Next Steps After S3 Setup

1. **Verify all permissions**:
   ```bash
   ./verify_frankfurt_setup.sh
   ```

2. **Deploy your infrastructure**:
   ```bash
   ./deploy_cloud_analytics.sh
   ```

3. **Monitor S3 usage**:
   - AWS Console → S3 → Check for SAM buckets
   - Small files (~KB to MB range)
   - Very low costs (cents per month)

## Cost Impact

### 💰 **S3 Costs for SAM**
- **Storage**: ~$0.001/month (few MB of artifacts)
- **Requests**: ~$0.001/month (minimal API calls)
- **Total**: Less than $0.01/month

The S3 usage for SAM deployments is minimal and typically within the AWS free tier!
