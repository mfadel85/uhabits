#!/bin/bash

# uHabits Analytics Cache Management Script
# This script helps manage local data caching to reduce AWS API calls

CACHE_DIR="$HOME/.uhabits_cache"
API_ENDPOINT="https://bhg1kt9cf2.execute-api.eu-central-1.amazonaws.com/prod/api/groups"

# Create cache directory if it doesn't exist
mkdir -p "$CACHE_DIR"

show_help() {
    echo "🔧 uHabits Analytics Cache Manager"
    echo ""
    echo "Usage: $0 [COMMAND]"
    echo ""
    echo "Commands:"
    echo "  refresh    Fetch fresh data from AWS and cache it"
    echo "  status     Show cache status and age"
    echo "  clear      Clear all cached data"
    echo "  view       View cached data summary"
    echo "  help       Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0 refresh    # Update cache with fresh AWS data"
    echo "  $0 status     # Check when cache was last updated"
    echo "  $0 clear      # Clear cache to force fresh API calls"
}

refresh_cache() {
    echo "🔄 Fetching fresh data from AWS API..."
    
    TEMP_FILE="$CACHE_DIR/temp_data.json"
    CACHE_FILE="$CACHE_DIR/analytics_data.json"
    
    # Fetch data from API
    if curl -s "$API_ENDPOINT" -o "$TEMP_FILE"; then
        # Check if the response is valid JSON
        if jq empty "$TEMP_FILE" 2>/dev/null; then
            # Move temp file to cache and add timestamp
            jq '. + {"cached_at": "'$(date -Iseconds)'"}' "$TEMP_FILE" > "$CACHE_FILE"
            rm "$TEMP_FILE"
            
            # Extract summary info
            DATA_SOURCE=$(jq -r '.summary.data_source' "$CACHE_FILE")
            HABITS_COUNT=$(jq -r '.summary.total_habits_analyzed' "$CACHE_FILE")
            GROUPS_COUNT=$(jq -r '.summary.total_groups' "$CACHE_FILE")
            
            echo "✅ Cache updated successfully!"
            echo "📊 Data source: $DATA_SOURCE"
            echo "📈 $HABITS_COUNT habits across $GROUPS_COUNT categories"
            echo "🕒 Cached at: $(date)"
        else
            echo "❌ Invalid JSON response from API"
            rm "$TEMP_FILE" 2>/dev/null
            exit 1
        fi
    else
        echo "❌ Failed to fetch data from API"
        rm "$TEMP_FILE" 2>/dev/null
        exit 1
    fi
}

show_status() {
    CACHE_FILE="$CACHE_DIR/analytics_data.json"
    
    if [ -f "$CACHE_FILE" ]; then
        CACHED_AT=$(jq -r '.cached_at // empty' "$CACHE_FILE")
        DATA_SOURCE=$(jq -r '.summary.data_source' "$CACHE_FILE")
        HABITS_COUNT=$(jq -r '.summary.total_habits_analyzed' "$CACHE_FILE")
        GROUPS_COUNT=$(jq -r '.summary.total_groups' "$CACHE_FILE")
        
        echo "📱 Cache Status"
        echo "==============="
        echo "File: $CACHE_FILE"
        echo "Cached at: ${CACHED_AT:-Unknown}"
        echo "Data source: $DATA_SOURCE"
        echo "Habits: $HABITS_COUNT across $GROUPS_COUNT categories"
        
        # Calculate age
        if [ -n "$CACHED_AT" ]; then
            CACHE_TIMESTAMP=$(date -d "$CACHED_AT" +%s 2>/dev/null || echo "0")
            CURRENT_TIMESTAMP=$(date +%s)
            AGE_SECONDS=$((CURRENT_TIMESTAMP - CACHE_TIMESTAMP))
            AGE_MINUTES=$((AGE_SECONDS / 60))
            
            if [ $AGE_MINUTES -lt 60 ]; then
                echo "Age: $AGE_MINUTES minutes old"
            else
                AGE_HOURS=$((AGE_MINUTES / 60))
                if [ $AGE_HOURS -lt 24 ]; then
                    echo "Age: $AGE_HOURS hours old"
                else
                    AGE_DAYS=$((AGE_HOURS / 24))
                    echo "Age: $AGE_DAYS days old"
                fi
            fi
            
            # Recommend refresh if old
            if [ $AGE_MINUTES -gt 30 ]; then
                echo "💡 Consider running '$0 refresh' to update cache"
            fi
        fi
        
        echo "File size: $(du -h "$CACHE_FILE" | cut -f1)"
    else
        echo "❌ No cache file found"
        echo "💡 Run '$0 refresh' to create cache"
    fi
}

clear_cache() {
    CACHE_FILE="$CACHE_DIR/analytics_data.json"
    
    if [ -f "$CACHE_FILE" ]; then
        rm "$CACHE_FILE"
        echo "🗑️  Cache cleared successfully"
        echo "💡 Next dashboard load will fetch fresh data from AWS"
    else
        echo "ℹ️  No cache to clear"
    fi
}

view_cache() {
    CACHE_FILE="$CACHE_DIR/analytics_data.json"
    
    if [ -f "$CACHE_FILE" ]; then
        echo "📊 Cached Data Summary"
        echo "====================="
        
        # Show categories and their performance
        jq -r '.group_performance | to_entries[] | "\(.key): \(.value.habit_count) habits, \(.value.grade) grade, \(.value.weighted_average * 100 | round)% success"' "$CACHE_FILE"
        
        echo ""
        echo "🔍 Full data available at: $CACHE_FILE"
        echo "💡 Use 'jq . $CACHE_FILE' to view complete JSON"
    else
        echo "❌ No cache file found"
        echo "💡 Run '$0 refresh' to create cache"
    fi
}

# Main script logic
case "${1:-help}" in
    "refresh")
        refresh_cache
        ;;
    "status")
        show_status
        ;;
    "clear")
        clear_cache
        ;;
    "view")
        view_cache
        ;;
    "help"|"--help"|"-h")
        show_help
        ;;
    *)
        echo "❌ Unknown command: $1"
        echo ""
        show_help
        exit 1
        ;;
esac