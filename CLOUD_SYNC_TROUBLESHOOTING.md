# Cloud Sync Troubleshooting Guide

This guide will help you troubleshoot common cloud sync issues in the uHabits application.

## Common Issues and Solutions

### 1. Sync is Disabled / No Option to Enable

If you don't see an option to enable cloud sync:

- Ensure you have the latest app version installed
- Try the following quick fix:
  - Go to Settings -> Cloud Sync
  - Click "Fix Sync Issues"
  - Restart the application

### 2. Network Connection Failed

If you see "Network connection failed" when trying to sync:

#### Check your Internet Connection
- Ensure your device is connected to WiFi or mobile data
- Try accessing other websites to confirm internet connectivity
- If using WiFi, try switching to mobile data or vice versa

#### Check API Connectivity
- Use the built-in diagnostics tool:
  1. Go to Cloud Sync Settings
  2. Tap "Diagnostics"
  3. Tap "Test Connection"
  4. Review the results for specific issues

#### Possible Issues and Solutions:
- **DNS Issues**: Some networks block AWS domains. Try using a different network.
- **API Key Invalid**: The API key in your app's configuration may be incorrect or expired. Use the "Fix Sync Issues" button.
- **Firewall Blocking**: Corporate or school networks often block cloud services. Try using a different network.
- **VPN Interference**: If you're using a VPN, try disabling it temporarily.

### 3. Authentication Failed

If you see "Authentication failed" or "API key may be invalid":

- Use the "Fix Sync Issues" button to reset your API configuration
- Try reinstalling the application
- Contact support if the issue persists

### 4. Timeout Errors

If sync takes too long and fails:

- Try again when you have a stronger internet connection
- Reduce the number of habits if you have many (100+)
- Check if your device has background data restrictions for the app

### 5. Server Errors (500+)

If you see server errors (HTTP 500, 502, etc.):

- This indicates an issue with the AWS Lambda function
- Try again later as the service might be temporarily unavailable
- Report the issue if it persists for more than 24 hours

## Using the Diagnostics Tool

The app includes a built-in diagnostics tool to help troubleshoot sync issues:

1. Open the app and access Cloud Sync settings
2. Tap "Diagnostics" to open the diagnostic tool
3. Use the following features:
   - **Run Diagnostics**: Performs a comprehensive check of your sync configuration and network connectivity
   - **Test Connection**: Tests basic connectivity to the API endpoint
   - **Fix Sync Issues**: Attempts to automatically fix common configuration problems
   - **Copy Report**: Copies diagnostic information to share when reporting issues

## Manual Testing

For advanced users, a test script is included to check API connectivity from your computer:

```bash
# Navigate to the app directory
cd /path/to/uHabits/uhabits

# Make the script executable if needed
chmod +x test_cloud_sync.sh

# Run the test script
./test_cloud_sync.sh
```

## Contact Support

If you continue experiencing issues after trying these solutions:

1. Run a full diagnostic
2. Copy the diagnostic report
3. Contact support with the following information:
   - Your diagnostic report
   - Steps to reproduce the issue
   - Device model and Android version
   - App version

## AWS Configuration

If you're using a custom AWS deployment:

1. Verify your API Gateway endpoint is correctly configured
2. Check that your Lambda function is properly deployed
3. Ensure your API key is valid and not expired
4. Verify CORS settings if applicable

Remember that cloud sync features rely on AWS services being properly configured and accessible from your device's network.
