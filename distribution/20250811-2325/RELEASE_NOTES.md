# uHabits with Priority-Based Scoring System

**Build Date:** 20250811-2325  
**Commit:** 7483692c  
**Branch:** dev  
**APK Size:** 28M

## 🆕 New Features

### 🏆 Priority-Based Habit Scoring
- **3-Level Priority System**: Set High (🔥), Normal (📝), or Low (🌱) priorities
- **Weighted Scoring**: High priority habits count 3x, Normal 1x, Low 0.5x toward daily performance
- **Visual Priority Indicators**: Color-coded buttons and emojis for easy identification
- **Smart Defaults**: New habits start with Normal priority

### 🎯 Enhanced Daily Performance Calculation
- **Dual Scoring**: Maintains both regular and weighted performance scores
- **Intelligent Weighting**: More important habits contribute more to your daily score
- **Dashboard Integration**: All widgets and charts use the new weighted scoring
- **Backward Compatibility**: Existing habits automatically get Normal priority

### 🔧 Technical Improvements
- **Database Migration**: Seamless upgrade from existing installations
- **Crash Prevention**: Robust error handling for priority system initialization
- **Memory Safety**: Defensive programming prevents crashes from invalid data
- **UI Polish**: Material Design priority selection with visual feedback

## 📱 Installation Instructions

1. **Enable Unknown Sources**: 
   - Go to Settings → Security → Unknown Sources (ON)
   - Or Settings → Apps → Special Access → Install Unknown Apps

2. **Install APK**:
   - Download the APK file
   - Tap to install
   - Follow the installation prompts

3. **Test Priority System**:
   - Open uHabits app
   - Create a new habit or edit an existing one
   - Look for Priority section with High 🔥, Normal 📝, Low 🌱 buttons
   - Set different priorities and check daily performance calculation

## 🧪 Testing Checklist

- [ ] App launches successfully without crashes
- [ ] Can create and edit habits
- [ ] Priority selection works (High, Normal, Low buttons)
- [ ] Priority indicators show correctly in habit list
- [ ] Daily performance calculation reflects priority weights
- [ ] Widgets update with new weighted scoring
- [ ] Database migration completes without issues

## 🔧 Crash Fixes in This Build

- **Memory Safety**: Added bounds checking for priority array access
- **Null Protection**: Safe handling of priority field during database operations
- **Exception Handling**: Graceful fallbacks for invalid priority values
- **Defensive Programming**: Try-catch blocks prevent crashes from priority system

## 📧 Feedback

Report issues or suggestions:
- **GitHub Issues**: [Create Issue](https://github.com/mfadel85/uhabits/issues)
- **Email**: mfadel85@example.com

---
*Built with ❤️ by mfadel85*  
*Based on Loop Habit Tracker by Álinson Santos Xavier*
