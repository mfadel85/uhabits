#!/bin/bash

# uHabits Analytics - Deployment Status Checker
# Quick status check for builds and distribution

set -e

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}📊 uHabits Analytics - Deployment Status${NC}"
echo "=============================================="

# Check local environment
echo -e "${YELLOW}🔧 Local Environment:${NC}"

# Check Java
if java -version >/dev/null 2>&1; then
    JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2)
    echo -e "  ✅ Java: $JAVA_VERSION"
else
    echo -e "  ❌ Java: Not installed"
fi

# Check Android SDK
if [ -d "$HOME/android-sdk" ]; then
    echo -e "  ✅ Android SDK: $HOME/android-sdk"
else
    echo -e "  ❌ Android SDK: Not found"
fi

# Check Gradle
if ./gradlew --version >/dev/null 2>&1; then
    GRADLE_VERSION=$(./gradlew --version 2>/dev/null | grep "Gradle" | head -n 1)
    echo -e "  ✅ $GRADLE_VERSION"
else
    echo -e "  ❌ Gradle: Not working"
fi

echo ""

# Check recent builds
echo -e "${YELLOW}📦 Recent Builds:${NC}"
if [ -d "distribution" ]; then
    RECENT_BUILDS=$(find distribution -name "*.apk" -type f -printf "%T@ %p\n" | sort -nr | head -3)
    if [ -n "$RECENT_BUILDS" ]; then
        while IFS= read -r line; do
            TIMESTAMP=$(echo "$line" | cut -d' ' -f1)
            APK_PATH=$(echo "$line" | cut -d' ' -f2-)
            APK_NAME=$(basename "$APK_PATH")
            BUILD_DATE=$(date -d "@$TIMESTAMP" "+%Y-%m-%d %H:%M")
            APK_SIZE=$(ls -lh "$APK_PATH" | awk '{print $5}')
            echo -e "  ✅ $APK_NAME"
            echo -e "     📅 $BUILD_DATE  📏 $APK_SIZE"
        done <<< "$RECENT_BUILDS"
    else
        echo -e "  ⚠️  No APKs found in distribution folder"
    fi
else
    echo -e "  ⚠️  No distribution folder found"
fi

echo ""

# Check GitHub Actions status
echo -e "${YELLOW}🔄 GitHub Actions:${NC}"
if [ -d ".github/workflows" ]; then
    WORKFLOW_COUNT=$(find .github/workflows -name "*.yml" -o -name "*.yaml" | wc -l)
    echo -e "  ✅ Workflows: $WORKFLOW_COUNT configured"
    
    for workflow in .github/workflows/*.yml .github/workflows/*.yaml; do
        if [ -f "$workflow" ]; then
            WORKFLOW_NAME=$(basename "$workflow" | sed 's/\.[^.]*$//')
            echo -e "     📄 $WORKFLOW_NAME"
        fi
    done
else
    echo -e "  ❌ No GitHub Actions workflows found"
fi

echo ""

# Check Firebase setup
echo -e "${YELLOW}🔥 Firebase Setup:${NC}"
if [ -f "FIREBASE_SETUP.md" ]; then
    echo -e "  ✅ Setup guide available: FIREBASE_SETUP.md"
else
    echo -e "  ⚠️  Firebase setup guide not found"
fi

echo ""

# Check git status
echo -e "${YELLOW}📋 Git Status:${NC}"
if git status >/dev/null 2>&1; then
    BRANCH=$(git branch --show-current 2>/dev/null || echo "unknown")
    COMMIT=$(git rev-parse --short HEAD 2>/dev/null || echo "unknown")
    REMOTE_URL=$(git remote get-url origin 2>/dev/null || echo "no remote")
    
    echo -e "  📌 Branch: $BRANCH"
    echo -e "  🔖 Commit: $COMMIT"
    echo -e "  🌐 Remote: $REMOTE_URL"
    
    # Check if there are uncommitted changes
    if ! git diff-index --quiet HEAD -- 2>/dev/null; then
        echo -e "  ⚠️  You have uncommitted changes"
    else
        echo -e "  ✅ Working tree clean"
    fi
else
    echo -e "  ❌ Not a git repository"
fi

echo ""

# Recommendations
echo -e "${YELLOW}📝 Next Steps:${NC}"

# Check if APK exists
if ls uhabits-android/build/outputs/apk/debug/*.apk >/dev/null 2>&1; then
    echo -e "  ✅ Latest APK ready for testing"
else
    echo -e "  🔨 Run: ${BLUE}./build-and-distribute.sh${NC} to build APK"
fi

# Check git status for cloud deployment
if git status >/dev/null 2>&1; then
    if ! git diff-index --quiet HEAD -- 2>/dev/null; then
        echo -e "  📤 Commit and push changes to trigger cloud build"
    else
        echo -e "  🚀 Push to trigger GitHub Actions build"
    fi
fi

# Firebase setup check
if [ ! -f ".github/workflows/firebase-distribution.yml" ]; then
    echo -e "  🔥 Set up Firebase App Distribution for auto-testing"
else
    echo -e "  🧪 Configure Firebase secrets for auto-distribution"
fi

echo ""
echo -e "${GREEN}📱 Test your APK:${NC}"
echo -e "  1. Install APK on device"
echo -e "  2. Open uHabits → Menu → Analytics & Export"
echo -e "  3. Test data export functionality"
echo -e "  4. Check Downloads folder for exported files"

echo ""
echo -e "${BLUE}Happy coding! 🚀${NC}"
