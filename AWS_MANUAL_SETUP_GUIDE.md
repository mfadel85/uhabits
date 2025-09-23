# 🌐 AWS Console Manual Setup Guide
# Complete step-by-step configuration for uHabits Analytics

## 📋 **Prerequisites**
- AWS Account created and logged in
- Browser open to AWS Console
- Have your Account ID ready (visible in top-right corner)

## 🗂️ **Setup Order (Important!)**

### 1. **DynamoDB Table** - Create First
**Location**: AWS Console → Database → DynamoDB → Tables → Create Table

#### Basic Information:
```
Table name: uHabits-Analytics-prod
Partition key: PK (String)
Sort key: SK (String)
```

#### Table Settings:
- **Table class**: DynamoDB Standard
- **Capacity mode**: On-demand
- **Billing mode**: Pay per request

#### Additional Settings:
**Encryption**:
- Encryption at rest: Owned by Amazon DynamoDB
- Encryption in transit: TLS

**Point-in-time recovery**:
- Status: Disabled (saves costs)

**Tags** (Optional):
```
Key: Project, Value: uHabits-Analytics
Key: Environment, Value: prod
Key: CostOptimized, Value: true
```

#### Time to Live (TTL):
1. After table creation, go to: Table → Additional settings → Time to Live
2. **TTL attribute**: `ttl`
3. **Status**: Enabled

#### Local Secondary Index:
1. During table creation, expand "Additional settings"
2. **Create local secondary index**:
   - Index name: `DateIndex`
   - Sort key: `sync_date` (String)
   - Attribute projections: All

**Click "Create table"** ✅

---

### 2. **Lambda Function** - Create Second
**Location**: AWS Console → Compute → Lambda → Functions → Create function

#### Basic Information:
```
Function name: uhabits-sync-prod
Runtime: Python 3.12
Architecture: x86_64
```

#### Permissions:
- **Execution role**: Create a new role with basic Lambda permissions
- (We'll enhance this role in step 3)

#### After Creation - Function Code:
1. **Code source**: Delete default code
2. **Paste this code**:

```python
import json
import boto3
import logging
import os
from datetime import datetime
from decimal import Decimal

logger = logging.getLogger()
logger.setLevel(logging.INFO)

dynamodb = boto3.resource('dynamodb')
table_name = os.environ['TABLE_NAME']
table = dynamodb.Table(table_name)

def lambda_handler(event, context):
    try:
        if 'body' in event:
            body = json.loads(event['body'])
        else:
            body = event
        
        logger.info(f"Processing sync request for user: {body.get('user_id', 'unknown')}")
        
        required_fields = ['user_id', 'sync_timestamp', 'habits_data']
        for field in required_fields:
            if field not in body:
                return create_response(400, f"Missing required field: {field}")
        
        result = process_habit_sync(body)
        
        return create_response(200, {
            "status": "success",
            "message": "Habit data synced successfully",
            "timestamp": datetime.utcnow().isoformat(),
            "processed": result
        })
        
    except Exception as e:
        logger.error(f"Sync error: {str(e)}")
        return create_response(500, f"Internal server error: {str(e)}")

def process_habit_sync(sync_data):
    user_id = sync_data['user_id']
    sync_timestamp = sync_data['sync_timestamp']
    
    def convert_floats(obj):
        if isinstance(obj, dict):
            return {k: convert_floats(v) for k, v in obj.items()}
        elif isinstance(obj, list):
            return [convert_floats(item) for item in obj]
        elif isinstance(obj, float):
            return Decimal(str(obj))
        else:
            return obj
    
    converted_data = convert_floats(sync_data)
    
    summary_item = {
        'PK': f"USER#{user_id}",
        'SK': f"SYNC#{sync_timestamp}",
        'user_id': user_id,
        'sync_timestamp': sync_timestamp,
        'sync_date': converted_data.get('sync_date'),
        'summary_metrics': converted_data.get('summary_metrics', {}),
        'priority_distribution': converted_data.get('priority_distribution', {}),
        'device_info': converted_data.get('device_info', {}),
        'metadata': converted_data.get('metadata', {}),
        'ttl': int(sync_timestamp / 1000) + (365 * 24 * 60 * 60)
    }
    
    habits_stored = 0
    with table.batch_writer() as batch:
        batch.put_item(Item=summary_item)
        
        for habit in converted_data.get('habits_data', []):
            habit_item = {
                'PK': f"USER#{user_id}",
                'SK': f"HABIT#{sync_timestamp}#{habit.get('id', 'unknown')}",
                'user_id': user_id,
                'sync_timestamp': sync_timestamp,
                'habit_id': habit.get('id'),
                'habit_name': habit.get('name'),
                'priority': habit.get('priority'),
                'weight': habit.get('weight'),
                'success_rate': habit.get('success_rate'),
                'weighted_success_rate': habit.get('weighted_success_rate'),
                'ttl': int(sync_timestamp / 1000) + (365 * 24 * 60 * 60)
            }
            batch.put_item(Item=habit_item)
            habits_stored += 1
    
    return {
        'summary': f"Stored 1 sync record and {habits_stored} habit records",
        'habits_count': habits_stored,
        'sync_timestamp': sync_timestamp
    }

def create_response(status_code, body):
    return {
        'statusCode': status_code,
        'headers': {
            'Content-Type': 'application/json',
            'Access-Control-Allow-Origin': '*',
            'Access-Control-Allow-Headers': 'Content-Type,X-Amz-Date,Authorization,X-Api-Key',
            'Access-Control-Allow-Methods': 'OPTIONS,POST'
        },
        'body': json.dumps(body, default=str)
    }
```

#### Environment Variables:
**Configuration → Environment variables**:
```
TABLE_NAME = uHabits-Analytics-prod
ENVIRONMENT = prod
```

#### Function Settings:
- **Timeout**: 30 seconds
- **Memory**: 256 MB

**Click "Deploy"** ✅

---

### 3. **IAM Role Enhancement** - Update Lambda Permissions
**Location**: AWS Console → Security → IAM → Roles

#### Find Lambda Role:
- Search for: `uhabits-sync-prod-role-xxxxx` (created automatically)
- Click on the role name

#### Add DynamoDB Permissions:
1. **Permissions tab** → **Add permissions** → **Create inline policy**
2. **Service**: DynamoDB
3. **Actions**: 
   - PutItem
   - GetItem
   - Query
   - BatchWriteItem
4. **Resources**: Specific
   - **Table ARN**: `arn:aws:dynamodb:us-east-1:YOUR_ACCOUNT_ID:table/uHabits-Analytics-prod`
   - Replace `YOUR_ACCOUNT_ID` with your actual account ID

#### Policy JSON:
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
            "Resource": "arn:aws:dynamodb:us-east-1:YOUR_ACCOUNT_ID:table/uHabits-Analytics-prod"
        }
    ]
}
```

**Policy name**: `uhabits-dynamodb-access`

**Click "Create policy"** ✅

---

### 4. **API Gateway** - Create Mobile API
**Location**: AWS Console → Networking → API Gateway → Create API

#### API Type:
- **REST API** (not REST API Private)
- **New API**

#### API Details:
```
API name: uhabits-api-prod
Description: API for uHabits mobile app sync
Endpoint Type: Regional
```

#### Create Resource:
1. **Actions** → **Create Resource**
2. **Resource Name**: sync
3. **Resource Path**: /sync
4. **Enable API Gateway CORS**: ✅ Checked

#### Create Method:
1. **Select /sync resource**
2. **Actions** → **Create Method** → **POST**
3. **Integration type**: Lambda Function
4. **Use Lambda Proxy integration**: ✅ Checked
5. **Lambda Function**: `uhabits-sync-prod`
6. **Use Default Timeout**: ✅ Checked

#### Configure CORS:
1. **Select /sync resource**
2. **Actions** → **Enable CORS**
3. **Access-Control-Allow-Origin**: `*`
4. **Access-Control-Allow-Headers**: 
   ```
   Content-Type,X-Amz-Date,Authorization,X-Api-Key,X-Amz-Security-Token
   ```
5. **Access-Control-Allow-Methods**: `GET,POST,OPTIONS`

#### Deploy API:
1. **Actions** → **Deploy API**
2. **Deployment stage**: [New Stage]
3. **Stage name**: `prod`
4. **Stage description**: `Production stage for uHabits`

**Note the Invoke URL** (you'll need this) ✅

---

### 5. **API Key & Usage Plan** - Secure Mobile Access
**Location**: Still in API Gateway Console

#### Create API Key:
1. **Left menu** → **API Keys** → **Create API Key**
2. **Name**: `uhabits-mobile-app-key`
3. **Description**: `API key for uHabits mobile app access`
4. **Auto Generate**: ✅ Checked

#### Create Usage Plan:
1. **Left menu** → **Usage Plans** → **Create**
2. **Name**: `uhabits-usage-plan`
3. **Description**: `Usage limits for uHabits mobile app`
4. **Enable throttling**: ✅ Checked
   - **Rate**: 10 requests per second
   - **Burst**: 20 requests
5. **Enable quota**: ✅ Checked
   - **Requests per month**: 1000

#### Associate API:
1. **Add API Stage**
2. **API**: `uhabits-api-prod`
3. **Stage**: `prod`

#### Associate API Key:
1. **API Keys tab**
2. **Add API Key**
3. **Select**: `uhabits-mobile-app-key`

#### Get API Key Value:
1. **API Keys** → **uhabits-mobile-app-key**
2. **Click "Show"** next to API key
3. **Copy the key value** (you'll need this)

**Save both the Invoke URL and API Key** ✅

---

## 📝 **Configuration Summary**

After completing all steps, you should have:

### DynamoDB:
- **Table**: `uHabits-Analytics-prod`
- **Status**: Active
- **TTL**: Enabled on `ttl` attribute

### Lambda:
- **Function**: `uhabits-sync-prod`
- **Runtime**: Python 3.12
- **Environment Variables**: TABLE_NAME, ENVIRONMENT

### API Gateway:
- **API**: `uhabits-api-prod`
- **Endpoint**: `https://xxxxxxxxxx.execute-api.us-east-1.amazonaws.com/prod`
- **Resource**: `/sync`
- **Method**: POST

### Security:
- **IAM Role**: Enhanced with DynamoDB permissions
- **API Key**: Generated and protected
- **Usage Plan**: 1000 requests/month limit

## 🔧 **Final Configuration**

Create your `android_config.json` file:

```json
{
    "api_endpoint": "https://YOUR_API_ID.execute-api.us-east-1.amazonaws.com/prod/sync",
    "api_key": "YOUR_API_KEY_VALUE",
    "region": "us-east-1",
    "table_name": "uHabits-Analytics-prod",
    "environment": "prod"
}
```

Replace:
- `YOUR_API_ID` with the API Gateway ID from step 4
- `YOUR_API_KEY_VALUE` with the API key from step 5

## 🧪 **Test Your Setup**

Use this curl command to test:

```bash
curl -X POST \
  -H "Content-Type: application/json" \
  -H "x-api-key: YOUR_API_KEY" \
  -d '{
    "user_id": "user_primary",
    "sync_timestamp": 1695081600000,
    "summary_metrics": {"total_habits": 1},
    "habits_data": [{"id": "test", "name": "Test"}]
  }' \
  "https://YOUR_API_ID.execute-api.us-east-1.amazonaws.com/prod/sync"
```

Expected response: `{"status": "success", ...}`

## 💰 **Cost Monitoring**

All services are configured for AWS Free Tier:
- **DynamoDB**: 25GB free storage
- **Lambda**: 1M free requests/month
- **API Gateway**: 1M free requests/month

**Expected monthly cost**: $0.00 - $0.10

Your serverless analytics platform is now ready! 🚀
