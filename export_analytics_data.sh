#!/bin/bash

# uHabits Data Export Script
# Exports DynamoDB data in various formats for analytics consumption

set -e

echo "🚀 uHabits Analytics Data Export"
echo "=================================="

# Configuration
TABLE_NAME="uHabits-Analytics-prod"
REGION="eu-central-1"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
OUTPUT_DIR="exports"

# Create output directory
mkdir -p "$OUTPUT_DIR"

echo "📊 Exporting from DynamoDB table: $TABLE_NAME"
echo "🌍 Region: $REGION"
echo "📅 Timestamp: $TIMESTAMP"

# Export 1: Full JSON export
echo ""
echo "1️⃣ Exporting full data to JSON..."
aws dynamodb scan \
    --table-name "$TABLE_NAME" \
    --region "$REGION" \
    --output json > "$OUTPUT_DIR/uhabits_full_export_$TIMESTAMP.json"

echo "✅ Full export: $OUTPUT_DIR/uhabits_full_export_$TIMESTAMP.json"

# Export 2: Habits summary (PowerBI ready)
echo ""
echo "2️⃣ Creating PowerBI-ready habits summary..."
aws dynamodb scan \
    --table-name "$TABLE_NAME" \
    --region "$REGION" \
    --filter-expression "begins_with(SK, :sk_prefix)" \
    --expression-attribute-values '{":sk_prefix":{"S":"HABIT#"}}' \
    --projection-expression "PK,SK,habit_name,priority,success_rate,weighted_success_rate,streak_length,priority_weight,color_code,is_active,target_value" \
    --output json | jq -r '
        .Items[] | 
        [
            .PK.S // "",
            .SK.S // "",
            .habit_name.S // "",
            .priority.S // "",
            (.success_rate.N // "0" | tonumber),
            (.weighted_success_rate.N // "0" | tonumber),
            (.streak_length.N // "0" | tonumber),
            (.priority_weight.N // "1" | tonumber),
            .color_code.S // "",
            (.is_active.BOOL // true),
            (.target_value.N // "1" | tonumber)
        ] | @csv
    ' > "$OUTPUT_DIR/habits_summary_$TIMESTAMP.csv"

# Add CSV header
sed -i '1i\PK,SK,HabitName,Priority,SuccessRate,WeightedSuccessRate,StreakLength,PriorityWeight,ColorCode,IsActive,TargetValue' "$OUTPUT_DIR/habits_summary_$TIMESTAMP.csv"

echo "✅ Habits summary: $OUTPUT_DIR/habits_summary_$TIMESTAMP.csv"

# Export 3: Daily performance data
echo ""
echo "3️⃣ Creating daily performance export..."
aws dynamodb scan \
    --table-name "$TABLE_NAME" \
    --region "$REGION" \
    --filter-expression "begins_with(SK, :sk_prefix)" \
    --expression-attribute-values '{":sk_prefix":{"S":"DAILY#"}}' \
    --output json | jq -r '
        .Items[] | 
        [
            .PK.S // "",
            .SK.S // "",
            .date.S // "",
            (.completion_rate.N // "0" | tonumber),
            (.weighted_completion_rate.N // "0" | tonumber),
            (.priority_score.N // "0" | tonumber)
        ] | @csv
    ' > "$OUTPUT_DIR/daily_performance_$TIMESTAMP.csv"

# Add CSV header
sed -i '1i\UserID,SK,Date,CompletionRate,WeightedCompletionRate,PriorityScore' "$OUTPUT_DIR/daily_performance_$TIMESTAMP.csv"

echo "✅ Daily performance: $OUTPUT_DIR/daily_performance_$TIMESTAMP.csv"

# Export 4: Weekly aggregations
echo ""
echo "4️⃣ Creating weekly aggregations export..."
aws dynamodb scan \
    --table-name "$TABLE_NAME" \
    --region "$REGION" \
    --filter-expression "begins_with(SK, :sk_prefix)" \
    --expression-attribute-values '{":sk_prefix":{"S":"WEEKLY#"}}' \
    --output json | jq -r '
        .Items[] | 
        [
            .PK.S // "",
            .SK.S // "",
            .week.S // "",
            (.completion_rate.N // "0" | tonumber),
            (.weighted_completion_rate.N // "0" | tonumber),
            (.priority_score.N // "0" | tonumber)
        ] | @csv
    ' > "$OUTPUT_DIR/weekly_performance_$TIMESTAMP.csv"

# Add CSV header
sed -i '1i\UserID,SK,Week,CompletionRate,WeightedCompletionRate,PriorityScore' "$OUTPUT_DIR/weekly_performance_$TIMESTAMP.csv"

echo "✅ Weekly performance: $OUTPUT_DIR/weekly_performance_$TIMESTAMP.csv"

# Summary
echo ""
echo "🎉 Export Complete!"
echo "==================="
echo "📁 All files exported to: $OUTPUT_DIR/"
echo ""
echo "📊 Files created:"
ls -lh "$OUTPUT_DIR/"*"$TIMESTAMP"*
echo ""
echo "💡 Next steps:"
echo "   • PowerBI: Import the CSV files"
echo "   • Python: Use the JSON file"
echo "   • Excel: Open CSV files directly"
echo "   • Looker Studio: Upload CSV to Google Drive"
echo ""
echo "🔄 Re-run this script anytime to get fresh data!"
