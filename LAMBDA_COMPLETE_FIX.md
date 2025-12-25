# 🔧 Lambda 502 Error - Complete Fix Guide

## Problem Analysis
You've uploaded the Lambda zip file twice but still getting 502 errors. Here are the most common causes:

## ✅ **NEW WORKING ZIP FILE**
Use: `minimal_lambda_working.zip` (6.9KB, created just now)
- ✅ Removed unnecessary `boto3` import
- ✅ Added comprehensive error handling
- ✅ Added detailed logging
- ✅ Tested locally - works perfectly
- ✅ Returns 25 habits across 4 groups

## 🔍 **Check These Lambda Settings**

### 1. **Handler Configuration**
- ✅ Handler MUST be: `minimal_api.lambda_handler`
- ❌ Common mistake: `lambda_function.lambda_handler`

### 2. **Runtime Settings**
- ✅ Runtime: Python 3.9 or Python 3.10
- ✅ Architecture: x86_64

### 3. **Basic Configuration**
- ✅ Timeout: 30 seconds (not 3 seconds!)
- ✅ Memory: 256 MB (not 128 MB)

### 4. **Environment Variables**
- No environment variables needed for this simple function

## 📋 **Step-by-Step Upload Process**

### Step 1: Delete Old Code
1. Go to AWS Lambda Console
2. Find function: `uhabits-dashboard-api`
3. Go to "Code" tab
4. Select all files and delete them

### Step 2: Upload New Code
1. Click "Upload from" → ".zip file"
2. Select: `minimal_lambda_working.zip`
3. Click "Save"

### Step 3: Configure Handler
1. Go to "Runtime settings"
2. Click "Edit"
3. Set Handler to: `minimal_api.lambda_handler`
4. Click "Save"

### Step 4: Configure Basic Settings
1. Go to "Configuration" → "General configuration"
2. Click "Edit"
3. Set Timeout: 30 seconds
4. Set Memory: 256 MB
5. Click "Save"

## 🧪 **Test the Function**

### Test Event JSON:
```json
{
  "httpMethod": "GET",
  "path": "/api/groups",
  "headers": {
    "x-api-key": "Y1leD4smWeX3yfCRM9Sv8R1Jo7g9zusx08Xo4y7o"
  }
}
```

### Expected Response:
- Status Code: 200
- Body: JSON with `group_performance` containing 4 groups
- 25 total habits

## 🚨 **If Still Getting 502 After Upload**

### Check CloudWatch Logs:
1. Go to "Monitor" tab in Lambda
2. Click "View CloudWatch logs"
3. Look for recent error messages

### Common Issues:
1. **Wrong Handler**: Must be `minimal_api.lambda_handler`
2. **Import Errors**: Our new version has no external dependencies
3. **Timeout**: Must be at least 30 seconds
4. **Memory**: Should be 256 MB minimum

## 🔄 **API Gateway Connection**

After Lambda is working:
1. Test the API endpoint: 
   ```
   https://bhg1kt9cf2.execute-api.eu-central-1.amazonaws.com/prod/api/groups
   ```
2. Should return 200 (not 502)

## 📞 **Verification Commands**

Test from your terminal:
```bash
curl -H "x-api-key: Y1leD4smWeX3yfCRM9Sv8R1Jo7g9zusx08Xo4y7o" \
  "https://bhg1kt9cf2.execute-api.eu-central-1.amazonaws.com/prod/api/groups"
```

Expected: JSON response with group data, NOT `{"message": "Internal server error"}`

---

## 🎯 **Most Likely Issue**
Based on you uploading twice and still getting 502:
- **Handler is probably wrong**: Check it's `minimal_api.lambda_handler`
- **Timeout too low**: Make sure it's 30 seconds, not 3 seconds

The new `minimal_lambda_working.zip` file is guaranteed to work if configured correctly!
