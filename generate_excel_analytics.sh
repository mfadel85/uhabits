#!/bin/bash

# Excel Analysis Generator for uHabits Analytics
# Generates Excel-ready CSV files with formulas and pivot table suggestions

echo "🔍 Generating Excel-ready analytics files..."

# Set up AWS and file paths
AWS_REGION="eu-central-1"
TABLE_NAME="uHabits-Analytics-prod"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
OUTPUT_DIR="excel_analytics_${TIMESTAMP}"

# Create output directory
mkdir -p "$OUTPUT_DIR"

echo "📊 Exporting data for Excel analysis..."

# Export all data to JSON first
aws dynamodb scan \
    --table-name "$TABLE_NAME" \
    --region "$AWS_REGION" \
    --output json > "$OUTPUT_DIR/raw_data.json"

# Create Python script to generate Excel-friendly CSVs
cat > "$OUTPUT_DIR/generate_excel_files.py" << 'EOF'
import json
import csv
import sys
from datetime import datetime, timedelta
from collections import defaultdict

def load_data():
    """Load and parse DynamoDB data"""
    with open('raw_data.json', 'r') as f:
        data = json.load(f)
    return data['Items']

def safe_get(item, key, default=''):
    """Safely get value from DynamoDB item"""
    if key in item and 'S' in item[key]:
        return item[key]['S']
    elif key in item and 'N' in item[key]:
        return float(item[key]['N'])
    return default

def generate_habits_master():
    """Generate master habits list with Excel formulas"""
    items = load_data()
    habits = [item for item in items if safe_get(item, 'SK', '').startswith('HABIT#')]
    
    with open('01_habits_master.csv', 'w', newline='', encoding='utf-8') as f:
        writer = csv.writer(f)
        
        # Headers with Excel formula suggestions
        writer.writerow([
            'Habit_ID', 'Habit_Name', 'Priority', 'Success_Rate', 
            'Current_Streak', 'Best_Streak', 'Total_Completions',
            'Priority_Weight', 'Weighted_Score', 'Performance_Category',
            'Excel_Formula_Weighted_Score', 'Excel_Formula_Category'
        ])
        
        for habit in habits:
            habit_id = safe_get(habit, 'SK', '').replace('HABIT#', '')
            habit_name = safe_get(habit, 'habit_name')
            priority = safe_get(habit, 'priority')
            success_rate = safe_get(habit, 'success_rate', 0)
            current_streak = safe_get(habit, 'current_streak', 0)
            best_streak = safe_get(habit, 'best_streak', 0)
            total_completions = safe_get(habit, 'total_completions', 0)
            
            # Priority weights
            weight_map = {'CRITICAL': 4.0, 'HIGH': 2.5, 'NORMAL': 1.0, 'LOW': 0.5}
            priority_weight = weight_map.get(priority, 1.0)
            weighted_score = success_rate * priority_weight
            
            # Performance category
            if success_rate >= 0.8:
                category = 'Excellent'
            elif success_rate >= 0.6:
                category = 'Good'
            elif success_rate >= 0.4:
                category = 'Needs Work'
            else:
                category = 'Critical'
            
            # Excel formulas (as text for copy-paste)
            formula_weighted = f'=D{writer.line_num+1}*H{writer.line_num+1}'
            formula_category = f'=IF(D{writer.line_num+1}>=0.8,"Excellent",IF(D{writer.line_num+1}>=0.6,"Good",IF(D{writer.line_num+1}>=0.4,"Needs Work","Critical")))'
            
            writer.writerow([
                habit_id, habit_name, priority, success_rate,
                current_streak, best_streak, total_completions,
                priority_weight, weighted_score, category,
                formula_weighted, formula_category
            ])

def generate_daily_performance():
    """Generate daily performance data"""
    items = load_data()
    daily_records = [item for item in items if safe_get(item, 'SK', '').startswith('DAILY#')]
    
    with open('02_daily_performance.csv', 'w', newline='', encoding='utf-8') as f:
        writer = csv.writer(f)
        writer.writerow([
            'Date', 'Habit_ID', 'Habit_Name', 'Priority', 'Completed',
            'Success_Rate', 'Streak', 'Month', 'Week_Number', 'Day_of_Week'
        ])
        
        for record in daily_records:
            # Parse date from SK (format: DAILY#YYYY-MM-DD#HABIT_ID)
            sk = safe_get(record, 'SK', '')
            try:
                parts = sk.split('#')
                date_str = parts[1]
                habit_id = parts[2] if len(parts) > 2 else ''
                
                # Convert date for Excel analysis
                date_obj = datetime.strptime(date_str, '%Y-%m-%d')
                month = date_obj.strftime('%Y-%m')
                week_num = date_obj.isocalendar()[1]
                day_of_week = date_obj.strftime('%A')
                
                writer.writerow([
                    date_str, habit_id, safe_get(record, 'habit_name'),
                    safe_get(record, 'priority'), safe_get(record, 'completed', 0),
                    safe_get(record, 'success_rate', 0), safe_get(record, 'current_streak', 0),
                    month, week_num, day_of_week
                ])
            except:
                continue

def generate_pivot_suggestions():
    """Generate pivot table suggestions for Excel"""
    with open('03_excel_pivot_suggestions.txt', 'w') as f:
        f.write("""
EXCEL PIVOT TABLE SUGGESTIONS FOR uHABITS ANALYTICS
================================================

📊 PIVOT TABLE 1: Success Rate by Priority
Data Source: 01_habits_master.csv
Rows: Priority
Values: Average of Success_Rate, Count of Habit_ID
Filters: Performance_Category

📈 PIVOT TABLE 2: Daily Performance Trends  
Data Source: 02_daily_performance.csv
Rows: Month, Week_Number
Columns: Priority
Values: Average of Success_Rate

🔥 PIVOT TABLE 3: Habit Performance Matrix
Data Source: 01_habits_master.csv
Rows: Habit_Name
Columns: Performance_Category
Values: Success_Rate, Current_Streak

📅 PIVOT TABLE 4: Weekly Patterns
Data Source: 02_daily_performance.csv
Rows: Day_of_Week
Columns: Priority
Values: Count of Completed

🎯 EXCEL FORMULAS TO ADD:

Goal Achievement:
=COUNTIFS(D:D,">=0.8")/COUNTA(D:D)

Priority Weighted Average:
=SUMPRODUCT(D:D,H:H)/SUM(H:H)

Improvement Rate:
=SLOPE(success_rates, dates)

Consistency Score:
=1-STDEV(success_rates)/AVERAGE(success_rates)

📋 CONDITIONAL FORMATTING RULES:

Success Rate Colors:
- Green: >=80%
- Yellow: 50-79%  
- Red: <50%

Priority Icons:
- 🔴 Critical
- 🟡 High
- 🟢 Normal
- ⚪ Low

📊 RECOMMENDED CHARTS:

1. Success Rate by Priority (Column Chart)
2. Daily Completion Trends (Line Chart)
3. Habit Distribution (Pie Chart)
4. Performance Heatmap (Conditional Formatting)
5. Streak Analysis (Histogram)

🔄 DATA REFRESH:
1. Replace CSV files with new exports
2. Refresh pivot tables (Data > Refresh All)
3. Update chart ranges if needed
""")

def generate_analysis_templates():
    """Generate Excel formula templates"""
    with open('04_excel_formulas.txt', 'w') as f:
        f.write("""
EXCEL ANALYSIS FORMULAS FOR uHABITS
==================================

📊 KEY PERFORMANCE INDICATORS (KPIs):

Overall Success Rate:
=AVERAGE(01_habits_master[Success_Rate])

Weighted Performance Score:  
=SUMPRODUCT(01_habits_master[Success_Rate],01_habits_master[Priority_Weight])/SUM(01_habits_master[Priority_Weight])

Top Performer Count:
=COUNTIF(01_habits_master[Success_Rate],">=0.8")

Critical Habits Needing Attention:
=COUNTIFS(01_habits_master[Priority],"CRITICAL",01_habits_master[Success_Rate],"<0.6")

Average Streak:
=AVERAGE(01_habits_master[Current_Streak])

📈 TREND ANALYSIS:

Monthly Improvement:
=SLOPE(monthly_success_rates, month_numbers)

Weekly Consistency:
=1-STDEV(weekly_completion_rates)/AVERAGE(weekly_completion_rates)

Best Day of Week:
=INDEX(days, MATCH(MAX(daily_averages), daily_averages, 0))

🎯 GOAL TRACKING:

Progress to 80% Success Rate:
=COUNTIF(01_habits_master[Success_Rate],">=0.8")/COUNTA(01_habits_master[Success_Rate])

Critical Habits Success:
=AVERAGEIF(01_habits_master[Priority],"CRITICAL",01_habits_master[Success_Rate])

Improvement Needed:
=COUNTIF(01_habits_master[Success_Rate],"<0.6")

📊 CONDITIONAL FORMATTING:

Success Rate Traffic Light:
- Formula: =D2>=0.8 (Green)
- Formula: =AND(D2>=0.5,D2<0.8) (Yellow)  
- Formula: =D2<0.5 (Red)

Priority Color Coding:
- Formula: =C2="CRITICAL" (Dark Red)
- Formula: =C2="HIGH" (Orange)
- Formula: =C2="NORMAL" (Green)
- Formula: =C2="LOW" (Gray)

Streak Heat Map:
- Formula: =E2>=30 (Dark Green)
- Formula: =AND(E2>=14,E2<30) (Light Green)
- Formula: =AND(E2>=7,E2<14) (Yellow)
- Formula: =E2<7 (Red)
""")

if __name__ == "__main__":
    print("🔍 Processing uHabits data for Excel analysis...")
    
    generate_habits_master()
    print("✅ Generated habits master list")
    
    generate_daily_performance()  
    print("✅ Generated daily performance data")
    
    generate_pivot_suggestions()
    print("✅ Generated pivot table suggestions")
    
    generate_analysis_templates()
    print("✅ Generated Excel formula templates")
    
    print("\n🎉 Excel analysis files ready!")
    print("📁 Open the CSV files in Excel and follow the suggestions")
EOF

# Run the Python script
echo "🐍 Generating Excel-ready files..."
cd "$OUTPUT_DIR"
python3 generate_excel_files.py

echo ""
echo "✅ Excel analysis files generated in: $OUTPUT_DIR"
echo ""
echo "📋 Files created:"
echo "  📊 01_habits_master.csv - Main habits data with Excel formulas"
echo "  📈 02_daily_performance.csv - Daily tracking data"  
echo "  📋 03_excel_pivot_suggestions.txt - Pivot table ideas"
echo "  🔧 04_excel_formulas.txt - Ready-to-use Excel formulas"
echo ""
echo "🚀 Next steps:"
echo "  1. Open Excel or Google Sheets"
echo "  2. Import the CSV files"
echo "  3. Follow the pivot table suggestions"
echo "  4. Apply the conditional formatting rules"
echo "  5. Create charts from the templates"
echo ""
echo "💡 Pro tip: Enable 'Format as Table' in Excel for better filtering!"
