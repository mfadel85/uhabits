#!/bin/bash

# Build Debug Version of uHabits with Enhanced Logging
# This script builds a debug version with detailed sync logging

echo "===== Building Debug Version of uHabits ====="
echo "This will create a debug APK with enhanced logging for sync operations."
echo ""

# Check if we're in the right directory
if [ ! -f "gradlew" ]; then
    echo "❌ Error: gradlew not found. Please run this script from the uHabits root directory."
    exit 1
fi

# Create a backup of the original CloudSyncManager
SYNC_MANAGER_FILE="uhabits-android/src/main/java/org/isoron/uhabits/sync/CloudSyncManager.kt"
if [ -f "$SYNC_MANAGER_FILE" ]; then
    echo "📁 Creating backup of CloudSyncManager.kt..."
    cp "$SYNC_MANAGER_FILE" "$SYNC_MANAGER_FILE.backup"
fi

echo "🔧 Building debug version with enhanced logging..."

# Build debug APK
./gradlew assembleDebug -Pandroid.useAndroidX=true --stacktrace

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ Debug build successful!"
    echo ""
    echo "Debug APK location:"
    find . -name "*-debug.apk" -type f | head -5
    echo ""
    echo "To install on your connected device:"
    echo "1. Enable 'Install unknown apps' on your phone if needed"
    echo "2. Run: adb install -r path/to/debug.apk"
    echo ""
    echo "Or use the install script:"
    echo "   ./install-debug-to-phone.sh"
else
    echo "❌ Build failed. Check the error messages above."
    exit 1
fi
