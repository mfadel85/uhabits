package org.isoron.uhabits.sync

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Network diagnostics tool specifically for cloud sync connectivity issues
 */
class CloudSyncDiagnostics(private val context: Context) {

    companion object {
        private const val TAG = "CloudSyncDiagnostics"
        private const val CONFIG_FILE = "cloud_config.json"
        private const val DIAGNOSTICS_LOG_FILE = "cloud_sync_diagnostics.log"
    }

    private val networkDiagnostics = NetworkDiagnostics(context)
    
    /**
     * Runs comprehensive diagnostics and returns a detailed report
     */
    suspend fun runDiagnostics(syncManager: CloudSyncManager): DiagnosticReport = withContext(Dispatchers.IO) {
        Log.i(TAG, "Running comprehensive cloud sync diagnostics")
        
        val report = DiagnosticReport()
        
        // Check configuration 
        val configExists = checkConfigFile(report)
        
        // Extract API endpoint from config if possible
        val apiEndpoint = if (configExists) {
            extractApiEndpoint() ?: "https://jodcprzip3.execute-api.eu-central-1.amazonaws.com/prod/sync"
        } else {
            "https://jodcprzip3.execute-api.eu-central-1.amazonaws.com/prod/sync"
        }
        
        // Check network and endpoint connectivity
        val networkResults = networkDiagnostics.runDiagnostics(apiEndpoint)
        report.networkDiagnostics = networkResults
        
        // Check sync status
        checkSyncStatus(report, syncManager)
        
        // Log findings to file for debugging
        val logFileContent = createDiagnosticsLog(report, networkResults)
        saveDiagnosticsToFile(logFileContent)
        
        report
    }
    
    /**
     * Checks if the config file exists and is valid
     */
    private fun checkConfigFile(report: DiagnosticReport): Boolean {
        try {
            val inputStream = context.assets.open(CONFIG_FILE)
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(jsonString)
            
            // Validate required fields
            val awsConfig = json.optJSONObject("aws_config")
            if (awsConfig == null) {
                report.configStatus = "Missing aws_config in cloud_config.json"
                return false
            }
            
            val apiGateway = awsConfig.optJSONObject("api_gateway")
            if (apiGateway == null) {
                report.configStatus = "Missing api_gateway in cloud_config.json"
                return false
            }
            
            val baseUrl = apiGateway.optString("base_url")
            val syncEndpoint = apiGateway.optString("sync_endpoint")
            val apiKey = apiGateway.optString("api_key")
            
            if (baseUrl.isBlank()) {
                report.configStatus = "Missing base_url in cloud_config.json"
                return false
            }
            
            if (syncEndpoint.isBlank()) {
                report.configStatus = "Missing sync_endpoint in cloud_config.json"
                return false
            }
            
            if (apiKey.isBlank()) {
                report.configStatus = "Missing api_key in cloud_config.json"
                return false
            }
            
            report.configStatus = "Valid"
            report.apiEndpoint = "$baseUrl$syncEndpoint"
            report.apiKeyValid = apiKey.isNotBlank()
            
            return true
            
        } catch (e: Exception) {
            Log.e(TAG, "Error validating config", e)
            report.configStatus = "Error: ${e.message}"
            return false
        }
    }
    
    /**
     * Extract API endpoint from config file
     */
    private fun extractApiEndpoint(): String? {
        return try {
            val inputStream = context.assets.open(CONFIG_FILE)
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(jsonString)
            
            val awsConfig = json.optJSONObject("aws_config")
            val apiGateway = awsConfig?.optJSONObject("api_gateway")
            val baseUrl = apiGateway?.optString("base_url")
            val syncEndpoint = apiGateway?.optString("sync_endpoint")
            
            if (baseUrl != null && syncEndpoint != null) {
                "$baseUrl$syncEndpoint"
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting API endpoint", e)
            null
        }
    }
    
    /**
     * Check sync status and settings
     */
    private fun checkSyncStatus(report: DiagnosticReport, syncManager: CloudSyncManager) {
        report.syncEnabled = syncManager.isSyncEnabled()
        report.autoSyncEnabled = syncManager.isAutoSyncEnabled()
        report.lastSyncTime = formatTime(syncManager.getLastSyncTime())
        report.lastNetworkError = syncManager.getLastNetworkError()
    }
    
    private fun formatTime(timestamp: Long): String {
        if (timestamp <= 0) return "Never"
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(timestamp))
    }
    
    /**
     * Generate a comprehensive log file with all diagnostic information
     */
    private fun createDiagnosticsLog(report: DiagnosticReport, networkResults: NetworkDiagnostics.DiagnosticResults): String {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        
        return buildString {
            appendLine("===== CLOUD SYNC DIAGNOSTICS REPORT =====")
            appendLine("Generated: $timestamp")
            appendLine("")
            
            appendLine("===== CONFIGURATION STATUS =====")
            appendLine("Config Status: ${report.configStatus}")
            appendLine("API Endpoint: ${report.apiEndpoint}")
            appendLine("API Key Present: ${report.apiKeyValid}")
            appendLine("")
            
            appendLine("===== SYNC STATUS =====")
            appendLine("Sync Enabled: ${report.syncEnabled}")
            appendLine("Auto Sync Enabled: ${report.autoSyncEnabled}")
            appendLine("Last Sync Time: ${report.lastSyncTime}")
            appendLine("")
            
            appendLine("===== LAST NETWORK ERROR =====")
            val lastError = report.lastNetworkError
            if (lastError != null) {
                appendLine("Error Code: ${lastError.code}")
                appendLine("Error Message: ${lastError.message}")
                appendLine("Is Connectivity Error: ${lastError.isConnectivityError}")
                appendLine("Is Timeout Error: ${lastError.isTimeoutError}")
                appendLine("Is Authentication Error: ${lastError.isAuthError}")
            } else {
                appendLine("No previous network errors recorded")
            }
            appendLine("")
            
            appendLine("===== NETWORK DIAGNOSTICS =====")
            
            appendLine("Network Status:")
            appendLine("  Connected: ${networkResults.networkStatus.isConnected}")
            appendLine("  Connection Type: ${networkResults.networkStatus.transportTypes.joinToString(", ")}")
            appendLine("")
            
            appendLine("Internet Check (Google.com):")
            appendLine("  Reachable: ${networkResults.internetCheck.isReachable}")
            appendLine("  Response Code: ${networkResults.internetCheck.responseCode}")
            appendLine("  Response Time: ${networkResults.internetCheck.responseTime}ms")
            appendLine("  Error: ${networkResults.internetCheck.error ?: "None"}")
            appendLine("")
            
            appendLine("API Gateway Check:")
            appendLine("  URL: ${networkResults.apiGatewayCheck.url}")
            appendLine("  Reachable: ${networkResults.apiGatewayCheck.isReachable}")
            appendLine("  Response Code: ${networkResults.apiGatewayCheck.responseCode}")
            appendLine("  Response Time: ${networkResults.apiGatewayCheck.responseTime}ms")
            appendLine("  Error: ${networkResults.apiGatewayCheck.error ?: "None"}")
            appendLine("")
            
            appendLine("DNS Lookup:")
            appendLine("  Lookup Time: ${if (networkResults.dnsLookupTime < 0) "Failed" else "${networkResults.dnsLookupTime}ms"}")
            appendLine("  Resolved IP: ${networkResults.resolvedIp ?: "Unknown"}")
            appendLine("")
            
            appendLine("===== RECOMMENDATIONS =====")
            if (networkResults.recommendations.isEmpty()) {
                appendLine("No specific recommendations")
            } else {
                networkResults.recommendations.forEachIndexed { index, recommendation ->
                    appendLine("${index + 1}. $recommendation")
                }
            }
            appendLine("")
            
            if (!report.syncEnabled) {
                appendLine("RECOMMENDATION: Sync is disabled in preferences. Enable it in the app settings.")
            }
            
            if (report.configStatus != "Valid") {
                appendLine("RECOMMENDATION: Configuration file is not valid. Check cloud_config.json.")
            }
            
            appendLine("===== END OF REPORT =====")
        }
    }
    
    /**
     * Save diagnostics report to external storage for later analysis
     */
    private fun saveDiagnosticsToFile(content: String) {
        try {
            val file = File(context.getExternalFilesDir(null), DIAGNOSTICS_LOG_FILE)
            FileOutputStream(file).use { stream ->
                stream.write(content.toByteArray())
            }
            Log.i(TAG, "Diagnostics saved to ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save diagnostics log", e)
        }
    }
    
    /**
     * Data class containing diagnostic report information
     */
    data class DiagnosticReport(
        var configStatus: String = "Unknown",
        var apiEndpoint: String = "Unknown",
        var apiKeyValid: Boolean = false,
        var syncEnabled: Boolean = false,
        var autoSyncEnabled: Boolean = false,
        var lastSyncTime: String = "Never",
        var lastNetworkError: CloudSyncManager.NetworkError? = null,
        var networkDiagnostics: NetworkDiagnostics.DiagnosticResults? = null
    )
}
