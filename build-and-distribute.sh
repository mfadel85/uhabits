#!/bin/bash

# Enhanced uHabits Build and Distribution Script
# Builds APK and creates distribution-ready packages

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}🚀 uHabits Analytics - Build & Distribution Script${NC}"
echo "=================================================="

# Get build info
BUILD_DATE=$(date +'%Y%m%d-%H%M')
COMMIT_HASH=$(git rev-parse --short HEAD 2>/dev/null || echo "unknown")
BRANCH_NAME=$(git branch --show-current 2>/dev/null || echo "unknown")

echo -e "${YELLOW}📋 Build Info:${NC}"
echo "  Date: $BUILD_DATE"
echo "  Commit: $COMMIT_HASH"
echo "  Branch: $BRANCH_NAME"
echo ""

# Clean previous builds
echo -e "${YELLOW}🧹 Cleaning previous builds...${NC}"
./gradlew clean

# Build APK
echo -e "${YELLOW}🏗️  Building Debug APK...${NC}"
export ANDROID_HOME=~/android-sdk
./gradlew assembleDebug

# Check if build was successful
APK_PATH="uhabits-android/build/outputs/apk/debug/uhabits-android-debug.apk"
if [ -f "$APK_PATH" ]; then
    APK_SIZE=$(ls -lh "$APK_PATH" | awk '{print $5}')
    echo -e "${GREEN}✅ Build successful!${NC}"
    echo "  APK: $APK_PATH"
    echo "  Size: $APK_SIZE"
else
    echo -e "${RED}❌ Build failed - APK not found${NC}"
    exit 1
fi

# Create distribution folder
DIST_DIR="distribution/$BUILD_DATE"
mkdir -p "$DIST_DIR"

# Copy APK with better name
NEW_APK_NAME="uHabits-Analytics-$BUILD_DATE-$COMMIT_HASH.apk"
cp "$APK_PATH" "$DIST_DIR/$NEW_APK_NAME"

# Create release notes
cat > "$DIST_DIR/RELEASE_NOTES.md" << EOF
# uHabits with Advanced Analytics

**Build Date:** $BUILD_DATE  
**Commit:** $COMMIT_HASH  
**Branch:** $BRANCH_NAME  
**APK Size:** $APK_SIZE

## 🆕 New Features

### 📊 Advanced Analytics System
- **Data Export**: Export habit data to CSV for analysis
- **PowerBI Integration**: Optimized datasets for PowerBI dashboards
- **Looker Studio Support**: JSON exports for Google's BI platform
- **Excel Compatible**: Standard CSV format for Excel analysis

### 📁 Smart Storage Management
- **Downloads Folder**: Files saved to accessible Downloads directory
- **Fallback Storage**: Robust handling for devices without external storage
- **Permission Management**: Automatic storage permission requests
- **User Feedback**: Clear messaging about export locations

### 🔧 Technical Improvements
- **Multi-location Storage**: Tries Downloads → App Storage → Internal Storage
- **Error Handling**: Graceful handling of storage failures
- **User Experience**: Clear success/failure messages with exact file locations

## 📱 Installation Instructions

1. **Enable Unknown Sources**: 
   - Go to Settings → Security → Unknown Sources (ON)
   - Or Settings → Apps → Special Access → Install Unknown Apps

2. **Install APK**:
   - Download the APK file
   - Tap to install
   - Follow the installation prompts

3. **Test Analytics**:
   - Open uHabits app
   - Tap menu (⋮) → "Analytics & Export"
   - Try exporting data
   - Check Downloads folder for exported files

## 🧪 Testing Checklist

- [ ] App launches successfully
- [ ] Can access Analytics menu
- [ ] Export functionality works
- [ ] Files appear in Downloads folder
- [ ] Permission handling works correctly
- [ ] Error messages are clear and helpful

## 📧 Feedback

Report issues or suggestions:
- **GitHub Issues**: [Create Issue](https://github.com/mfadel85/uhabits/issues)
- **Email**: mfadel85@example.com

---
*Built with ❤️ by mfadel85*  
*Based on Loop Habit Tracker by Álinson Santos Xavier*
EOF

# Create installation guide
cat > "$DIST_DIR/INSTALL_GUIDE.txt" << EOF
🚀 QUICK INSTALL GUIDE
=====================

📱 FOR ANDROID USERS:

1. ENABLE UNKNOWN SOURCES
   → Settings → Security → "Unknown sources" (ON)
   → Or Settings → Apps → "Install unknown apps"

2. DOWNLOAD & INSTALL
   → Download: $NEW_APK_NAME
   → Tap the APK file to install
   → Allow installation when prompted

3. TEST ANALYTICS
   → Open uHabits app
   → Menu (⋮) → "Analytics & Export"
   → Tap "Export All Analytics Data"
   → Check Downloads folder for files

🔧 TROUBLESHOOTING:
- If installation blocked: Check "Unknown sources" setting
- If export fails: Grant storage permission when asked
- If no files appear: Check internal storage or app folder

📧 Need help? Create an issue on GitHub!
EOF

echo ""
echo -e "${GREEN}📦 Distribution package created:${NC}"
echo "  Folder: $DIST_DIR"
echo "  APK: $NEW_APK_NAME"
echo "  Size: $APK_SIZE"
echo "  Release Notes: RELEASE_NOTES.md"
echo "  Install Guide: INSTALL_GUIDE.txt"

# Generate QR code if qrencode is available
if command -v qrencode &> /dev/null; then
    echo ""
    echo -e "${YELLOW}📱 Generating QR code for GitHub release...${NC}"
    GITHUB_RELEASE_URL="https://github.com/mfadel85/uhabits/releases"
    qrencode -t PNG -o "$DIST_DIR/github-releases-qr.png" -s 8 "$GITHUB_RELEASE_URL"
    echo "  QR Code: github-releases-qr.png (scan to go to releases page)"
fi

echo ""
echo -e "${BLUE}🎉 Build completed successfully!${NC}"
echo ""
echo -e "${YELLOW}📤 Next Steps:${NC}"
echo "1. Test the APK locally"
echo "2. Commit and push to trigger GitHub Actions"
echo "3. Check GitHub Releases for automatic distribution"
echo "4. Share the release link with testers"
echo ""
echo -e "${GREEN}Happy testing! 🚀${NC}"
