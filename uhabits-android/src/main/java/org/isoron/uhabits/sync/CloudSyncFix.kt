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
            
            // 2. Force enable sync in preferences and reload config
            val syncEnabled = syncManager.forceEnableSync()
            Log.i(TAG, "Force enable sync result: $syncEnabled")
            
            // 3. Verify fix was successful
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
