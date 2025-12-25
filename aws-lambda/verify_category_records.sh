#!/bin/bash

# Direct verification script for category standardization
# Analyzes existing DynamoDB records to check for standardized categories

echo "🔍 Verifying category standardization in DynamoDB records..."

# Check for proper AWS credentials
aws sts get-caller-identity > /dev/null 2>&1
if [ $? -ne 0 ]; then
    echo "❌ AWS credentials not configured correctly. Run 'aws configure' first."
    exit 1
fi

# Function to show records with their categories
show_categories() {
    local record_type=$1
    echo "📋 Checking $record_type records for categories..."
    
    # Get the user IDs first
    USER_IDS=$(aws dynamodb scan \
        --table-name uHabits-Analytics-prod \
        --projection-expression "PK" \
        --filter-expression "begins_with(PK, :pk)" \
        --expression-attribute-values "{\":pk\": {\"S\": \"USER#\"}}" \
        --region eu-central-1 \
        --query "Items[*].PK.S" \
        --output text)
    
    # Initialize counters
    RELIGIOUS=0
    CAREER=0
    SOCIAL=0
    PERSONAL=0
    OTHER=0
    TOTAL_RECORDS=0
    
    # For each user, scan their records
    for user_id in $USER_IDS; do
        echo "Scanning records for $user_id..."
        
        # Scan records of specific type for this user
        RECORDS=$(aws dynamodb scan \
            --table-name uHabits-Analytics-prod \
            --filter-expression "PK = :pk AND begins_with(SK, :sk)" \
            --expression-attribute-values "{\":pk\": {\"S\": \"$user_id\"}, \":sk\": {\"S\": \"$record_type\"}}" \
            --region eu-central-1)
        
        # Display some sample records (max 5)
        echo "Sample $record_type records:"
        echo "$RECORDS" | jq -r '.Items | map({name: .habit_name.S, category: .category.S}) | .[0:5]' 2>/dev/null
        
        # Count categories
        REL_COUNT=$(echo "$RECORDS" | jq -r '.Items | map(select(.category.S == "Religious")) | length' 2>/dev/null || echo 0)
        CAR_COUNT=$(echo "$RECORDS" | jq -r '.Items | map(select(.category.S == "Career & Work")) | length' 2>/dev/null || echo 0)
        SOC_COUNT=$(echo "$RECORDS" | jq -r '.Items | map(select(.category.S == "Social & Family")) | length' 2>/dev/null || echo 0)
        PER_COUNT=$(echo "$RECORDS" | jq -r '.Items | map(select(.category.S == "Personal Improvement")) | length' 2>/dev/null || echo 0)
        
        # Count other non-standard categories
        OTHER_COUNT=$(echo "$RECORDS" | jq -r '.Items | map(select(.category.S != "Religious" and .category.S != "Career & Work" and .category.S != "Social & Family" and .category.S != "Personal Improvement")) | length' 2>/dev/null || echo 0)
        
        # Get total records for this user
        USER_TOTAL=$(echo "$RECORDS" | jq -r '.Items | length' 2>/dev/null || echo 0)
        
        # Add to running totals
        RELIGIOUS=$((RELIGIOUS + REL_COUNT))
        CAREER=$((CAREER + CAR_COUNT))
        SOCIAL=$((SOCIAL + SOC_COUNT))
        PERSONAL=$((PERSONAL + PER_COUNT))
        OTHER=$((OTHER + OTHER_COUNT))
        TOTAL_RECORDS=$((TOTAL_RECORDS + USER_TOTAL))
    done
    
    echo ""
    echo "📊 Category Distribution for $record_type Records:"
    echo "Religious: $RELIGIOUS"
    echo "Career & Work: $CAREER"
    echo "Social & Family: $SOCIAL" 
    echo "Personal Improvement: $PERSONAL"
    echo "Non-standard categories: $OTHER"
    echo ""
    
    # Display results
    echo ""
    echo "📊 Category Distribution for $record_type Records:"
    echo "Religious: $RELIGIOUS"
    echo "Career & Work: $CAREER"
    echo "Social & Family: $SOCIAL" 
    echo "Personal Improvement: $PERSONAL"
    echo "Non-standard categories: $OTHER"
    echo "Total records: $TOTAL_RECORDS"
    
    # Calculate percentage of standardized
    if [ $TOTAL_RECORDS -gt 0 ]; then
        STANDARDIZED=$((RELIGIOUS + CAREER + SOCIAL + PERSONAL))
        PERCENT=$((STANDARDIZED * 100 / TOTAL_RECORDS))
        echo ""
        echo "🔍 $STANDARDIZED out of $TOTAL_RECORDS records ($PERCENT%) have standardized categories"
        
        if [ $OTHER -eq 0 ]; then
            echo "✅ All $record_type records have standardized categories!"
        else
            echo "⚠️ $OTHER records have non-standard categories."
            
            # Non-standard categories already shown in the sample records above
        fi
    else
        echo ""
        echo "❗ No $record_type records found."
    fi
    
    echo "------------------------------------------------------"
}

echo "🔍 Checking category synchronization across record types..."
echo ""

# Check different record types
show_categories "HABIT#"
show_categories "DAILY#" 
show_categories "WEEKLY#"
show_categories "MONTHLY#"
show_categories "STREAK#"

echo "✨ Category verification complete!"

# Summary 
echo ""
echo "📝 Summary of Category Standardization:"
echo "1. The Lambda function is configured to standardize categories to:"
echo "   - Religious"
echo "   - Career & Work"
echo "   - Social & Family" 
echo "   - Personal Improvement"
echo ""
echo "2. This verification checked existing records in DynamoDB."
echo ""
echo "3. If you see non-standard categories, they could be from:"
echo "   a. Records synced before the category standardization was implemented"
echo "   b. Records manually added to DynamoDB"
echo "   c. Sync failures or bypass of the standardization logic"
echo ""
echo "4. Next steps:"
echo "   - Sync your app to upload new data with proper categories"
echo "   - Clear old records if needed using AWS Console"
echo "   - Check your dashboard to confirm categories are displayed correctly"
echo ""
