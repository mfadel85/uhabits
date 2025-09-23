# PowerBI Pro Integration Guide for uHabits Analytics

## 🎯 Overview

This guide walks you through connecting your PowerBI Pro license to the uHabits DynamoDB data source for advanced analytics and visualization.

## 📋 Prerequisites

- ✅ PowerBI Pro license (you already have this)
- ✅ AWS account with deployed uHabits Lambda infrastructure
- ✅ DynamoDB table populated with habit data
- ✅ AWS credentials configured for PowerBI access

## 🔧 Step 1: AWS IAM Setup

### Create PowerBI IAM Role
```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "dynamodb:Query",
                "dynamodb:GetItem",
                "dynamodb:BatchGetItem",
                "dynamodb:Scan"
            ],
            "Resource": [
                "arn:aws:dynamodb:*:*:table/uHabits-Analytics-*",
                "arn:aws:dynamodb:*:*:table/uHabits-Analytics-*/index/*"
            ]
        }
    ]
}
```

### Create Access Key for PowerBI
1. Create IAM user: `powerbi-uhabits-readonly`
2. Attach the above policy
3. Generate Access Key + Secret Key
4. Save credentials securely

## 📊 Step 2: PowerBI Desktop Setup

### Install Required Components
1. **PowerBI Desktop** (free download)
2. **AWS DynamoDB Connector** (if not built-in)
3. **AWS CLI** (for credential management)

### Configure AWS Credentials
```bash
aws configure set aws_access_key_id YOUR_ACCESS_KEY
aws configure set aws_secret_access_key YOUR_SECRET_KEY
aws configure set default.region us-east-1
aws configure set default.output json
```

## 🔌 Step 3: DynamoDB Connection

### Connect to DynamoDB
1. Open PowerBI Desktop
2. **Get Data** → **More** → **Amazon DynamoDB**
3. Enter connection details:
   - **Region**: us-east-1 (or your deployment region)
   - **Access Key**: Your PowerBI IAM access key
   - **Secret Key**: Your PowerBI IAM secret key

### Select Tables
- Choose: `uHabits-Analytics-prod`
- Preview data to ensure connection works

## 🛠️ Step 4: Data Transformation

### Power Query Transformations
```m
// Parse the JSON metadata columns
= Table.ExpandRecordColumn(
    Source, 
    "summary_metrics", 
    {"total_habits", "active_habits", "weighted_success_rate"}
)

// Convert timestamp to readable date
= Table.AddColumn(
    #"Expanded Metadata", 
    "SyncDate", 
    each DateTime.FromUnixTime([sync_timestamp]/1000)
)

// Filter for habit data only
= Table.SelectRows(
    Source, 
    each Text.StartsWith([SK], "HABIT#")
)
```

### Data Model Setup
1. **Fact Table**: Individual habit records
2. **Dimension Tables**: 
   - Habits (name, priority, type)
   - Time (date, week, month)
   - Priority levels (CRITICAL, HIGH, NORMAL, LOW)

## 📈 Step 5: Dashboard Creation

### Key Visualizations

#### 1. Priority Distribution (Pie Chart)
- **Values**: Count of habits
- **Legend**: Priority level
- **Colors**: Red (CRITICAL), Orange (HIGH), Blue (NORMAL), Gray (LOW)

#### 2. Weighted Success Rate Timeline (Line Chart)
- **Axis**: sync_date
- **Values**: weighted_success_rate
- **Legend**: priority (multiple lines)

#### 3. Habit Performance Matrix (Table)
```
Habit Name | Priority | Weight | Success Rate | Weighted Score | Trend
Morning Quran | CRITICAL | 4.0x | 92% | 3.68 | ↗️
Exercise | HIGH | 2.5x | 78% | 1.95 | ↗️
TV Time | LOW | 0.5x | 45% | 0.23 | ↘️
```

#### 4. Daily Performance Gauge
- **Value**: Today's weighted success rate
- **Target**: 3.0 (Good day threshold)
- **Colors**: Green (>3.0), Yellow (2.0-3.0), Red (<2.0)

#### 5. Streak Length Distribution (Histogram)
- **X-axis**: Streak length bins (1-7, 8-14, 15-30, 30+)
- **Y-axis**: Count of habits
- **Color**: By priority level

### Advanced Measures (DAX)
```dax
// Overall Weighted Performance
WeightedPerformance = 
SUMX(
    Habits,
    Habits[success_rate] * Habits[weight]
) / SUM(Habits[weight])

// Priority Score
PriorityScore = 
SWITCH(
    Habits[priority],
    "CRITICAL", 4,
    "HIGH", 2.5,
    "NORMAL", 1,
    "LOW", 0.5,
    1
)

// Monthly Trend
MonthlyTrend = 
VAR CurrentMonth = MAX(Calendar[sync_date])
VAR PreviousMonth = DATEADD(CurrentMonth, -1, MONTH)
RETURN
(
    CALCULATE([WeightedPerformance], Calendar[sync_date] = CurrentMonth) -
    CALCULATE([WeightedPerformance], Calendar[sync_date] = PreviousMonth)
) / CALCULATE([WeightedPerformance], Calendar[sync_date] = PreviousMonth)
```

## 🔄 Step 6: Automatic Refresh

### PowerBI Service Setup
1. **Publish** report to PowerBI Service
2. **Configure Data Source**: Add AWS credentials
3. **Schedule Refresh**: 
   - Frequency: 4 times per day
   - Times: 8AM, 2PM, 8PM, 11PM
   - This covers your typical sync patterns

### Refresh Optimization
- **Incremental Refresh**: Only sync last 7 days
- **Query Folding**: Push filtering to DynamoDB
- **Scheduled Refresh**: Outside peak AWS usage

## 📱 Step 7: Mobile Dashboard

### PowerBI Mobile App
1. **Download**: PowerBI Mobile App
2. **Access**: Your published dashboard
3. **Pin**: Key tiles to phone home screen
4. **Notifications**: Set alerts for low performance days

### Quick Insights on Phone
- Today's weighted score
- Priority distribution
- Top performing habits
- Habits needing attention

## 🎨 Dashboard Design Tips

### Color Scheme
- **CRITICAL**: #DC143C (Crimson)
- **HIGH**: #FF8C00 (Dark Orange)  
- **NORMAL**: #4169E1 (Royal Blue)
- **LOW**: #696969 (Dim Gray)

### Layout Suggestions
```
+------------------+------------------+
|   Priority Pie   |  Performance     |
|     Chart        |     Gauge        |
+------------------+------------------+
|          Habit Performance          |
|             Matrix                  |
+-------------------------------------+
|        Success Rate Timeline        |
+-------------------------------------+
|  Streak Analysis  |   Weekly Goals  |
+------------------+------------------+
```

## 🚀 Advanced Features

### Predictive Analytics
- **Trend Analysis**: Success rate forecasting
- **Habit Recommendations**: Suggest priority adjustments
- **Goal Setting**: Automatic target calculations

### Custom Insights
- **Best Performance Days**: Day-of-week analysis
- **Habit Correlations**: Which habits succeed together
- **Seasonal Patterns**: Monthly/quarterly trends

## 💡 Troubleshooting

### Common Issues
1. **Connection Timeout**: Increase query timeout in PowerBI
2. **Large Dataset**: Implement incremental refresh
3. **Refresh Failures**: Check AWS credentials and permissions
4. **Slow Performance**: Add filters to reduce data volume

### Performance Optimization
- Use **DirectQuery** for real-time data
- **Import Mode** for faster visualizations
- **Composite Model** for best of both worlds

## 📊 Sample Report Template

A complete PowerBI template file (.pbit) would include:
- Pre-built visualizations
- Configured data connections
- Custom measures and calculations
- Mobile-optimized layouts
- Automated refresh settings

This PowerBI integration transforms your habit data into actionable insights, leveraging your existing Pro license for maximum value at zero additional cost.

## 🎯 Next Steps

1. **Deploy AWS Infrastructure**: Use the CloudFormation template
2. **Sync Some Data**: Use the Android app cloud sync feature  
3. **Connect PowerBI**: Follow this guide step-by-step
4. **Create Dashboard**: Start with basic visualizations
5. **Optimize & Iterate**: Add advanced features over time

Your habit tracking just became a professional analytics platform! 🚀
