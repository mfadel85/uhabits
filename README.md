# 🚀 Enhanced uHabits - Advanced Analytics Fork

[![Build APK](https://github.com/mfadel85/uhabits/actions/workflows/build-apk.yml/badge.svg)](https://github.com/mfadel85/uhabits/actions/workflows/build-apk.yml)
[![Release](https://github.com/mfadel85/uhabits/actions/workflows/release.yml/badge.svg)](https://github.com/mfadel85/uhabits/actions/workflows/release.yml)

> An enhanced fork of [Loop Habit Tracker](https://github.com/iSoron/uhabits) with advanced analytics, 100-point scoring, and business intelligence integration.

## 🎯 What's Enhanced

### 📊 **Advanced Analytics System**
- **100-Point Scoring**: Intuitive 0-100 scale instead of confusing 0-1 decimals
- **Multi-Timeframe Analysis**: Daily, weekly, and monthly performance metrics
- **Trend Detection**: Automatic identification of improving/declining patterns
- **Streak Bonuses**: Extra points for consistent habit maintenance
- **Consistency Scoring**: Measure habit regularity and stability

### 🔗 **Business Intelligence Integration**
- **PowerBI Export**: CSV format optimized for Microsoft PowerBI dashboards
- **Looker Studio Support**: JSON export for Google's BI platform  
- **Custom Analytics**: Comprehensive data export for any BI tool
- **Professional Reports**: Executive-ready habit performance summaries

### 🎨 **Enhanced User Interface**
- **Professional Dashboard**: Tab-based analytics interface
- **Progress Indicators**: Circular progress bars with 0-100 scores
- **Trend Arrows**: Visual indicators for performance direction
- **Correlation Analysis**: Discover relationships between habits

## 📱 Download & Install

### 🎯 **Latest Release**
[![Download APK](https://img.shields.io/badge/Download-Enhanced%20uHabits%20APK-brightgreen?style=for-the-badge&logo=android)](https://github.com/mfadel85/uhabits/releases/latest)

### 📋 **Installation Steps**
1. **Download** the latest APK from [Releases](https://github.com/mfadel85/uhabits/releases)
2. **Enable Unknown Sources** in Android Settings
3. **Install** the APK file
4. **Launch** "Loop Habit Tracker"
5. **Access Analytics** via Menu → "Advanced Analytics"

### 🔧 **Requirements**
- Android 7.0+ (API level 28+)
- ~10MB storage space
- Same permissions as original uHabits

## 🎯 Key Features Comparison

| Feature | Original uHabits | Enhanced uHabits |
|---------|------------------|------------------|
| Scoring System | 0-1 decimal | ✅ 0-100 points |
| Analytics | Basic charts | ✅ Advanced dashboard |
| Data Export | CSV only | ✅ PowerBI + Looker Studio |
| Performance Tracking | Daily only | ✅ Daily/Weekly/Monthly |
| Trend Analysis | None | ✅ Automatic detection |
| Habit Correlations | None | ✅ Advanced correlation |
| Business Intelligence | None | ✅ Full BI integration |

## 📊 Business Intelligence Setup

### **PowerBI Integration**
```powershell
# Export from Enhanced uHabits
Menu → Advanced Analytics → Export → PowerBI Dataset

# Import in PowerBI Desktop
Get Data → Text/CSV → Import daily_scores.csv + habit_metadata.csv
Create relationships → Build 0-100 score visualizations
```

### **Looker Studio Integration**
```javascript
// Export from Enhanced uHabits
Menu → Advanced Analytics → Export → Looker Studio

// Import in Looker Studio  
Create New Report → Add Data Source → File Upload → JSON file
Use ScoreCategory field for color-coded charts
```

## 🏗️ Build from Source

### **GitHub Actions (Recommended)**
1. **Fork this repository**
2. **Push changes** to trigger automatic builds
3. **Download APK** from Actions artifacts
4. **Create releases** by pushing version tags (`v1.0.0`)

### **Local Ubuntu Build**
```bash
# Clone repository
git clone https://github.com/mfadel85/uhabits.git
cd uhabits

# Setup environment (one-time)
chmod +x setup-ubuntu.sh
./setup-ubuntu.sh

# Build APK
chmod +x build-ubuntu.sh
./build-ubuntu.sh

# Deploy to phone
chmod +x deploy-samsung.sh
./deploy-samsung.sh
```

### **Manual Build**
```bash
cd uhabits
./gradlew clean
./gradlew :uhabits-android:assembleDebug
# APK location: uhabits-android/build/outputs/apk/debug/
```

## 🎨 Screenshots

> *Screenshots coming soon - showing the enhanced analytics dashboard*

## 🔧 Technical Architecture

### **Enhanced Components**
- `AdvancedScore.kt` - 100-point scoring system with analytics
- `AnalyticsEngine.kt` - Comprehensive habit analysis engine  
- `AnalyticsDataExporter.kt` - Multi-format BI data export
- `AdvancedAnalyticsActivity.kt` - Professional analytics UI
- 15+ XML layouts for enhanced user interface

### **Compatibility**
- ✅ **Backward Compatible**: Works with existing uHabits data
- ✅ **Same Permissions**: No additional privacy concerns
- ✅ **Performance**: Minimal impact on app performance
- ✅ **Storage**: Efficient data storage and caching

## 📈 Analytics Features Deep Dive

### **Scoring Algorithm**
```kotlin
Daily Score = (Base Score × 0.7) + (Consistency × 0.2) + (Streak Bonus × 0.1)
Weekly Score = 7-day average × Frequency Multiplier
Monthly Score = 30-day average + Maturity Bonus
```

### **Performance Categories**
- 🟢 **Excellent**: 80-100 points
- 🟡 **Good**: 60-79 points  
- 🟠 **Average**: 40-59 points
- 🔴 **Poor**: 0-39 points

### **Trend Detection**
- 📈 **Improving**: +5 points from previous period
- 📉 **Declining**: -5 points from previous period
- ➡️ **Stable**: Within ±5 point range

## 🤝 Contributing

### **Development Setup**
```bash
git clone https://github.com/mfadel85/uhabits.git
cd uhabits/uhabits
./gradlew build
```

### **Areas for Contribution**
- 🎨 **UI Improvements**: Enhanced charts and visualizations
- 📊 **Analytics Features**: New correlation algorithms
- 🔗 **BI Integrations**: Additional export formats
- 🌐 **Translations**: Localization for analytics features
- 🧪 **Testing**: Unit tests for analytics components

## 📄 License

This project maintains the same **GNU General Public License v3.0** as the original Loop Habit Tracker.

- ✅ **Free to use** for personal and commercial purposes
- ✅ **Open source** - view and modify the code
- ✅ **Share improvements** with the community

## 🙏 Acknowledgments

- **[Álinson Santos Xavier](https://github.com/iSoron)** - Original Loop Habit Tracker creator
- **Loop Habit Tracker Community** - Feature requests and feedback
- **GitHub Actions** - Automated build and release system

## 📞 Support

### **Getting Help**
- 📋 **Issues**: [GitHub Issues](https://github.com/mfadel85/uhabits/issues)
- 💬 **Discussions**: [GitHub Discussions](https://github.com/mfadel85/uhabits/discussions)
- 📖 **Documentation**: Check the `/docs` folder and wiki

### **Feature Requests**
Have ideas for more analytics features? Open an issue with the `enhancement` label!

---

**Built with ❤️ by [mfadel85](https://github.com/mfadel85)**

*Transform your habit tracking into a powerful analytics system! 📈*
