#!/bin/bash

# uHabits Enhanced Build Script for Ubuntu
# This script builds the enhanced uHabits app with analytics features

echo "🏗️ Building Enhanced uHabits with Advanced Analytics..."

# Check if we're in the right directory
if [ ! -f "gradlew" ]; then
    echo "❌ Error: gradlew not found. Make sure you're in the uhabits/uhabits directory"
    exit 1
fi

# Check Java version
JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 17 ]; then
    echo "❌ Error: Java 17 or higher required. Current version: $JAVA_VERSION"
    echo "Run: sudo apt install openjdk-17-jdk"
    exit 1
fi

# Check Android SDK
if [ -z "$ANDROID_HOME" ]; then
    echo "❌ Error: ANDROID_HOME not set. Run the setup-ubuntu.sh script first."
    exit 1
fi

echo "✅ Java Version: $(java -version 2>&1 | head -n 1)"
echo "✅ Android SDK: $ANDROID_HOME"

# Clean previous builds
echo "🧹 Cleaning previous builds..."
./gradlew clean

# Build debug APK (faster, for testing)
echo "🔨 Building debug APK..."
./gradlew :uhabits-android:assembleDebug

if [ $? -eq 0 ]; then
    echo "✅ Debug build successful!"
    DEBUG_APK="uhabits-android/build/outputs/apk/debug/uhabits-android-debug.apk"
    if [ -f "$DEBUG_APK" ]; then
        echo "📱 Debug APK location: $DEBUG_APK"
        echo "📊 APK size: $(du -h "$DEBUG_APK" | cut -f1)"
    fi
else
    echo "❌ Debug build failed!"
    exit 1
fi

# Ask if user wants to build release APK
read -p "🚀 Build release APK? (y/N): " build_release

if [[ $build_release =~ ^[Yy]$ ]]; then
    echo "🔨 Building release APK..."
    ./gradlew :uhabits-android:assembleRelease
    
    if [ $? -eq 0 ]; then
        echo "✅ Release build successful!"
        RELEASE_APK="uhabits-android/build/outputs/apk/release/uhabits-android-release.apk"
        if [ -f "$RELEASE_APK" ]; then
            echo "📱 Release APK location: $RELEASE_APK"
            echo "📊 APK size: $(du -h "$RELEASE_APK" | cut -f1)"
        fi
    else
        echo "❌ Release build failed!"
    fi
fi

# Check if phone is connected
echo ""
echo "📱 Checking for connected Android devices..."
adb devices -l

DEVICE_COUNT=$(adb devices | grep -c "device$")
if [ $DEVICE_COUNT -gt 0 ]; then
    echo "✅ Found $DEVICE_COUNT Android device(s)"
    
    # Ask if user wants to install
    read -p "📲 Install debug APK to connected device? (y/N): " install_apk
    
    if [[ $install_apk =~ ^[Yy]$ ]]; then
        echo "📲 Installing APK..."
        adb install -r "$DEBUG_APK"
        
        if [ $? -eq 0 ]; then
            echo "✅ Installation successful!"
            echo "🎉 Enhanced uHabits with Analytics is now installed on your Samsung phone!"
            echo ""
            echo "🚀 Launch the app and check the new Analytics menu item!"
        else
            echo "❌ Installation failed. Check phone permissions and USB debugging."
        fi
    fi
else
    echo "⚠️ No Android devices found."
    echo "💡 To install manually:"
    echo "   1. Copy $DEBUG_APK to your phone"
    echo "   2. Open file manager on phone"
    echo "   3. Tap the APK file to install"
fi

echo ""
echo "📋 Build Summary:"
echo "=================="
echo "✅ Enhanced uHabits built successfully"
echo "🎯 Features included:"
echo "   • 100-point scoring system"
echo "   • Advanced analytics engine"
echo "   • PowerBI/Looker Studio export"
echo "   • Comprehensive insights UI"
echo "   • Habit correlation analysis"
echo ""
echo "📱 APK locations:"
if [ -f "$DEBUG_APK" ]; then
    echo "   Debug: $DEBUG_APK"
fi
if [ -f "$RELEASE_APK" ]; then
    echo "   Release: $RELEASE_APK"
fi

echo ""
echo "🎉 Ready to deploy to Samsung phone!"
