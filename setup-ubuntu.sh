#!/bin/bash

# Ubuntu Setup Script for uHabits Android Build
# Run this script to install all required dependencies

echo "🚀 Setting up Ubuntu environment for uHabits Android development..."

# Update system packages
sudo apt update && sudo apt upgrade -y

# Install Java 17 (required by the project)
echo "📦 Installing OpenJDK 17..."
sudo apt install -y openjdk-17-jdk

# Set JAVA_HOME
echo 'export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64' >> ~/.bashrc
echo 'export PATH=$PATH:$JAVA_HOME/bin' >> ~/.bashrc
source ~/.bashrc

# Install Android SDK Command Line Tools
echo "📱 Installing Android SDK..."
mkdir -p ~/android-sdk/cmdline-tools
cd ~/android-sdk/cmdline-tools

# Download Android Command Line Tools (latest version)
wget https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
unzip commandlinetools-linux-11076708_latest.zip
mv cmdline-tools latest
rm commandlinetools-linux-11076708_latest.zip

# Set Android environment variables
echo 'export ANDROID_HOME=$HOME/android-sdk' >> ~/.bashrc
echo 'export ANDROID_SDK_ROOT=$HOME/android-sdk' >> ~/.bashrc
echo 'export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin' >> ~/.bashrc
echo 'export PATH=$PATH:$ANDROID_HOME/platform-tools' >> ~/.bashrc
echo 'export PATH=$PATH:$ANDROID_HOME/build-tools/34.0.0' >> ~/.bashrc
source ~/.bashrc

# Accept Android SDK licenses
yes | $ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager --licenses

# Install required Android SDK components
echo "⚙️ Installing Android SDK components..."
$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager \
    "platform-tools" \
    "platforms;android-36" \
    "platforms;android-34" \
    "platforms;android-33" \
    "platforms;android-28" \
    "build-tools;34.0.0" \
    "build-tools;33.0.0" \
    "ndk;25.1.8937393"

# Install Git (if not already installed)
sudo apt install -y git curl wget unzip

# Install additional build tools
sudo apt install -y build-essential

echo "✅ Ubuntu setup complete!"
echo ""
echo "🔧 Verification:"
echo "Java version: $(java -version)"
echo "ANDROID_HOME: $ANDROID_HOME"
echo ""
echo "📝 Next steps:"
echo "1. Restart your terminal or run: source ~/.bashrc"
echo "2. Navigate to your uHabits project directory"
echo "3. Run: ./gradlew build"
