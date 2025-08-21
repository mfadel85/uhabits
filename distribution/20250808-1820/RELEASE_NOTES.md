# uHabits with Advanced Analytics

**Build Date:** 20250808-1820  
**Commit:** 7483692c  
**Branch:** dev  
**APK Size:** 28M

## 🆕 New Features

### � Home Screen Widget: Daily Performance
- **Quick Access**: View today's performance directly on your home screen
- **Progress Tracking**: See completion percentage and remaining habits at a glance
- **Smart Design**: Color-coded performance indicators (Excellent/Good/Average/Poor)
- **One-Tap Access**: Tap widget to open the full uHabits app
- **Real-time Updates**: Widget automatically refreshes as you complete habits

### �📊 Advanced Analytics System
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

3. **Add Home Screen Widget**:
   - Long press on home screen → Widgets
   - Find "uHabits" → "Daily Performance" 
   - Add widget to home screen
   - Widget shows today's performance and remaining habits

4. **Test Analytics**:
   - Open uHabits app
   - Tap menu (⋮) → "Analytics & Export"
   - Try exporting data
   - Check Downloads folder for exported files

## 🧪 Testing Checklist

- [ ] App launches successfully
- [ ] Daily Performance Widget can be added to home screen
- [ ] Widget displays correct performance score and habit count
- [ ] Widget updates when habits are completed
- [ ] Tapping widget opens the main app
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
