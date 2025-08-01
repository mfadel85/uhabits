#!/bin/bash

# Enhanced uHabits Deployment Script
# Deploy to Samsung Android device via ADB or manual installation

echo "📱 Enhanced uHabits Deployment Script"
echo "====================================="

# Find APK files
DEBUG_APK="uhabits-android/build/outputs/apk/debug/uhabits-android-debug.apk"
RELEASE_APK="uhabits-android/build/outputs/apk/release/uhabits-android-release.apk"

# Check for APK files
if [ ! -f "$DEBUG_APK" ] && [ ! -f "$RELEASE_APK" ]; then
    echo "❌ No APK files found. Run ./build-ubuntu.sh first."
    exit 1
fi

# Select APK to deploy
APK_TO_DEPLOY=""
if [ -f "$RELEASE_APK" ]; then
    echo "🎯 Found release APK: $RELEASE_APK"
    APK_TO_DEPLOY="$RELEASE_APK"
elif [ -f "$DEBUG_APK" ]; then
    echo "🔧 Found debug APK: $DEBUG_APK"
    APK_TO_DEPLOY="$DEBUG_APK"
fi

echo "📊 APK size: $(du -h "$APK_TO_DEPLOY" | cut -f1)"
echo ""

# Method 1: ADB Installation (Recommended)
echo "Method 1: ADB Installation (USB/WiFi)"
echo "======================================"

# Check ADB availability
if ! command -v adb &> /dev/null; then
    echo "❌ ADB not found. Install Android SDK platform-tools."
    echo "💡 Run the setup-ubuntu.sh script to install required tools."
else
    echo "✅ ADB found: $(which adb)"
    
    # List connected devices
    echo "📱 Scanning for Android devices..."
    adb devices -l
    
    DEVICE_COUNT=$(adb devices | grep -c "device$")
    
    if [ $DEVICE_COUNT -gt 0 ]; then
        echo "✅ Found $DEVICE_COUNT Android device(s)"
        
        # Get device info
        MANUFACTURER=$(adb shell getprop ro.product.manufacturer 2>/dev/null | tr -d '\r')
        MODEL=$(adb shell getprop ro.product.model 2>/dev/null | tr -d '\r')
        ANDROID_VERSION=$(adb shell getprop ro.build.version.release 2>/dev/null | tr -d '\r')
        
        echo "📱 Device: $MANUFACTURER $MODEL (Android $ANDROID_VERSION)"
        
        # Check if it's Samsung
        if [[ "$MANUFACTURER" == *"samsung"* ]]; then
            echo "✅ Samsung device detected!"
            ONEUI_VERSION=$(adb shell getprop ro.build.version.oneui 2>/dev/null | tr -d '\r')
            if [ ! -z "$ONEUI_VERSION" ]; then
                echo "🎨 One UI version: $ONEUI_VERSION"
            fi
        fi
        
        echo ""
        read -p "📲 Install Enhanced uHabits via ADB? (Y/n): " install_adb
        
        if [[ ! $install_adb =~ ^[Nn]$ ]]; then
            echo "📲 Installing APK via ADB..."
            
            # Uninstall previous version if exists
            echo "🗑️ Removing previous installation..."
            adb uninstall org.isoron.uhabits 2>/dev/null
            
            # Install new version
            echo "⬇️ Installing Enhanced uHabits..."
            adb install -r "$APK_TO_DEPLOY"
            
            if [ $? -eq 0 ]; then
                echo "✅ Installation successful via ADB!"
                echo ""
                echo "🚀 Starting Enhanced uHabits..."
                adb shell am start -n org.isoron.uhabits/.activities.habits.list.ListHabitsActivity
                
                echo ""
                echo "🎉 Enhanced uHabits deployed successfully!"
                echo "📈 New features available:"
                echo "   • Advanced Analytics (100-point scoring)"
                echo "   • PowerBI/Looker Studio export"
                echo "   • Habit correlation analysis"
                echo "   • Weekly/Monthly performance tracking"
                echo ""
                echo "💡 Look for the 'Advanced Analytics' menu item in the app!"
                exit 0
            else
                echo "❌ ADB installation failed."
                echo "💡 Try manual installation method below."
            fi
        fi
    else
        echo "⚠️ No Android devices found via ADB."
        echo ""
        echo "🔧 Troubleshooting:"
        echo "   1. Enable USB debugging on Samsung phone"
        echo "   2. Connect USB cable"
        echo "   3. Accept RSA fingerprint on phone"
        echo "   4. Try different USB port/cable"
        echo ""
        echo "📱 For WiFi debugging (Android 11+):"
        echo "   1. Enable 'Wireless debugging' in Developer options"
        echo "   2. Connect both devices to same WiFi"
        echo "   3. Use 'adb pair' command"
    fi
fi

echo ""
echo "Method 2: Manual Installation"
echo "============================="

# Create a deployment package
DEPLOY_DIR="uhabits-deploy-$(date +%Y%m%d-%H%M%S)"
mkdir -p "$DEPLOY_DIR"

# Copy APK
cp "$APK_TO_DEPLOY" "$DEPLOY_DIR/enhanced-uhabits.apk"

# Create installation instructions
cat > "$DEPLOY_DIR/INSTALL_INSTRUCTIONS.txt" << 'EOF'
📱 Enhanced uHabits - Manual Installation Guide
===============================================

🚀 Your Enhanced uHabits APK is ready for installation!

📋 Installation Steps:
1. Copy "enhanced-uhabits.apk" to your Samsung phone
   - Use USB cable, cloud storage, or email
   - Save to Downloads folder for easy access

2. On your Samsung phone:
   - Open "My Files" or any file manager
   - Navigate to Downloads folder
   - Tap "enhanced-uhabits.apk"

3. If prompted about "Unknown sources":
   - Tap "Settings" → Enable "Allow from this source"
   - Go back and tap "Install"

4. After installation:
   - Open "Loop Habit Tracker" 
   - Look for "Advanced Analytics" in the menu!

🎯 New Features in Enhanced Version:
✅ 100-point scoring system (instead of 0-1)
✅ Advanced analytics dashboard
✅ Weekly/Monthly performance tracking  
✅ Habit correlation analysis
✅ PowerBI/Looker Studio data export
✅ Comprehensive insights and trends

💡 Troubleshooting:
- If "App not installed" error: Enable "Install unknown apps"
- If "Parse error": Re-download the APK file
- For Samsung Knox warnings: Tap "Install anyway"

🔧 Samsung-Specific Notes:
- Some Samsung devices may show Knox security warnings
- This is normal for sideloaded apps - tap "Install anyway"
- The app uses same permissions as original uHabits

📊 Analytics Features:
After installation, tap the menu button in uHabits and select
"Advanced Analytics" to access:
- 100-point habit scores
- Performance trends
- Correlation analysis
- Export data for business intelligence tools

🎉 Enjoy your Enhanced uHabits with Advanced Analytics!
EOF

# Create export guide for BI tools
cat > "$DEPLOY_DIR/BI_EXPORT_GUIDE.txt" << 'EOF'
📊 Business Intelligence Export Guide
====================================

🎯 Your Enhanced uHabits now supports exporting data to:
• Microsoft PowerBI
• Google Looker Studio (formerly Data Studio)
• Excel/CSV for custom analysis

📤 How to Export Data:
1. Open Enhanced uHabits
2. Tap Menu → "Advanced Analytics"
3. Tap Menu → "Export Data" 
4. Choose export format:
   - PowerBI Dataset (CSV)
   - Looker Studio (JSON)
   - Raw Analytics (CSV)

📈 PowerBI Integration:
1. Export "PowerBI Dataset" from the app
2. In PowerBI Desktop:
   - Get Data → Text/CSV
   - Import daily_scores.csv
   - Import habit_metadata.csv
   - Create relationships between tables
3. Build dashboards with 100-point scoring system

📊 Looker Studio Integration:
1. Export "Looker Studio" format from app
2. In Looker Studio:
   - Create new report
   - Add data source → File Upload
   - Upload the exported JSON file
   - Use ScoreCategory for color coding
   - Create time-series visualizations

🎨 Suggested Visualizations:
• Line charts: Daily scores over time
• Bar charts: Average scores by habit
• Heat maps: Weekly performance patterns
• Correlation matrices: Habit relationships
• Trend indicators: Performance direction

💡 Data Dictionary:
- DailyScore: 0-100 performance score
- WeeklyScore: 7-day rolling average
- MonthlyScore: 30-day performance
- TrendDirection: UP/DOWN/STABLE
- ConsistencyScore: Regularity metric
- StreakBonus: Points for consecutive days

🔄 Export Frequency:
- Daily: For real-time dashboards
- Weekly: For performance reviews
- Monthly: For long-term analysis

📱 The exported data maintains privacy - only your habit performance 
data is included, not personal information.
EOF

echo "📦 Deployment package created: $DEPLOY_DIR/"
echo ""
echo "📋 Package contents:"
echo "   • enhanced-uhabits.apk (ready to install)"
echo "   • INSTALL_INSTRUCTIONS.txt (step-by-step guide)"
echo "   • BI_EXPORT_GUIDE.txt (PowerBI/Looker Studio setup)"
echo ""
echo "💾 Transfer options:"
echo "   1. USB: Copy entire '$DEPLOY_DIR' folder to phone"
echo "   2. Cloud: Upload to Google Drive/OneDrive/Dropbox"
echo "   3. Email: Send APK as attachment"
echo "   4. ADB: adb push '$DEPLOY_DIR' /sdcard/Download/"
echo ""
echo "🎉 Enhanced uHabits deployment package ready!"
echo "📱 Follow the instructions in INSTALL_INSTRUCTIONS.txt on your Samsung phone."
