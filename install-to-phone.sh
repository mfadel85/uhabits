#!/bin/bash

# uHabits Analytics - Direct Phone Install Script
# Builds APK and installs directly to connected Android device

set -e

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}📱 uHabits Analytics - Direct Phone Install${NC}"
echo "=============================================="

# Check if device is connected
echo -e "${YELLOW}📋 Checking device connection...${NC}"
DEVICES=$(adb devices | grep -v "List of devices" | grep "device")

if [ -z "$DEVICES" ]; then
    echo -e "${RED}❌ No Android device connected!${NC}"
    echo ""
    echo -e "${YELLOW}📱 Please connect your phone:${NC}"
    echo "1. Connect USB cable"
    echo "2. Enable USB Debugging in Developer Options"
    echo "3. Select 'File Transfer' mode"
    echo "4. Allow USB debugging when prompted"
    echo ""
    echo "Then run this script again!"
    exit 1
fi

echo -e "${GREEN}✅ Device connected:${NC}"
echo "$DEVICES"
echo ""

# Get device info
DEVICE_MODEL=$(adb shell getprop ro.product.model 2>/dev/null || echo "Unknown")
ANDROID_VERSION=$(adb shell getprop ro.build.version.release 2>/dev/null || echo "Unknown")

echo -e "${YELLOW}📱 Device Info:${NC}"
echo "  Model: $DEVICE_MODEL"
echo "  Android: $ANDROID_VERSION"
echo ""

# Build APK
echo -e "${YELLOW}🏗️  Building fresh APK...${NC}"
export ANDROID_HOME=~/android-sdk
./gradlew assembleDebug

# Check if build was successful
APK_PATH="uhabits-android/build/outputs/apk/debug/uhabits-android-debug.apk"
if [ ! -f "$APK_PATH" ]; then
    echo -e "${RED}❌ Build failed - APK not found${NC}"
    exit 1
fi

APK_SIZE=$(ls -lh "$APK_PATH" | awk '{print $5}')
echo -e "${GREEN}✅ Build successful!${NC}"
echo "  APK: $APK_PATH"
echo "  Size: $APK_SIZE"
echo ""

# Install on device
echo -e "${YELLOW}📲 Installing on device...${NC}"

# Uninstall previous version if exists
adb uninstall org.isoron.uhabits 2>/dev/null || echo "No previous version to uninstall"

# Install new version
if adb install "$APK_PATH"; then
    echo -e "${GREEN}✅ Installation successful!${NC}"
    echo ""
    
    # Launch the app
    echo -e "${YELLOW}🚀 Launching uHabits...${NC}"
    adb shell am start -n org.isoron.uhabits/org.isoron.uhabits.activities.habits.list.ListHabitsActivity
    
    echo ""
    echo -e "${GREEN}🎉 Success! uHabits is now running on your phone!${NC}"
    echo ""
    echo -e "${YELLOW}📊 To test Analytics:${NC}"
    echo "1. Open the app on your phone"
    echo "2. Tap the menu (⋮) button"
    echo "3. Select 'Analytics & Export'"
    echo "4. Tap 'Export All Analytics Data'"
    echo "5. Check Downloads folder for exported files"
    echo ""
    echo -e "${BLUE}Happy testing! 📱✨${NC}"
    
else
    echo -e "${RED}❌ Installation failed!${NC}"
    echo ""
    echo -e "${YELLOW}💡 Possible solutions:${NC}"
    echo "1. Enable 'Install via USB' in Developer Options"
    echo "2. Allow 'Install unknown apps' for ADB"
    echo "3. Check if USB debugging is still enabled"
    exit 1
fi
