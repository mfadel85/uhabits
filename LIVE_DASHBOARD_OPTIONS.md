# Looker Studio with Live AWS DynamoDB Connection

## 🎯 Option: Direct AWS Integration (Advanced)

### Method 1: AWS Athena + S3 Export (Recommended)

#### Step 1: Set up DynamoDB Export to S3
```bash
# Create S3 bucket for exports
aws s3 mb s3://uhabits-analytics-exports --region eu-central-1

# Enable DynamoDB continuous export
aws dynamodb enable-continuous-backups \
    --table-name uHabits-Analytics-prod \
    --region eu-central-1

# Export to S3 (run daily via Lambda)
aws dynamodb export-table-to-point-in-time \
    --table-arn "arn:aws:dynamodb:eu-central-1:$(aws sts get-caller-identity --query Account --output text):table/uHabits-Analytics-prod" \
    --s3-bucket uhabits-analytics-exports \
    --s3-prefix "exports/$(date +%Y/%m/%d)" \
    --export-format DYNAMODB_JSON \
    --region eu-central-1
```

#### Step 2: Create Athena Database
```sql
-- Create external table in Athena
CREATE EXTERNAL TABLE uhabits_analytics (
  PK string,
  SK string,
  habit_name string,
  priority string,
  success_rate double,
  current_streak bigint,
  best_streak bigint,
  total_completions bigint,
  completed boolean,
  completion_rate double,
  total_days bigint,
  completed_days bigint
)
STORED AS INPUTFORMAT 'org.apache.hadoop.mapred.TextInputFormat'
OUTPUTFORMAT 'org.apache.hadoop.hive.ql.io.HiveIgnoreKeyTextOutputFormat'
LOCATION 's3://uhabits-analytics-exports/exports/'
TBLPROPERTIES ('has_encrypted_data'='false');
```

#### Step 3: Connect Looker Studio to Athena
1. Use **BigQuery Data Transfer Service**
2. Set up **Athena connector** (requires Looker Studio Pro)
3. Or export Athena results to Google Sheets automatically

### Method 2: Automated Google Sheets Sync

#### Create Auto-Sync Lambda Function:
```python
import boto3
import json
from google.oauth2.service_account import Credentials
import gspread

def lambda_handler(event, context):
    """Auto-sync DynamoDB to Google Sheets every hour"""
    
    # DynamoDB scan
    dynamodb = boto3.resource('dynamodb', region_name='eu-central-1')
    table = dynamodb.Table('uHabits-Analytics-prod')
    
    response = table.scan()
    items = response['Items']
    
    # Google Sheets setup
    scope = ['https://spreadsheets.google.com/feeds',
             'https://www.googleapis.com/auth/drive']
    
    # Use service account JSON (store in Lambda environment)
    creds = Credentials.from_service_account_info(
        json.loads(os.environ['GOOGLE_SHEETS_CREDENTIALS']), 
        scopes=scope
    )
    
    client = gspread.authorize(creds)
    sheet = client.open('uHabits Live Analytics').sheet1
    
    # Convert DynamoDB items to rows
    headers = ['PK', 'SK', 'habit_name', 'priority', 'success_rate', 'current_streak']
    rows = [headers]
    
    for item in items:
        row = [
            item.get('PK', ''),
            item.get('SK', ''),
            item.get('habit_name', ''),
            item.get('priority', ''),
            float(item.get('success_rate', 0)),
            int(item.get('current_streak', 0))
        ]
        rows.append(row)
    
    # Clear and update sheet
    sheet.clear()
    sheet.update('A1', rows)
    
    return {'statusCode': 200, 'body': 'Sync completed'}
```

#### Set up CloudWatch Event:
```bash
# Schedule Lambda to run every hour
aws events put-rule \
    --name "uhabits-sheets-sync" \
    --schedule-expression "rate(1 hour)" \
    --region eu-central-1

aws events put-targets \
    --rule "uhabits-sheets-sync" \
    --targets "Id"="1","Arn"="arn:aws:lambda:eu-central-1:ACCOUNT:function:sheets-sync"
```

## 🚀 Benefits of Live Connections:

### AWS QuickSight:
- ✅ **Real-time data** (refresh every hour)
- ✅ **No manual exports** needed
- ✅ **Native AWS integration**
- ✅ **Mobile app** available
- ✅ **Advanced analytics** (ML insights)

### Enhanced Web Dashboard:
- ✅ **Instant updates** via API calls
- ✅ **Custom visualizations**
- ✅ **Free to host**
- ✅ **Full control** over design

### Auto-Sync Google Sheets:
- ✅ **Familiar interface** (Sheets)
- ✅ **Automatic updates** (hourly)
- ✅ **Easy sharing**
- ✅ **Looker Studio integration**

## 📊 Recommended Architecture:

```
DynamoDB → Lambda API → Web Dashboard (Real-time)
    ↓
CloudWatch Event → Auto-Sync → Google Sheets → Looker Studio
    ↓
DynamoDB Export → S3 → Athena → QuickSight
```

This gives you **3 live dashboards**:
1. **Web Dashboard** - Real-time, custom
2. **Looker Studio** - Professional, shareable  
3. **QuickSight** - Enterprise, ML-powered

## 🎯 Quick Start:
1. **Immediate**: Use the enhanced web dashboard with API
2. **This week**: Set up QuickSight connection
3. **Next week**: Add auto-sync to Google Sheets

All options eliminate manual exports and provide live data!
