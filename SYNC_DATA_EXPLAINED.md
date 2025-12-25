# 📊 uHabits Data Sync Explained

## What Data Is Synced When You Click "Sync"

When you tap the sync button in the uHabits mobile app, the following data is sent to AWS:

### 1. User Information
- **user_id**: Your unique user identifier (usually "user_primary")
- **sync_timestamp**: Current time in milliseconds (e.g., 1672531200000)
- **sync_date**: Current date in YYYY-MM-DD format

### 2. Summary Metrics
```json
"summary_metrics": {
  "total_habits": 12,
  "active_habits": 10,
  "weighted_success_rate": 0.85
}
```
- Overall statistics about your habits
- Success rates and active habit counts
- Weighted performance metrics

### 3. Priority Distribution
```json
"priority_distribution": {
  "CRITICAL": 3,
  "HIGH": 4,
  "NORMAL": 2,
  "LOW": 1
}
```
- Count of habits by priority level
- Used for dashboard visualizations

### 4. Device Information
- Details about your mobile device
- Operating system version
- App version

### 5. Metadata
- Any additional sync-related information
- Configuration settings

### 6. Detailed Habit Data
For **each habit** in your app, the following is synced:

```json
{
  "id": "habit123",
  "name": "Morning Prayer",
  "category": "Religious",  // Standardized during sync
  "priority": "CRITICAL",
  "weight": 4.0,
  "success_rate": 0.92,
  "weighted_success_rate": 3.68,
  "streak_length": 14,
  "is_numerical": false,
  "target_value": null,
  "frequency": "DAILY",
  "color": "#4CAF50",
  "type": "yes_no",
  
  "performance_history": {
    "daily_data": [...],
    "weekly_data": [...],
    "monthly_data": [...],
    "streak_history": [...]
  }
}
```

### 7. Performance History
For each habit, detailed performance history is synced:

#### Daily Performance
```json
"daily_data": [
  {
    "date": "2025-09-26",
    "completed": true,
    "value": null,
    "notes": "Completed early",
    "day_of_week": 5
  },
  ...
]
```
- Individual day-by-day tracking data
- Completion status for each day
- Optional notes and values

#### Weekly Aggregations
```json
"weekly_data": [
  {
    "week_start": "2025-09-21",
    "week_end": "2025-09-27",
    "completed_days": 6,
    "expected_days": 7,
    "completion_rate": 0.857,
    "total_value": null
  },
  ...
]
```
- Weekly summaries of your habit performance
- Aggregated statistics by week

#### Monthly Aggregations
```json
"monthly_data": [
  {
    "month": "2025-09",
    "completed_days": 25,
    "expected_days": 30,
    "completion_rate": 0.833,
    "total_value": null
  },
  ...
]
```
- Monthly summaries of your habit performance
- Aggregated statistics by month

#### Streak History
```json
"streak_history": [
  {
    "start_date": "2025-09-01",
    "end_date": "2025-09-14",
    "length": 14
  },
  ...
]
```
- Historical records of habit streaks
- Start and end dates
- Streak lengths

## ✅ Category Standardization

When your habit data is synced, the categories are automatically standardized into four groups:

1. **Religious** - Habits containing "religious", "pray", or "faith"
2. **Career & Work** - Habits containing "career", "work", or "job"
3. **Social & Family** - Habits containing "social", "family", or "friend"
4. **Personal Improvement** - Habits containing "personal", "health", "self", or any unmatched category

## 📋 Where Data Is Stored

All this data is stored in the DynamoDB table `uHabits-Analytics-prod` with the following structure:

1. **Sync Summary Record**:
   - Key: `USER#<user_id>` / `SYNC#<timestamp>`
   - Contains overall sync metadata

2. **Habit Records**:
   - Key: `USER#<user_id>` / `HABIT#<timestamp>#<habit_id>`
   - Contains main habit details

3. **Daily Performance Records**:
   - Key: `USER#<user_id>` / `DAILY#<habit_id>#<date>`
   - Contains day-by-day tracking

4. **Weekly Aggregation Records**:
   - Key: `USER#<user_id>` / `WEEKLY#<habit_id>#<week_start>`
   - Contains weekly summaries

5. **Monthly Aggregation Records**:
   - Key: `USER#<user_id>` / `MONTHLY#<habit_id>#<month>`
   - Contains monthly summaries

6. **Streak History Records**:
   - Key: `USER#<user_id>` / `STREAK#<habit_id>#<start_date>#<index>`
   - Contains streak tracking information

All records include your standardized habit categories for consistent analytics.

## 🔄 Sync Process Flow

1. You click "Sync" in the mobile app
2. App collects all habit data including performance history
3. Data is sent to AWS Lambda function
4. Lambda processes and standardizes the data
5. Data is stored in DynamoDB with appropriate structure
6. Success response is sent back to the app
7. Your dashboard can now show the latest analytics with correct categories
