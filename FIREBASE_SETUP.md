# 🔥 Firebase App Distribution Setup Guide

This guide helps you set up Firebase App Distribution for automatic testing distribution.

## 📋 Prerequisites

- [ ] Google/Firebase account
- [ ] GitHub repository with admin access
- [ ] APK builds working locally

## 🚀 Step 1: Create Firebase Project

1. **Go to Firebase Console**: https://console.firebase.google.com/
2. **Create new project**:
   - Project name: `uHabits-Analytics`
   - Enable Google Analytics (optional)
   - Select your country/region

3. **Add Android app**:
   - Package name: `org.isoron.uhabits` (from AndroidManifest.xml)
   - App nickname: `uHabits Analytics`
   - Debug signing certificate (optional for now)

## 🔧 Step 2: Enable App Distribution

1. **In Firebase Console**:
   - Go to your project
   - Left sidebar → "App Distribution"
   - Click "Get started"

2. **Configure distribution**:
   - Select your Android app
   - Enable App Distribution for this app

## 👥 Step 3: Set Up Tester Groups

1. **Create tester group**:
   - In App Distribution → "Testers & Groups"
   - Click "Add group"
   - Group name: `Beta Testers`
   - Add email addresses of testers

2. **Individual testers**:
   - Can also add individual email addresses
   - Testers will receive email invitations

## 🔑 Step 4: Generate Service Account

1. **Go to Project Settings**:
   - Click gear icon → "Project settings"
   - Go to "Service accounts" tab
   - Click "Generate new private key"
   - **Save the JSON file securely**

2. **Enable APIs**:
   - Go to Google Cloud Console for your project
   - Enable "Firebase App Distribution API"
   - Enable "Cloud Resource Manager API"

## 🔐 Step 5: Add GitHub Secrets

In your GitHub repository settings:

1. **Go to Settings → Secrets → Actions**

2. **Add these secrets**:
   
   **FIREBASE_APP_ID**:
   ```
   1:123456789:android:abcdefghijk123456
   ```
   *(Find this in Firebase Project Settings → General → Your apps)*

   **FIREBASE_SERVICE_ACCOUNT**:
   ```json
   {
     "type": "service_account",
     "project_id": "your-project-id",
     "private_key_id": "...",
     "private_key": "-----BEGIN PRIVATE KEY-----\n...\n-----END PRIVATE KEY-----\n",
     "client_email": "firebase-adminsdk-xxxxx@your-project.iam.gserviceaccount.com",
     "client_id": "...",
     "auth_uri": "https://accounts.google.com/o/oauth2/auth",
     "token_uri": "https://oauth2.googleapis.com/token",
     "auth_provider_x509_cert_url": "https://www.googleapis.com/oauth2/v1/certs",
     "client_x509_cert_url": "..."
   }
   ```
   *(Copy the entire contents of the service account JSON file)*

## 🧪 Step 6: Test the Setup

1. **Make a code change** (or just commit something)
2. **Push to main branch**:
   ```bash
   git add .
   git commit -m "test: trigger firebase distribution"
   git push origin main
   ```

3. **Check GitHub Actions**:
   - Go to repository → Actions tab
   - Watch the "Firebase App Distribution" workflow
   - Should complete successfully

4. **Check Firebase Console**:
   - Go to App Distribution → Releases
   - Should see your new release
   - Testers should receive email notifications

## 📱 Step 7: Test Installation

1. **Testers receive email** with install link
2. **Or use the Firebase App Distribution app**:
   - Install from Play Store
   - Login with Google account
   - See available test apps

3. **Install and test** the app

## 🔧 Troubleshooting

### Common Issues:

**❌ "App not found" error**:
- Check FIREBASE_APP_ID is correct
- Ensure app is added to Firebase project

**❌ "Permission denied" error**:
- Check service account has correct permissions
- Ensure Firebase App Distribution API is enabled

**❌ "No testers" warning**:
- Add testers to the Firebase console
- Or use tester groups

**❌ APK upload fails**:
- Check APK is signed properly
- Ensure package name matches Firebase app

### Debugging Steps:

1. **Check GitHub Actions logs**:
   - Look for detailed error messages
   - Verify all secrets are set

2. **Verify Firebase setup**:
   - App Distribution is enabled
   - Service account has permissions
   - APIs are enabled

3. **Test locally**:
   ```bash
   # Install Firebase CLI
   npm install -g firebase-tools
   
   # Login
   firebase login
   
   # Test upload (from project root)
   firebase appdistribution:distribute \
     uhabits-android/build/outputs/apk/debug/uhabits-android-debug.apk \
     --app YOUR_FIREBASE_APP_ID \
     --release-notes "Test release" \
     --testers "your-email@example.com"
   ```

## 🎉 Success Indicators

✅ **GitHub Actions workflow completes successfully**  
✅ **Release appears in Firebase App Distribution console**  
✅ **Testers receive email notifications**  
✅ **APK can be downloaded and installed from email/app**  
✅ **App launches and analytics features work**

## 📧 Support

If you need help:
- Check Firebase documentation
- Review GitHub Actions logs
- Create an issue in the repository

---

*Happy testing! 🚀*
