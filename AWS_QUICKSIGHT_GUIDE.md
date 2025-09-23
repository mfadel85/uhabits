# AWS QuickSight Live Dashboard Setup Guide

## 🎯 Why AWS QuickSight?
- **Native DynamoDB integration** - No exports needed!
- **Auto-refresh** dashboards (hourly/daily)
- **Pay-per-session** pricing ($5/month for first user)
- **Mobile app** available
- **Share dashboards** easily

## 📊 Setup Steps

### Step 1: Enable QuickSight
```bash
# Check if QuickSight is available in your region
aws quicksight describe-account-settings --aws-account-id $(aws sts get-caller-identity --query Account --output text) --region eu-central-1
```

### Step 2: Create DynamoDB Data Source
1. Go to [AWS QuickSight Console](https://eu-central-1.quicksight.aws.amazon.com)
2. Click "Datasets" → "New dataset"
3. Choose "DynamoDB"
4. Select table: `uHabits-Analytics-prod`
5. Import mode: "SPICE" (for faster queries)

### Step 3: Data Preparation
Since DynamoDB is NoSQL, we need to structure the data:

**Create Analysis Views:**
- **Habits View**: Filter records where `SK` starts with `HABIT#`
- **Daily View**: Filter records where `SK` starts with `DAILY#`
- **Weekly View**: Filter records where `SK` starts with `WEEKLY#`

### Step 4: Build Dashboard

#### Key Visualizations:
- **KPI Cards**: Total habits, average success rate, critical habits count
- **Priority Bar Chart**: Success rate by priority level
- **Timeline**: Performance trends over time
- **Heatmap**: Daily completion patterns
- **Table**: Top/bottom performing habits

### Step 5: Schedule Auto-Refresh
- Set refresh: Every 6 hours
- SPICE capacity: Import latest data automatically
- Email alerts: When data refreshes

## 🔧 QuickSight Calculated Fields

**Weighted Score:**
```sql
CASE 
  WHEN {priority} = 'CRITICAL' THEN {success_rate} * 4.0
  WHEN {priority} = 'HIGH' THEN {success_rate} * 2.5
  WHEN {priority} = 'NORMAL' THEN {success_rate} * 1.0
  WHEN {priority} = 'LOW' THEN {success_rate} * 0.5
  ELSE {success_rate}
END
```

**Performance Category:**
```sql
CASE 
  WHEN {success_rate} >= 0.8 THEN 'Excellent'
  WHEN {success_rate} >= 0.6 THEN 'Good'
  WHEN {success_rate} >= 0.4 THEN 'Needs Work'
  ELSE 'Critical'
END
```

**Date Extraction (from SK):**
```sql
split({SK}, '#', 2)
```

## 💰 Pricing
- **Author**: $18/month (can create dashboards)
- **Reader**: $0.30/session (view-only)
- **SPICE**: $0.25/GB/month
- **First user**: $5/month special rate

## 🚀 Advanced Features
- **ML Insights**: Automatic anomaly detection
- **Forecasting**: Predict future performance
- **Drill-down**: Click to see details
- **Mobile responsive**: Auto-adapts to phone
- **Embedding**: Put in web apps

---

## 📱 Mobile Dashboard Access
1. Download AWS QuickSight mobile app
2. Login with your AWS credentials
3. Access dashboards anywhere
4. Get push notifications for insights
