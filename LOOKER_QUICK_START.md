# 📊 Quick Reference: Google Looker Studio API Endpoints

## 🔗 API Endpoints

### Base URL
```
https://kpitracker.quest/api/looker.php
```

---

## 📋 Available Datasets

### 1️⃣ HABITS (Main Dataset) - 32,351 records
```
GET https://kpitracker.quest/api/looker.php?type=habits
```
**What**: Daily checkmarks flattened into rows  
**Best for**: Trend analysis, day-of-week patterns, individual habit tracking  
**Key fields**: `record_date`, `habit_name`, `value`, `completed`, `score`, `category`

---

### 2️⃣ SUMMARY (Overview) - 123 records
```
GET https://kpitracker.quest/api/looker.php?type=summary
```
**What**: Aggregated metrics per sync  
**Best for**: KPI dashboards, overall performance trends, grade distribution  
**Key fields**: `sync_date`, `average_score`, `grade_a_count`, `total_habits`

---

### 3️⃣ CATEGORIES (Group Performance) - 489 records
```
GET https://kpitracker.quest/api/looker.php?type=categories
```
**What**: Category-level stats over time  
**Best for**: Category comparison, focus area identification  
**Key fields**: `category`, `average_score`, `habit_count`, `grade`

---

### 4️⃣ STREAKS (Motivation) - 3,351 records
```
GET https://kpitracker.quest/api/looker.php?type=streaks
```
**What**: Active streaks by habit  
**Best for**: Streak leaderboards, consistency tracking  
**Key fields**: `habit_name`, `streak_length`, `score`, `category`

---

## 🎯 Common Use Cases

### Executive Dashboard
```
Data Source: summary
Visuals: 
- Scorecards: average_score, total_habits, grade_a_count
- Line chart: sync_date × average_score
- Pie chart: grade distribution
```

### Habit Tracking Dashboard
```
Data Source: habits
Visuals:
- Time series: record_date × value (filtered by habit_name)
- Heatmap: habit_name × day_of_week × COUNT(completed)
- Table: habit_name, score, streak_length
```

### Category Analysis
```
Data Source: categories
Visuals:
- Bar chart: category × average_score
- Line chart: sync_date × average_score (breakdown by category)
- Table: category, habit_count, grade
```

### Streak Leaderboard
```
Data Source: streaks
Visuals:
- Table: habit_name, streak_length, score (sorted DESC)
- Scorecard: MAX(streak_length)
- Bar chart: habit_name × streak_length (TOP 10)
```

---

## 🔧 Quick Setup Steps

1. **Upload API** ✅ DONE
   ```bash
   # Already deployed to: /var/www/kpitracker.quest/api/looker.php
   ```

2. **Test Endpoints** ✅ DONE
   ```bash
   curl "https://kpitracker.quest/api/looker.php?type=summary"
   ```

3. **Open Looker Studio**
   - Go to: https://lookerstudio.google.com/
   - Create → Data Source → JSON/URL Connector

4. **Add Data Source**
   - URL: `https://kpitracker.quest/api/looker.php?type=habits`
   - Name: "uHabits - Daily Records"
   - Click Connect

5. **Create Report**
   - Create → Report
   - Select "uHabits - Daily Records"
   - Start building charts!

---

## 📊 Sample Visualizations

### Chart 1: Score Trend (Line)
- **X-axis**: `record_date` (Date)
- **Y-axis**: `AVG(score)` (Metric)
- **Breakdown**: `habit_name` or `category`

### Chart 2: Completion Rate by Day (Bar)
- **Dimension**: `day_of_week`
- **Metric**: `COUNT(completed = true) / COUNT(*)`
- **Sort**: Monday → Sunday

### Chart 3: Category Performance (Pie)
- **Data**: categories endpoint
- **Dimension**: `category`
- **Metric**: `average_score`

### Chart 4: Top Streaks (Table)
- **Data**: streaks endpoint
- **Columns**: `habit_name`, `streak_length`, `category`
- **Sort**: `streak_length DESC`
- **Limit**: 10

### Chart 5: Numerical Habit Values (Area)
- **Filter**: `is_numerical = true`
- **X-axis**: `record_date`
- **Y-axis**: `value`
- **Breakdown**: `habit_name`

---

## 🎨 Dashboard Templates

### Template 1: Personal KPI Dashboard
```
Layout:
┌──────────────────────────────────────┐
│  Avg Score    Total Habits   Streaks │ Scorecards
├──────────────────────────────────────┤
│                                      │
│     Score Trend (Last 30 Days)       │ Line Chart
│                                      │
├────────────────┬─────────────────────┤
│   Category     │   Grade             │
│   Performance  │   Distribution      │ Pie Charts
└────────────────┴─────────────────────┘
```

### Template 2: Habit Deep Dive
```
Layout:
┌──────────────────────────────────────┐
│  [Habit Selector ▾]  [Date Range]    │ Filters
├──────────────────────────────────────┤
│                                      │
│     Daily Completion Heatmap          │ Table
│     (7 days × selected habits)        │
├──────────────────────────────────────┤
│                                      │
│     Value Trend (if numerical)        │ Area Chart
│                                      │
└──────────────────────────────────────┘
```

### Template 3: Category Comparison
```
Layout:
┌──────────────────────────────────────┐
│  Category Rankings                   │
│  ┌──────────────────────────────┐   │
│  │ Prayer        A    12 habits │   │ Table
│  │ Health        B     8 habits │   │
│  │ Learning      C     5 habits │   │
│  └──────────────────────────────┘   │
├──────────────────────────────────────┤
│                                      │
│  Category Performance Over Time      │ Multi-line
│                                      │
└──────────────────────────────────────┘
```

---

## 💡 Pro Tips

### Filter by Date Range
```
Add parameter to URL:
?type=habits&start_date=2025-11-01&end_date=2025-11-30
```

### Combine Data Sources (Blended Data)
```
1. Start with habits data
2. Click "Blend Data"
3. Add summary data
4. Join on: sync_date = sync_date
5. Use fields from both sources
```

### Create Calculated Fields

**Week Number**:
```
WEEK(record_date)
```

**Month Name**:
```
MONTH_NAME(record_date)
```

**Success Rate %**:
```
COUNT(CASE WHEN completed = true THEN 1 END) / COUNT(*) * 100
```

**Streak Status**:
```
CASE
  WHEN streak_length >= 30 THEN "🔥 Strong"
  WHEN streak_length >= 7 THEN "⚡ Building"
  ELSE "🌱 New"
END
```

---

## 🚀 Current Data Stats

- **Total daily records**: 32,351
- **Total syncs**: 123
- **Categories tracked**: 8-10
- **Active streaks**: 25 habits
- **Longest streak**: 323 days (Day Planned)
- **Average score**: 85.07%
- **Habits with grade A**: 12/25

---

## 📞 Support

**Test Endpoints**:
```bash
curl "https://kpitracker.quest/api/looker.php?type=summary" | jq
```

**Check Logs**:
```bash
ssh root@kpitracker.quest "tail -f /var/log/nginx/error.log"
```

**Verify Database**:
```bash
mysql -u uhabits_user -p uhabits_analytics
```

---

**Ready to build your custom BI dashboard! 🎉**

Full guide: See `GOOGLE_LOOKER_SETUP.md`
