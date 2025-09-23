# Step 2: Create Lambda Function from Scratch

## Create New Lambda Function
**Service**: Lambda → Functions → Create function
**Region**: eu-central-1 (Frankfurt)

### Basic Information
- **Function name**: `uhabits-sync-prod`
- **Runtime**: Python 3.12
- **Architecture**: x86_64
- **Execution role**: Create a new role with basic Lambda permissions

### Function Code
1. **Delete the default code** in the code editor
2. **Copy and paste** the entire content from your `habit-sync-function.py` file
3. **Deploy** the function

### Environment Variables
In the **Configuration** tab → **Environment variables**:
- **TABLE_NAME**: `uHabits-Analytics-prod`
- **ENVIRONMENT**: `prod`

### Execution Role Permissions
1. Go to **Configuration** → **Permissions**
2. Click on the **Execution role** link (opens IAM)
3. **Attach policies**:
   - `AmazonDynamoDBFullAccess`
   - OR create custom policy with these permissions:
     ```json
     {
         "Version": "2012-10-17",
         "Statement": [
             {
                 "Effect": "Allow",
                 "Action": [
                     "dynamodb:PutItem",
                     "dynamodb:GetItem",
                     "dynamodb:Query",
                     "dynamodb:BatchWriteItem"
                 ],
                 "Resource": "arn:aws:dynamodb:eu-central-1:328600977654:table/uHabits-Analytics-prod"
             }
         ]
     }
     ```

### Test the Function
1. Go to **Test** tab
2. **Create new test event**:
   - **Event name**: `test-habit-sync`
   - **Event JSON**:
     ```json
     {
         "user_id": "user_primary",
         "sync_timestamp": 1726836000000,
         "summary_metrics": {
             "total_habits": 1,
             "active_habits": 1,
             "weighted_success_rate": 1.0
         },
         "habits_data": [{
             "id": "test",
             "name": "Test Habit",
             "priority": "NORMAL",
             "weight": 1.0,
             "success_rate": 1.0,
             "weighted_success_rate": 1.0
         }]
     }
     ```
3. **Run test** - should return success response

---

## Alternative: Upload Function Code via CLI

If you prefer to use the AWS CLI:

```bash
# Zip your function code
cd /home/muosman/uHabits/uhabits/aws-lambda
zip function.zip habit-sync-function.py

# Create the function
aws lambda create-function \
    --function-name uhabits-sync-prod \
    --runtime python3.12 \
    --role arn:aws:iam::328600977654:role/lambda-execution-role \
    --handler habit-sync-function.lambda_handler \
    --zip-file fileb://function.zip \
    --region eu-central-1

# Add environment variables
aws lambda update-function-configuration \
    --function-name uhabits-sync-prod \
    --environment Variables='{TABLE_NAME=uHabits-Analytics-prod,ENVIRONMENT=prod}' \
    --region eu-central-1
```

---

## Quick Verification

After creating the Lambda function:

```bash
# Check if function exists
aws lambda get-function --function-name uhabits-sync-prod --region eu-central-1

# Test the function
aws lambda invoke \
    --function-name uhabits-sync-prod \
    --payload '{"user_id":"test","sync_timestamp":1726836000000,"habits_data":[{"id":"test","name":"Test"}]}' \
    --region eu-central-1 \
    response.json

# Check response
cat response.json
```

---

## Function Details Summary

After creation, your Lambda function will have:

- **Name**: `uhabits-sync-prod`
- **Runtime**: Python 3.12
- **Handler**: `habit-sync-function.lambda_handler`
- **Environment Variables**: 
  - `TABLE_NAME`: `uHabits-Analytics-prod`
  - `ENVIRONMENT`: `prod`
- **Permissions**: DynamoDB read/write access

Let me know when you've created the Lambda function and I'll help you with Step 3 (API Gateway)!
