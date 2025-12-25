#!/bin/bash

# uHabits Mobile Debug Script
# This script captures detailed logs from the mobile app during sync operations

echo "===== uHabits Mobile Sync Debug Tool ====="
echo "This will capture real-time logs from your mobile device during sync operations."
echo ""

# Check if device is connected
echo "Checking for connected Android devices..."
DEVICES=$(adb devices | grep -v "List of devices attached" | grep -v "^$" | wc -l)

if [ "$DEVICES" -eq 0 ]; then
    echo "❌ No Android devices found."
    echo ""
    echo "Please follow these steps:"
    echo "1. Enable Developer Options on your phone:"
    echo "   - Go to Settings > About Phone"
    echo "   - Tap 'Build Number' 7 times"
    echo "2. Enable USB Debugging:"
    echo "   - Go to Settings > Developer Options"
    echo "   - Turn on 'USB Debugging'"
    echo "3. Connect your phone to this computer via USB"
    echo "4. Accept the USB debugging prompt on your phone"
    echo "5. Run this script again"
    exit 1
fi

echo "✓ Found $DEVICES Android device(s) connected"
adb devices

# Create logs directory
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
LOG_DIR="mobile_debug_logs_$TIMESTAMP"
mkdir -p "$LOG_DIR"

echo ""
echo "===== Starting Debug Session ====="
echo "Log files will be saved to: $LOG_DIR"
echo ""

# Function to capture logs
capture_logs() {
    local log_type="$1"
    local filename="$2"
    local filter="$3"
    
    echo "Starting $log_type capture..."
    adb logcat -c  # Clear existing logs
    
    if [ -n "$filter" ]; then
        adb logcat $filter > "$LOG_DIR/$filename" &
    else
        adb logcat > "$LOG_DIR/$filename" &
    fi
    
    echo $! > "$LOG_DIR/$filename.pid"
}

# Function to stop log capture
stop_logs() {
    echo "Stopping log capture..."
    for pidfile in "$LOG_DIR"/*.pid; do
        if [ -f "$pidfile" ]; then
            pid=$(cat "$pidfile")
            kill $pid 2>/dev/null
            rm "$pidfile"
        fi
    done
}

# Trap to cleanup on exit
trap stop_logs EXIT

echo "Setting up log capture filters..."

# Capture different types of logs
capture_logs "uHabits App Logs" "uhabits_app.log" "*:S uHabits:V CloudSync:V CloudSyncManager:V NetworkDiagnostics:V"
capture_logs "Network Logs" "network.log" "*:S NetworkInfo:V ConnectivityManager:V HttpURLConnection:V"
capture_logs "System Network" "system_network.log" "*:S NetworkSecurityConfig:V OkHttp:V"
capture_logs "All System Logs" "full_system.log" ""

sleep 2

echo ""
echo "===== DEBUG SESSION ACTIVE ====="
echo "Now perform the following steps on your mobile device:"
echo ""
echo "1. Open the uHabits app"
echo "2. Go to the menu and find 'Cloud Sync' or sync settings"
echo "3. Try to perform a sync operation"
echo "4. Note any error messages that appear"
echo "5. Press ENTER here when sync attempt is complete"
echo ""
echo "📱 Monitoring logs in real-time..."
echo "📱 You can watch the logs by opening another terminal and running:"
echo "   tail -f $LOG_DIR/uhabits_app.log"
echo ""

# Show real-time uHabits logs
echo "Real-time uHabits logs (press Ctrl+C to stop viewing, then ENTER to finish):"
adb logcat -c
adb logcat *:S uHabits:V CloudSync:V CloudSyncManager:V NetworkDiagnostics:V ListHabitsScreen:V | while read line; do
    echo "[$(date '+%H:%M:%S')] $line"
done &
LOGCAT_PID=$!

# Wait for user to complete sync operation
read -p "Press ENTER when you've completed the sync operation on your phone..."

# Stop real-time viewing
kill $LOGCAT_PID 2>/dev/null

# Stop all log capture
stop_logs

echo ""
echo "===== DEBUG SESSION COMPLETE ====="
echo "Log files saved in: $LOG_DIR"
echo ""
echo "Analyzing captured logs..."

# Analyze the logs
analyze_logs() {
    echo "=== SYNC OPERATION ANALYSIS ==="
    
    # Check for sync-related entries
    if [ -f "$LOG_DIR/uhabits_app.log" ]; then
        echo ""
        echo "🔍 Cloud Sync Manager Activity:"
        grep -i "CloudSyncManager\|performSync\|uploadToCloud\|NetworkError" "$LOG_DIR/uhabits_app.log" | tail -20
        
        echo ""
        echo "🔍 Network Connection Attempts:"
        grep -i "connect\|network\|http\|api" "$LOG_DIR/uhabits_app.log" | tail -15
        
        echo ""
        echo "🔍 Error Messages:"
        grep -i "error\|exception\|failed\|timeout" "$LOG_DIR/uhabits_app.log" | tail -10
    fi
    
    if [ -f "$LOG_DIR/network.log" ]; then
        echo ""
        echo "🔍 Network System Activity:"
        grep -i "connect\|http\|network" "$LOG_DIR/network.log" | tail -10
    fi
}

analyze_logs

echo ""
echo "===== RECOMMENDATIONS ====="
echo "1. Check the log files in $LOG_DIR for detailed information"
echo "2. Look for specific error patterns in uhabits_app.log"
echo "3. If you see network errors, check network.log for system-level issues"
echo ""
echo "To share these logs for analysis:"
echo "   tar -czf mobile_debug_$TIMESTAMP.tar.gz $LOG_DIR"
echo ""
echo "Debug session complete!"
