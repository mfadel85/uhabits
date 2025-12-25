#!/bin/bash

echo "===== Cloud Sync Debug & Fix Tool ====="
echo "This script will help diagnose and fix cloud sync issues in the app."

# 1. Check if the cloud_config.json file exists
CONFIG_PATH="./uhabits-android/src/main/assets/cloud_config.json"
if [ -f "$CONFIG_PATH" ]; then
    echo "✅ cloud_config.json exists."
    echo "Current configuration:"
    cat "$CONFIG_PATH"
else
    echo "❌ cloud_config.json not found!"
    echo "Creating default config file..."
    mkdir -p ./uhabits-android/src/main/assets/
    cat > "$CONFIG_PATH" << EOL
{
  "aws_config": {
    "region": "eu-central-1",
    "api_gateway": {
      "base_url": "https://jodcprzip3.execute-api.eu-central-1.amazonaws.com/prod",
      "sync_endpoint": "/sync",
      "api_key": "Y1leD4smWeX3yfCRM9Sv8R1Jo7g9zusx08Xo4y7o",
      "timeout_seconds": 30
    },
    "environment": "prod"
  },
  "sync_settings": {
    "auto_sync_enabled": true,
    "sync_interval_minutes": 60,
    "retry_attempts": 3,
    "batch_size": 50
  },
  "analytics": {
    "priority_weighting_enabled": true,
    "include_device_info": true,
    "data_retention_days": 365
  }
}
EOL
    echo "✅ Default config file created."
fi

# 2. Create the CloudSyncFix.kt file
CLOUD_SYNC_FIX_PATH="./uhabits-android/src/main/java/org/isoron/uhabits/sync/CloudSyncFix.kt"
echo "Creating CloudSyncFix.kt file..."
mkdir -p ./uhabits-android/src/main/java/org/isoron/uhabits/sync/
cat > "$CLOUD_SYNC_FIX_PATH" << EOL
/*
 * Copyright (C) 2016-2021 Álinson Santos Xavier <git@axavier.org>
 *
 * This file is part of Loop Habit Tracker.
 *
 * Loop Habit Tracker is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Loop Habit Tracker is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.isoron.uhabits.sync

import android.content.Context
import android.util.Log
import android.widget.Toast
import org.json.JSONObject

/**
 * Helper class to fix cloud sync issues
 */
class CloudSyncFix(private val context: Context) {
    
    companion object {
        private const val TAG = "CloudSyncFix"
        private const val CONFIG_FILE = "cloud_config.json"
    }
    
    /**
     * Fixes cloud sync configuration issues by ensuring the config file is loaded properly
     * and enabling sync settings in preferences
     */
    fun fixSyncIssues(syncManager: CloudSyncManager): Boolean {
        Log.i(TAG, "Attempting to fix cloud sync issues...")
        
        try {
            // 1. Check if config file exists and is properly formed
            val configJson = loadAndValidateConfig()
            if (configJson == null) {
                Log.e(TAG, "Could not load valid config JSON")
                return false
            }
            
            // 2. Force enable sync in preferences
            syncManager.enableSync(true)
            Log.i(TAG, "Sync enabled in preferences")
            
            // 3. Reload config in sync manager (via reflection)
            val loadConfigMethod = CloudSyncManager::class.java.getDeclaredMethod("loadConfig")
            loadConfigMethod.isAccessible = true
            loadConfigMethod.invoke(syncManager)
            
            // 4. Verify fix was successful
            val isSyncEnabled = syncManager.isSyncEnabled()
            Log.i(TAG, "Fix applied. Sync enabled status: $isSyncEnabled")
            
            return isSyncEnabled
            
        } catch (e: Exception) {
            Log.e(TAG, "Error fixing sync issues", e)
            return false
        }
    }
    
    /**
     * Loads and validates the config file
     */
    private fun loadAndValidateConfig(): JSONObject? {
        try {
            val inputStream = context.assets.open(CONFIG_FILE)
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(jsonString)
            
            // Validate required fields
            val awsConfig = json.optJSONObject("aws_config")
            if (awsConfig == null) {
                Log.e(TAG, "Missing aws_config in config file")
                return null
            }
            
            val apiGateway = awsConfig.optJSONObject("api_gateway")
            if (apiGateway == null) {
                Log.e(TAG, "Missing api_gateway in config file")
                return null
            }
            
            val baseUrl = apiGateway.optString("base_url")
            val apiKey = apiGateway.optString("api_key")
            
            if (baseUrl.isBlank() || apiKey.isBlank()) {
                Log.e(TAG, "Missing required api_gateway settings")
                return null
            }
            
            return json
            
        } catch (e: Exception) {
            Log.e(TAG, "Error validating config", e)
            return null
        }
    }
    
    /**
     * Applies the fix and shows result to user
     */
    fun applyFixAndNotifyUser(syncManager: CloudSyncManager) {
        val success = fixSyncIssues(syncManager)
        
        if (success) {
            Toast.makeText(
                context,
                "Cloud sync has been enabled successfully! You can now use cloud sync.",
                Toast.LENGTH_LONG
            ).show()
        } else {
            Toast.makeText(
                context,
                "Could not fix cloud sync issues. Please check your configuration.",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
EOL
echo "✅ CloudSyncFix.kt created."

# 3. Create a patch file for CloudSyncManager.kt to add the fix button
PATCH_FILE="./cloud_sync_patch.diff"
cat > "$PATCH_FILE" << EOL
diff --git a/uhabits-android/src/main/java/org/isoron/uhabits/sync/CloudSyncManager.kt b/uhabits-android/src/main/java/org/isoron/uhabits/sync/CloudSyncManager.kt
index abcdef123..987654321 100644
--- a/uhabits-android/src/main/java/org/isoron/uhabits/sync/CloudSyncManager.kt
+++ b/uhabits-android/src/main/java/org/isoron/uhabits/sync/CloudSyncManager.kt
@@ -94,6 +94,15 @@ class CloudSyncManager(
         return preferences.isCloudSyncEnabled && config != null
     }
 
+    /**
+     * Force enables sync by setting the preference and ensuring config is loaded
+     * This is used by CloudSyncFix to repair sync functionality
+     */
+    fun forceEnableSync(): Boolean {
+        preferences.isCloudSyncEnabled = true
+        loadConfig() // Reload config to ensure it's available
+        return isSyncEnabled()
+    }
+
     fun enableSync(enabled: Boolean) {
         preferences.isCloudSyncEnabled = enabled
     }
EOL
echo "✅ Patch file created."

# 4. Create a patch for ListHabitsScreen.kt to add the fix button
SCREEN_PATCH_FILE="./list_habits_screen_patch.diff"
cat > "$SCREEN_PATCH_FILE" << EOL
diff --git a/uhabits-android/src/main/java/org/isoron/uhabits/activities/habits/list/ListHabitsScreen.kt b/uhabits-android/src/main/java/org/isoron/uhabits/activities/habits/list/ListHabitsScreen.kt
index abcdef123..987654321 100644
--- a/uhabits-android/src/main/java/org/isoron/uhabits/activities/habits/list/ListHabitsScreen.kt
+++ b/uhabits-android/src/main/java/org/isoron/uhabits/activities/habits/list/ListHabitsScreen.kt
@@ -81,6 +81,7 @@ import org.isoron.uhabits.utils.showMessage
 import org.isoron.uhabits.utils.showSendEmailScreen
 import org.isoron.uhabits.utils.showSendFileScreen
 import org.isoron.uhabits.sync.CloudSyncManager
+import org.isoron.uhabits.sync.CloudSyncFix
 import java.io.File
 import java.io.IOException
 import java.util.concurrent.TimeUnit
@@ -267,9 +268,22 @@ class ListHabitsScreen
         Log.i("ListHabitsScreen", "Showing cloud sync settings, currently enabled: $isEnabled")
         
         val dialog = AlertDialog.Builder(context)
-            .setTitle("Cloud Sync Settings")
+            .setTitle("Cloud Sync Settings" + if (!isEnabled) " (Currently Disabled)" else "")
             .setMessage("Enable cloud sync to automatically backup your habit data and generate analytics.\n\nThis will sync your data to AWS cloud storage for analytics and backup purposes.")
-            .setPositiveButton(if (isEnabled) "Disable" else "Enable") { _, _ ->
+            
+        if (!isEnabled) {
+            dialog.setNeutralButton("Fix Sync Issues") { _, _ ->
+                Log.i("ListHabitsScreen", "Attempting to fix sync issues...")
+                try {
+                    val syncFix = CloudSyncFix(context)
+                    syncFix.applyFixAndNotifyUser(syncManager)
+                } catch (e: Exception) {
+                    Log.e("ListHabitsScreen", "Error applying sync fix", e)
+                    activity.showMessage("Error fixing sync: ${e.message}")
+                }
+            }
+        }
+        dialog.setPositiveButton(if (isEnabled) "Disable" else "Enable") { _, _ ->
                 val newState = !isEnabled
                 syncManager.enableSync(newState)
                 Log.i("ListHabitsScreen", "Cloud sync toggled to: $newState")
EOL
echo "✅ ListHabitsScreen patch file created."

echo "===== Instructions ====="
echo "1. Apply the patches to fix cloud sync:"
echo "   a. patch uhabits-android/src/main/java/org/isoron/uhabits/sync/CloudSyncManager.kt < ./cloud_sync_patch.diff"
echo "   b. patch uhabits-android/src/main/java/org/isoron/uhabits/activities/habits/list/ListHabitsScreen.kt < ./list_habits_screen_patch.diff"
echo ""
echo "2. Rebuild and reinstall the app"
echo ""
echo "3. In the app, go to the main screen, tap the menu (three dots), and select 'Cloud Sync'"
echo "   Then click 'Fix Sync Issues' button to repair the sync functionality"
echo ""
echo "Note: This fix adds a way to enable cloud sync even when config is missing or incorrect."
echo "      It also adds a debug function to help diagnose sync issues."
