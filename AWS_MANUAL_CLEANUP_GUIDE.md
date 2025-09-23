# AWS Manual Cleanup Guide - Frankfurt Region

## Step-by-Step Cleanup Process

### 1. CloudFormation Stack (REQUIRED)
**Location**: AWS Console → CloudFormation → eu-central-1 (Frankfurt)

**Steps**:
1. Find stack: `uhabits-analytics-prod`
2. Click on the stack name
3. Click **"Delete"** button
4. If deletion fails, try **"Delete stack"** with **"Retain resources"** option
5. Confirm deletion

**Why**: The stack is stuck in DELETE_FAILED state and blocking new deployments

---

### 2. Lambda Function (IF IT EXISTS)
**Location**: AWS Console → Lambda → eu-central-1 (Frankfurt)

**Look for function named**:
- `uhabits-sync-prod`
- `HabitSyncFunction`
- Any function containing "uhabits" or "habit"

**Steps**:
1. Select the function
2. Click **Actions** → **Delete**
3. Type "delete" to confirm

**Note**: Only delete if you see it in the console

---

### 3. DynamoDB Table (IF IT EXISTS)
**Location**: AWS Console → DynamoDB → eu-central-1 (Frankfurt)

**Look for table named**:
- `uHabits-Analytics-prod`
- Any table containing "uhabits" or "habit"

**Steps**:
1. Select the table
2. Click **Delete**
3. Type the table name to confirm
4. Uncheck backup options (to avoid extra costs)

**Note**: Only delete if you see it in the console

---

### 4. API Gateway (IF IT EXISTS)
**Location**: AWS Console → API Gateway → eu-central-1 (Frankfurt)

**Look for API named**:
- `uhabits-api-prod`
- Any API containing "uhabits"

**Steps**:
1. Select the API
2. Click **Actions** → **Delete**
3. Type "confirm" to delete

---

### 5. S3 Bucket (OPTIONAL - SAM MANAGED)
**Location**: AWS Console → S3

**Look for bucket named**:
- `aws-sam-cli-managed-default-samclisourcebucket-*`

**Steps**:
1. Click on bucket name
2. **Empty** the bucket first (delete all objects)
3. Then **Delete** the bucket

**Note**: This is optional - SAM can reuse existing buckets

---

### 6. CloudWatch Log Groups (OPTIONAL)
**Location**: AWS Console → CloudWatch → Logs → Log groups → eu-central-1

**Look for log groups named**:
- `/aws/lambda/uhabits-sync-prod`
- Any log group containing "uhabits"

**Steps**:
1. Select log group
2. Click **Actions** → **Delete log group**
3. Confirm deletion

---

## Quick Checklist

### ✅ **Must Delete** (to fix deployment)
- [ ] CloudFormation stack: `uhabits-analytics-prod`

### 🔄 **Check and Delete if Present**
- [ ] Lambda function: `uhabits-sync-prod` or similar
- [ ] DynamoDB table: `uHabits-Analytics-prod` or similar
- [ ] API Gateway: `uhabits-api-prod` or similar

### 📋 **Optional Cleanup**
- [ ] S3 bucket: `aws-sam-cli-managed-*` (saves a few cents)
- [ ] CloudWatch log groups: `/aws/lambda/uhabits-*`

---

## After Cleanup

Once you've deleted the resources (especially the CloudFormation stack), run:

```bash
# Verify cleanup
aws cloudformation list-stacks --region eu-central-1 --query 'StackSummaries[?StackName==`uhabits-analytics-prod`]' --output table

# Should return empty table, then deploy fresh
./clean_deploy.sh
```

---

## Alternative: Use Different Stack Name

If deletion is problematic, you can also deploy with a new stack name:

```bash
cd aws-lambda
sam deploy --stack-name uhabits-analytics-prod-v2 --region eu-central-1 --resolve-s3 --capabilities CAPABILITY_IAM --parameter-overrides Environment=prod
```

This bypasses the stuck stack entirely.

---

## Most Important

**The #1 priority is deleting the CloudFormation stack `uhabits-analytics-prod`**. Everything else is secondary and can be cleaned up later if needed.
