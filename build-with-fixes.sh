#!/bin/bash

echo "===== Building uHabits with Cloud Sync Fix ====="

# Navigate to the root directory
cd "$(dirname "$0")"

# Display current status
echo "Current directory: $(pwd)"
echo "Building app with cloud sync fixes..."

# Clean build
./gradlew clean

# Build the app
./gradlew assembleDebug

# Check if build was successful
if [ $? -eq 0 ]; then
  echo "✅ Build successful!"
  echo "APK location: ./uhabits-android/build/outputs/apk/debug/uhabits-android-debug.apk"
  
  echo ""
  echo "To install on your device, connect via USB and run:"
  echo "./install-to-phone.sh"
  echo ""
  echo "After installing:"
  echo "1. Open the app"
  echo "2. Tap the menu (three dots) in the top right corner"
  echo "3. Select 'Cloud Sync'"
  echo "4. In the dialog, tap 'Fix Sync Issues'"
  echo "5. Cloud sync should now be enabled and functional"
else
  echo "❌ Build failed. Check the error messages above."
fi
