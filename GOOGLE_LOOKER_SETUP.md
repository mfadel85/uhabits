# Google Looker Studio Setup Guide for uHabits Analytics

## Overview

This guide will help you connect your uHabits data to Google Looker Studio (formerly Data Studio) to create custom dashboards and BI reports.

## API Endpoints for Looker Studio

### Base URL
```
https://kpitracker.quest/api/looker.php
```

### Available Endpoints

#### 1. **Habits Dataset** (Main/Primary)
```
GET https://kpitracker.quest/api/looker.php?type=habits
```

**Description**: Flattened daily habit performance records. Each checkmark becomes a row.

**Fields**:
- `sync_id` (Number) - Sync record ID
- `user_id` (Text) - User identifier
- `sync_date` (Date) - Date of sync
- `sync_datetime` (Date & Time) - Full sync timestamp
- `habit_name` (Text) - Name of habit
- `habit_group` (Text) - Habit group/category grouping
- `category` (Text) - Habit category (Prayer, Health, Learning, etc.)
- `priority` (Number) - Priority level (1=Critical, 5=None)
- `priority_label` (Text) - Priority as text (Critical, High, Medium, Low, None)
- `frequency` (Number) - Target frequency per week
- `score` (Number) - Success rate percentage (0-100)
- `score_percentage` (Text) - Score with % sign
- `grade` (Text) - Letter grade (A, B, C, D, F)
- `streak_length` (Number) - Current streak in days
- `is_numerical` (Boolean) - Whether habit tracks numbers
- `habit_type` (Text) - "Numerical" or "Yes/No"
- `record_date` (Date) - Date of specific checkmark
- `value` (Number) - Numerical value (for numerical habits)
- `completed` (Boolean) - Whether completed on that day
- `completed_text` (Text) - "Yes" or "No"
- `day_of_week` (Text) - Monday, Tuesday, etc.

**Use Cases**:
- Daily performance trends
- Individual habit tracking over time
- Day-of-week analysis
- Numerical habit value visualization

**Optional Parameters**:
- `start_date=YYYY-MM-DD` - Filter records from this date
- `end_date=YYYY-MM-DD` - Filter records until this date
- `user_id=xxx` - Filter by user

---

#### 2. **Summary Dataset**
```
GET https://kpitracker.quest/api/looker.php?type=summary
```

**Description**: Aggregated metrics per sync event.

**Fields**:
- `sync_date` (Date) - Date of sync
- `sync_datetime` (Date & Time) - Full sync timestamp
- `user_id` (Text) - User identifier
- `total_habits` (Number) - Total habits tracked
- `active_habits` (Number) - Currently active habits
- `average_score` (Number) - Average score across all habits
- `min_score` (Number) - Lowest habit score
- `max_score` (Number) - Highest habit score
- `numerical_habits` (Number) - Count of numerical habits
- `boolean_habits` (Number) - Count of yes/no habits
- `critical_count` (Number) - Number of critical priority habits
- `high_count` (Number) - Number of high priority habits
- `medium_count` (Number) - Number of medium priority habits
- `low_count` (Number) - Number of low priority habits
- `grade_a_count` (Number) - Habits with A grade (90%+)
- `grade_b_count` (Number) - Habits with B grade (80-89%)
- `grade_c_count` (Number) - Habits with C grade (70-79%)
- `grade_d_count` (Number) - Habits with D grade (60-69%)
- `grade_f_count` (Number) - Habits with F grade (<60%)

**Use Cases**:
- Overall performance trends over time
- Grade distribution analysis
- Priority distribution tracking
- High-level KPI dashboard

---

#### 3. **Categories Dataset**
```
GET https://kpitracker.quest/api/looker.php?type=categories
```

**Description**: Category-level aggregated performance over time.

**Fields**:
- `sync_date` (Date) - Date of sync
- `sync_datetime` (Date & Time) - Full sync timestamp
- `category` (Text) - Category name
- `habit_count` (Number) - Number of habits in category
- `average_score` (Number) - Average score for category
- `min_score` (Number) - Lowest score in category
- `max_score` (Number) - Highest score in category
- `grade` (Text) - Letter grade for category
- `total_streak_days` (Number) - Sum of all streaks in category
- `average_streak` (Number) - Average streak length in category

**Use Cases**:
- Category comparison (Prayer vs Health vs Learning)
- Category performance trends
- Focus area identification

---

#### 4. **Streaks Dataset**
```
GET https://kpitracker.quest/api/looker.php?type=streaks
```

**Description**: Habit streak tracking over time.

**Fields**:
- `sync_date` (Date) - Date of sync
- `sync_datetime` (Date & Time) - Full sync timestamp
- `habit_name` (Text) - Name of habit
- `category` (Text) - Habit category
- `priority` (Number) - Priority level
- `priority_label` (Text) - Priority as text
- `streak_length` (Number) - Streak length in days
- `score` (Number) - Current score percentage

**Use Cases**:
- Longest streaks visualization
- Streak maintenance tracking
- Motivational dashboards

---

## Step-by-Step Setup Instructions

### Step 1: Upload API to VPS

```bash
# Upload the Looker API file to your VPS
scp /home/muosman/uHabits/uhabits/looker_api.php root@kpitracker.quest:/var/www/kpitracker.quest/api/looker.php
```

Test the endpoint:
```bash
curl "https://kpitracker.quest/api/looker.php?type=summary"
```

### Step 2: Access Google Looker Studio

1. Go to [https://lookerstudio.google.com/](https://lookerstudio.google.com/)
2. Sign in with your Google account
3. Click **Create** → **Data Source**

### Step 3: Connect Data Source

1. **Select Connector**:
   - Search for **"JSON / CSV"** or **"URL Fetch"** connector
   - Alternatively, use **"Google Sheets"** connector if you want to import via sheets first

2. **For JSON/URL Fetch Connector**:
   - Enter URL: `https://kpitracker.quest/api/looker.php?type=habits`
   - Set refresh schedule (e.g., hourly, daily)
   - Click **Connect**

3. **For Google Sheets Method** (Recommended for beginners):
   - Create a new Google Sheet
   - Use **IMPORTDATA** function:
     ```
     =IMPORTDATA("https://kpitracker.quest/api/looker.php?type=habits")
     ```
   - Or use Google Apps Script to fetch JSON and parse it
   - Then connect Looker to the Sheet

### Step 4: Configure Field Types

Looker Studio will auto-detect field types, but verify:

- **Date fields**: Ensure `sync_date` and `record_date` are set as "Date"
- **Datetime fields**: Set `sync_datetime` as "Date & Time"
- **Number fields**: `score`, `value`, `streak_length`, etc. should be "Number"
- **Text fields**: `habit_name`, `category`, `priority_label` should be "Text"
- **Boolean fields**: `completed`, `is_numerical` should be "Boolean"

### Step 5: Create Multiple Data Sources

Repeat the connection process for each dataset:

1. **Habits Data Source**:
   - Name: "uHabits - Daily Records"
   - URL: `https://kpitracker.quest/api/looker.php?type=habits`

2. **Summary Data Source**:
   - Name: "uHabits - Summary"
   - URL: `https://kpitracker.quest/api/looker.php?type=summary`

3. **Categories Data Source**:
   - Name: "uHabits - Categories"
   - URL: `https://kpitracker.quest/api/looker.php?type=categories`

4. **Streaks Data Source**:
   - Name: "uHabits - Streaks"
   - URL: `https://kpitracker.quest/api/looker.php?type=streaks`

### Step 6: Create Your First Report

1. Click **Create** → **Report**
2. Select data source: "uHabits - Daily Records"
3. Click **Add to Report**

### Step 7: Build Visualizations

#### Example Chart 1: Score Over Time (Time Series)
- Chart Type: **Line Chart**
- Date Range Dimension: `record_date`
- Dimension: `habit_name`
- Metric: `AVG(score)`
- Optional Dimension: `category` (for color grouping)

#### Example Chart 2: Category Performance (Pie Chart)
- Data Source: "uHabits - Categories"
- Chart Type: **Pie Chart**
- Dimension: `category`
- Metric: `average_score`

#### Example Chart 3: Habit Completion Heatmap
- Chart Type: **Table with Heatmap**
- Row Dimension: `habit_name`
- Column Dimension: `day_of_week`
- Metric: `COUNT(completed)` where `completed = true`

#### Example Chart 4: Streak Leaders (Scorecard)
- Data Source: "uHabits - Streaks"
- Chart Type: **Scorecard** or **Table**
- Dimension: `habit_name`
- Metric: `MAX(streak_length)`
- Sort: Descending by streak

#### Example Chart 5: Priority Distribution (Bar Chart)
- Data Source: "uHabits - Summary"
- Chart Type: **Stacked Bar Chart**
- Date Dimension: `sync_date`
- Metrics: `critical_count`, `high_count`, `medium_count`, `low_count`

#### Example Chart 6: Numerical Habit Values (Area Chart)
- Data Source: "uHabits - Daily Records"
- Chart Type: **Area Chart**
- Filter: `is_numerical = true`
- Date Dimension: `record_date`
- Dimension: `habit_name`
- Metric: `value`

### Step 8: Add Filters and Controls

1. **Date Range Control**:
   - Add Control → Date Range Control
   - Link to `record_date` or `sync_date`

2. **Category Filter**:
   - Add Control → Drop-down List
   - Link to `category` field

3. **Habit Name Filter**:
   - Add Control → Drop-down List (Allow Multiple)
   - Link to `habit_name` field

4. **Priority Filter**:
   - Add Control → Drop-down List
   - Link to `priority_label` field

---

## Advanced Tips

### Using Blended Data

Combine multiple data sources:
1. Create a chart with "uHabits - Daily Records"
2. Click **Blend Data**
3. Add "uHabits - Summary" as second source
4. Join on `sync_date` = `sync_date`

### Calculated Fields

Create custom metrics:

**Success Rate**:
```
SUM(CASE WHEN completed = true THEN 1 ELSE 0 END) / COUNT(record_date) * 100
```

**Weekly Average Value** (for numerical habits):
```
AVG(value) * 7
```

**Streak Status**:
```
CASE 
  WHEN streak_length >= 30 THEN "Strong"
  WHEN streak_length >= 7 THEN "Building"
  ELSE "New"
END
```

**Month Name**:
```
MONTH_NAME(record_date)
```

### Refresh Schedule

1. Go to **Resource** → **Manage Added Data Sources**
2. Click on your data source
3. Set **Data Freshness** to:
   - **1 hour** for near real-time
   - **Daily** for once-a-day refresh

---

## Sample Dashboard Layouts

### Layout 1: Executive Dashboard
- **Top Row**: Scorecards (Total Habits, Average Score, Total Streaks)
- **Middle**: Line chart showing score trends over 30 days
- **Bottom**: Category breakdown (pie chart) + Grade distribution (bar chart)

### Layout 2: Habit Deep Dive
- **Filters**: Habit selector, Date range
- **Main Chart**: Daily completion heatmap (7 days x habits)
- **Side Panel**: Numerical values trend, Current streak scorecard

### Layout 3: Category Comparison
- **Table**: Categories with avg score, habit count, grade
- **Chart 1**: Category performance over time (multi-line)
- **Chart 2**: Priority distribution within each category (stacked bar)

### Layout 4: Personal KPI Dashboard
- **Scorecards**: Best streak, Worst performing habit, Overall grade
- **Gauge Charts**: Score for each priority category (Critical, High, Medium)
- **Timeline**: Completion rate by day of week

---

## Troubleshooting

### Issue: Data not loading
- Check URL is accessible: `curl https://kpitracker.quest/api/looker.php?type=habits`
- Verify CORS headers are set (already configured in PHP)
- Check JSON format is valid

### Issue: Dates not parsing correctly
- Ensure dates are in `YYYY-MM-DD` format
- Check timezone settings in Looker Studio
- Use calculated field to parse if needed: `PARSE_DATE("%Y-%m-%d", record_date)`

### Issue: Too much data / slow loading
- Add date filters to API: `?type=habits&start_date=2025-11-01`
- Use Summary or Categories endpoints instead of Habits for overview dashboards
- Enable data caching in Looker Studio

### Issue: Missing numerical values
- Verify `is_numerical` flag is true in database
- Check `value` field is not null
- Filter charts: `is_numerical = true AND value IS NOT NULL`

---

## API Response Examples

### Habits Endpoint Sample
```json
{
  "data": [
    {
      "sync_id": 123,
      "user_id": "user123",
      "sync_date": "2025-11-23",
      "habit_name": "No of prayers at mosque",
      "category": "Prayer",
      "priority": 1,
      "priority_label": "Critical",
      "score": 85.5,
      "grade": "B",
      "is_numerical": true,
      "habit_type": "Numerical",
      "record_date": "2025-11-22",
      "value": 4,
      "completed": true,
      "day_of_week": "Friday"
    }
  ],
  "metadata": {
    "total_records": 750,
    "endpoint": "habits"
  }
}
```

### Summary Endpoint Sample
```json
{
  "data": [
    {
      "sync_date": "2025-11-23",
      "total_habits": 25,
      "average_score": 78.5,
      "grade_a_count": 8,
      "grade_b_count": 10,
      "grade_c_count": 5
    }
  ]
}
```

---

## Next Steps

1. **Upload API**: Deploy `looker_api.php` to your VPS
2. **Test Endpoints**: Verify each endpoint returns data
3. **Create Data Sources**: Add all 4 data sources to Looker Studio
4. **Build First Dashboard**: Start with Executive Dashboard layout
5. **Share**: Share dashboard with team or keep private

## Support

For issues or questions:
- Check API response: `curl https://kpitracker.quest/api/looker.php?type=habits`
- View server logs: `ssh root@kpitracker.quest "tail -f /var/log/nginx/error.log"`
- Test database connection: Check MySQL credentials and permissions

---

**Happy Analyzing! 📊**
