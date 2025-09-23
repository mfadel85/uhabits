# Google Looker Studio Setup Guide for uHabits Analytics

## 🎯 Why Looker Studio?
- **Free** Google tool for professional dashboards
- **Easy sharing** with teams or personal access
- **Automatic refresh** from data sources
- **No coding required**

## 📋 Setup Steps

### Step 1: Export Your Data
```bash
# Export data in CSV format for Looker Studio
./export_analytics_data.sh
```

### Step 2: Upload to Google Sheets
1. Go to [Google Sheets](https://sheets.google.com)
2. Create a new spreadsheet
3. Import the CSV files:
   - `habits_summary.csv`
   - `daily_performance.csv`
   - `weekly_performance.csv`

### Step 3: Connect to Looker Studio
1. Go to [Looker Studio](https://lookerstudio.google.com)
2. Click "Create" → "Data Source"
3. Select "Google Sheets"
4. Choose your uploaded spreadsheet
5. Click "Connect"

### Step 4: Create Your Dashboard

#### Key Metrics to Track:
- **Success Rate by Priority**: Bar chart showing CRITICAL vs HIGH vs NORMAL vs LOW
- **Habit Distribution**: Pie chart of priority levels
- **Performance Timeline**: Line chart of success rates over time
- **Top Performing Habits**: Table with highest success rates
- **Streak Analysis**: Histogram of current streaks

#### Recommended Visualizations:

**📈 Performance Overview Card**
- Metric: Average Success Rate
- Filter: Last 30 days
- Color coding: Green (>80%), Yellow (50-80%), Red (<50%)

**📊 Priority Comparison Chart**
- Type: Column chart
- X-axis: Priority (CRITICAL, HIGH, NORMAL, LOW)
- Y-axis: Average Success Rate
- Color: Priority-based gradient

**🔥 Streak Distribution**
- Type: Histogram
- X-axis: Current streak length
- Y-axis: Number of habits
- Breakdown: By priority level

**📅 Daily Performance Heatmap**
- Type: Calendar heatmap
- Metric: Daily completion rate
- Color intensity: Success percentage

### Step 5: Advanced Features

#### Calculated Fields
Create these in Looker Studio:

**Weighted Success Score**
```
CASE 
  WHEN Priority = "CRITICAL" THEN Success_Rate * 4.0
  WHEN Priority = "HIGH" THEN Success_Rate * 2.5
  WHEN Priority = "NORMAL" THEN Success_Rate * 1.0
  WHEN Priority = "LOW" THEN Success_Rate * 0.5
  ELSE Success_Rate
END
```

**Performance Category**
```
CASE 
  WHEN Success_Rate >= 0.8 THEN "Excellent"
  WHEN Success_Rate >= 0.6 THEN "Good"
  WHEN Success_Rate >= 0.4 THEN "Needs Work"
  ELSE "Critical"
END
```

#### Filters to Add
- Date range selector
- Priority level filter
- Habit category filter
- Success rate threshold

### Step 6: Sharing Options
1. **View-only sharing**: Share dashboard link
2. **Edit access**: Allow others to modify
3. **Embed**: Put dashboard on website
4. **PDF export**: Generate reports
5. **Email scheduling**: Automatic report delivery

## 🔄 Automatic Updates

### Option A: Google Sheets + AWS (Advanced)
1. Use Google Sheets API
2. Connect to your DynamoDB via Lambda
3. Schedule daily imports

### Option B: Manual Updates (Simple)
1. Run export script weekly
2. Replace data in Google Sheets
3. Dashboard updates automatically

## 📱 Mobile Access
- Download Looker Studio mobile app
- Access dashboards on phone
- Set up notification alerts
- Share insights quickly

## 🎨 Dashboard Templates

### Executive Summary Template
- Total habits count
- Overall success rate
- Critical habits performance
- Weekly improvement trend

### Detailed Analytics Template
- Individual habit performance
- Priority-weighted scores
- Streak analysis
- Daily completion patterns

### Goal Tracking Template
- Progress toward targets
- Habit consistency metrics
- Improvement recommendations
- Achievement celebrations

## 🚀 Pro Tips

1. **Use filters liberally** - Let users explore data
2. **Color code by priority** - Visual priority hierarchy
3. **Add trend indicators** - Show if improving/declining
4. **Include context** - Add text explanations
5. **Mobile-first design** - Optimize for phone viewing

## 📞 Next Steps
1. Export your data: `./export_analytics_data.sh`
2. Upload to Google Sheets
3. Connect to Looker Studio
4. Build your first chart
5. Share with yourself for testing

**Need help?** The export script creates CSV files ready for Google Sheets import!
