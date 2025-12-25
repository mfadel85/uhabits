# Cloud Sync Fix Guide

This guide explains how to fix cloud sync issues in the uHabits app.

## Common Sync Issues

If cloud sync is disabled in your app and there's no option to enable it, this is likely due to one of these issues:

1. The cloud configuration is missing or invalid
2. The AWS Lambda function is not properly set up
3. The preferences are not allowing sync to be enabled

## Quick Fix Instructions

### Method 1: Using the in-app fix

The latest app version includes a "Fix Sync Issues" button that will automatically repair sync functionality:

1. Open the app
2. Tap the three-dot menu in the top-right corner
3. Select "Cloud Sync"
4. In the dialog that appears, tap "Fix Sync Issues"
5. The app will attempt to repair the sync settings and show a confirmation message
6. If successful, you can now enable and use cloud sync

### Method 2: Manual Fix

If the in-app fix doesn't work, you can try the following manual steps:

1. Ensure your AWS Lambda function is properly set up:
   ```bash
   cd /home/muosman/uHabits/uhabits/aws-lambda
   ./check_lambda_function.sh
   ```

2. Verify your AWS credentials are properly configured:
   ```bash
   aws configure
   ```

3. Run the AWS Lambda status check:
   ```bash
   cd /home/muosman/uHabits/uhabits
   ./verify_lambda_update.sh
   ```

4. Rebuild and reinstall the app after making these changes.

## Technical Details

The sync functionality requires:

1. A properly configured `cloud_config.json` file in the assets folder
2. The `config` object to be successfully loaded in `CloudSyncManager`
3. The `preferences.isCloudSyncEnabled` flag to be set to `true`

The fix we've implemented addresses these issues by:
- Adding a special force-enable method to ensure sync can be enabled
- Adding a "Fix Sync Issues" button in the Cloud Sync settings dialog
- Creating a CloudSyncFix helper class that diagnoses and repairs sync configuration

## Verifying the Fix

After applying the fix, you should be able to:
1. Enable cloud sync in the settings
2. Successfully sync data to AWS
3. See your data in the analytics dashboard

If you continue to have issues, please check the AWS Lambda configuration and make sure your AWS services are properly set up according to the guides in the repository.
