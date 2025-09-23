# Manual AWS Resource Creation Guide - Frankfurt Region

## Resources to Create (in order)

### 1. DynamoDB Table
**Service**: DynamoDB → Tables → Create table
**Region**: eu-central-1 (Frankfurt)

**Settings**:
- **Table name**: `uHabits-Analytics-prod`
- **Partition key**: `PK` (String)
- **Sort key**: `SK` (String)
- **Table settings**: Default settings
- **Billing mode**: On-demand
- **Tags**: 
  - Key: `Project`, Value: `uHabits-Analytics`
  - Key: `Environment`, Value: `prod`

**After creation, add Local Secondary Index**:
- **Index name**: `DateIndex`
- **Sort key**: `sync_date` (String)
- **Projected attributes**: All

---

### 2. Lambda Function (Update Existing)
**Service**: Lambda → Functions → [Your existing function]
**Region**: eu-central-1 (Frankfurt)

**Environment Variables to Add**:
- **TABLE_NAME**: `uHabits-Analytics-prod`
- **ENVIRONMENT**: `prod`

**Execution Role Permissions** (add these policies):
- `AmazonDynamoDBFullAccess` (or custom policy for the table)

---

### 3. API Gateway
**Service**: API Gateway → Create API → REST API
**Region**: eu-central-1 (Frankfurt)

**Settings**:
- **API name**: `uhabits-api-prod`
- **API type**: Regional
- **Endpoint type**: Regional

**Create Resource**:
- **Resource name**: `sync`
- **Resource path**: `/sync`
- **Enable CORS**: Yes

**Create Method** (under /sync resource):
- **Method**: POST
- **Integration type**: Lambda Function
- **Lambda function**: [Your uhabits function]
- **Use Lambda Proxy integration**: Yes

**Enable CORS** (on /sync resource):
- **Access-Control-Allow-Origin**: `*`
- **Access-Control-Allow-Headers**: `Content-Type,X-Amz-Date,Authorization,X-Api-Key`
- **Access-Control-Allow-Methods**: `GET,POST,OPTIONS`

**Deploy API**:
- **Deployment stage**: `prod`
- **Stage description**: `Production stage`

---

### 4. API Key & Usage Plan
**In API Gateway console**:

**Create API Key**:
- **Name**: `uhabits-api-key-prod`
- **Auto Generate**: Yes
- **Enabled**: Yes

**Create Usage Plan**:
- **Name**: `uhabits-usage-plan-prod`
- **Throttle**: 5 requests per second, 10 burst
- **Quota**: 1000 requests per month
- **Associated API Stages**: Select your API → prod stage

**Associate API Key with Usage Plan**:
- Add the API key you created to the usage plan

**Enable API Key Requirement**:
- Go to your API → Resources → /sync → POST method
- Method Request → API Key Required: true
- Deploy API again

---

### 5. CloudWatch Log Group (Optional)
**Service**: CloudWatch → Logs → Log groups
**Region**: eu-central-1 (Frankfurt)

**Settings**:
- **Log group name**: `/aws/lambda/uhabits-sync-prod`
- **Retention**: 7 days
- **Tags**: Project: uHabits-Analytics

---

## Resource Summary

After creation, you should have:

| Resource Type | Name/Identifier | Purpose |
|---------------|-----------------|---------|
| DynamoDB Table | `uHabits-Analytics-prod` | Data storage |
| Lambda Function | [Your existing function] | Processing logic |
| API Gateway | `uhabits-api-prod` | Mobile app endpoint |
| API Key | `uhabits-api-key-prod` | Authentication |
| Usage Plan | `uhabits-usage-plan-prod` | Rate limiting |
| Log Group | `/aws/lambda/uhabits-sync-prod` | Function logs |

---

## After Manual Creation

Once you've created these resources, we can:

1. **Generate configuration files** for your Android app
2. **Test the API endpoint** with sample data
3. **Set up PowerBI connection** details
4. **Create monitoring scripts** for the infrastructure
5. **Implement the mobile app integration**

---

## Quick Verification Commands

After creating the resources, run these to verify:

```bash
# Check DynamoDB table
aws dynamodb describe-table --table-name uHabits-Analytics-prod --region eu-central-1

# Check Lambda function
aws lambda get-function --function-name [YOUR_FUNCTION_NAME] --region eu-central-1

# Check API Gateway
aws apigateway get-rest-apis --region eu-central-1 --query 'items[?name==`uhabits-api-prod`]'
```

---

## Estimated Time
- DynamoDB Table: 2 minutes
- Lambda Configuration: 2 minutes  
- API Gateway Setup: 10 minutes
- API Key & Usage Plan: 5 minutes
- **Total**: ~20 minutes

Let me know when you've created these resources and I'll help with the next steps!
