#!/bin/bash

# Install Debug APK to Connected Phone
echo "===== Installing Debug uHabits to Phone ====="

# Check if device is connected
DEVICES=$(adb devices | grep -E "\tdevice$" | wc -l)

if [ "$DEVICES" -eq 0 ]; then
    echo "❌ No Android devices found. Please connect your phone and enable USB debugging."
    exit 1
fi

echo "✓ Found $DEVICES Android device(s) connected"

# Find the debug APK
DEBUG_APK=$(find . -name "*-debug.apk" -type f | head -1)

if [ -z "$DEBUG_APK" ]; then
    echo "❌ Debug APK not found. Please run ./build_debug_version.sh first."
    exit 1
fi

echo "📱 Installing debug APK: $DEBUG_APK"

# Install the APK
adb install -r "$DEBUG_APK"

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ Debug version installed successfully!"
    echo ""
    echo "📱 The debug version should now be available on your phone."
    echo "📱 It may appear as a separate app or replace the existing one."
    echo ""
    echo "Next steps:"
    echo "1. Open the debug version of uHabits on your phone"
    echo "2. Run: ./debug_mobile_sync.sh"
    echo "3. Follow the instructions to capture sync logs"
else
    echo "❌ Installation failed. Make sure USB debugging is enabled and you've accepted any prompts on your phone."
fi
