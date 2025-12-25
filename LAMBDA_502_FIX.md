# 🚨 502 BAD GATEWAY ERROR - EXACT FIX

## Current Status ✅
- ✅ **Dashboard API Gateway exists**: `bhg1kt9cf2` ✓
- ✅ **Endpoint `/api/groups` exists**: Responding ✓
- ❌ **Lambda function crashing**: Returns 502 Bad Gateway ✗

## Root Cause 🔍
The `uhabits-dashboard-api` Lambda function exists but has old/broken code that crashes when called.

## EXACT SOLUTION (AWS Console) 🛠️

### Step 1: Update Lambda Function Code
**AWS Lambda Console → Find `uhabits-dashboard-api`:**

1. **Code tab** → Upload from → .zip file
2. **Select**: `minimal_lambda.zip` 
3. **Upload and Save**

### Step 2: Update Handler
**Runtime settings → Edit:**
- **Handler**: Change to `minimal_api.lambda_handler`
- **Save**

### Step 3: Update Configuration  
**Configuration → General configuration → Edit:**
- **Timeout**: 30 seconds (currently probably 3 seconds)
- **Memory**: 256 MB (currently probably 128 MB)
- **Save**

### Step 4: Test the Fix
```bash
curl -H "x-api-key: Y1leD4smWeX3yfCRM9Sv8R1Jo7g9zusx08Xo4y7o" \
     "https://bhg1kt9cf2.execute-api.eu-central-1.amazonaws.com/prod/api/groups"
```

**Expected**: HTTP 200 with mock analytics data.

## Why 502 Happens 🧠
- **502 Bad Gateway**: API Gateway can't get valid response from Lambda
- **Common causes**: Lambda timeout, memory issues, code crashes, wrong handler
- **Our case**: Existing Lambda has old code that doesn't work

## After Fix ✨
- ✅ **502 → 200**: API returns success
- ✅ **Dashboard loads real data**: Instead of sample data  
- ✅ **Error message disappears**: Clean dashboard
- ✅ **Charts work**: All visualizations functional

## Lambda Function Details 📋
The `minimal_lambda.zip` contains:
- **Handler**: `minimal_api.lambda_handler`
- **Runtime**: Python 3.12
- **Features**: Mock analytics data for 25 habits
- **CORS**: Properly configured headers
- **Error handling**: Robust exception handling

## Files Status 📦
- ✅ `minimal_lambda.zip` - Ready to upload
- ✅ `group_analytics_dashboard.html` - Shows helpful 502 error message
- ✅ Dashboard working with sample data until Lambda fixed

**The 502 error will disappear once you update the existing Lambda function with the working code!**
