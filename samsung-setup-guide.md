# Samsung Phone Setup for uHabits Deployment

## Enable Developer Options & USB Debugging

### Method 1: Standard Android Settings
1. **Open Settings** on your Samsung phone
2. **Scroll to "About phone"** (usually at the bottom)
3. **Find "Software information"** 
4. **Tap "Build number" 7 times** rapidly
   - You'll see a message: "You are now a developer!"
5. **Go back to main Settings**
6. **Look for "Developer options"** (usually under System or Advanced)
7. **Enable "USB debugging"**
8. **Enable "Install unknown apps"** (for manual APK installation)

### Method 2: Samsung One UI Specific
1. **Settings** → **About phone**
2. **Software information**
3. **Tap "Build number" 7 times**
4. **Settings** → **Developer options**
5. **Enable USB debugging**
6. **Enable "Wireless debugging"** (optional, for WiFi deployment)

## Additional Samsung-Specific Settings

### Allow Installation from Unknown Sources
1. **Settings** → **Apps**
2. **Special access** → **Install unknown apps**
3. **Select your file manager** (e.g., My Files)
4. **Enable "Allow from this source"**

### OEM Unlocking (Optional - for advanced users)
1. In **Developer options**
2. **Enable "OEM unlocking"** (only if you plan to unlock bootloader later)

## USB Connection Settings
- When you connect to Ubuntu, choose **"File Transfer (MTP)"** mode
- Allow USB debugging when prompted on the phone
- Trust the computer when the RSA fingerprint dialog appears

## Samsung Specific Notes
- **Samsung Knox**: May interfere with some developer features
- **Samsung DeX**: Disable if you experience connection issues
- **Smart Switch**: Close Samsung Smart Switch if running
- **Samsung USB Drivers**: Usually not needed on Linux, but may be required on Windows

## WiFi Debugging (Android 11+)
If you prefer wireless deployment:
1. **Developer options** → **Wireless debugging**
2. **Enable "Wireless debugging"**
3. Connect Ubuntu and phone to same WiFi network
4. Use `adb pair` and `adb connect` commands

## Troubleshooting Samsung Connection Issues

### Common Issues:
1. **Phone not detected**: Try different USB cable/port
2. **Unauthorized device**: Re-enable USB debugging, revoke previous authorizations
3. **MTP issues**: Switch to PTP mode temporarily
4. **Knox interference**: Some Knox policies may block debugging

### Reset ADB Authorization:
1. **Developer options** → **Revoke USB debugging authorizations**
2. **Reconnect USB cable**
3. **Accept new RSA fingerprint**

### Samsung-specific ADB Commands:
```bash
# Check if Samsung device is detected
adb devices

# Samsung-specific device info
adb shell getprop ro.product.manufacturer
adb shell getprop ro.product.model

# Install APK to Samsung device
adb install -r path/to/uhabits.apk

# Check Samsung One UI version
adb shell getprop ro.build.version.oneui
```
