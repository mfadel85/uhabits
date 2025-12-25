package org.isoron.uhabits.sync

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.isoron.uhabits.R
import org.isoron.uhabits.core.models.HabitList
import org.isoron.uhabits.core.preferences.Preferences
import org.isoron.uhabits.inject.HabitsApplicationComponent
import javax.inject.Inject

/**
 * Activity for diagnosing and fixing cloud sync connectivity issues
 */
class CloudSyncDiagnosticsActivity : AppCompatActivity() {

    private lateinit var diagnosticsTextView: TextView
    private lateinit var runDiagnosticsButton: Button
    private lateinit var fixSyncButton: Button
    private lateinit var testConnectionButton: Button
    private lateinit var copyLogButton: Button
    private lateinit var forceSyncButton: Button
    private lateinit var scrollView: ScrollView

    @Inject lateinit var preferences: Preferences
    @Inject lateinit var habitList: HabitList

    private lateinit var cloudSyncManager: CloudSyncManager
    private lateinit var cloudSyncDiagnostics: CloudSyncDiagnostics
    private lateinit var cloudSyncFix: CloudSyncFix
    private lateinit var networkDiagnostics: NetworkDiagnostics

    private var lastDiagnosticReport: String? = null

    companion object {
        private const val TAG = "SyncDiagnosticsActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cloud_sync_diagnostics)

        val appComponent = (application as HabitsApplicationComponent.Provider)
            .getHabitsApplicationComponent()
        appComponent.inject(this)

        // Initialize UI elements
        diagnosticsTextView = findViewById(R.id.diagnosticsTextView)
        runDiagnosticsButton = findViewById(R.id.runDiagnosticsButton)
        fixSyncButton = findViewById(R.id.fixSyncButton)
        testConnectionButton = findViewById(R.id.testConnectionButton)
        copyLogButton = findViewById(R.id.copyLogButton)
        forceSyncButton = findViewById(R.id.forceSyncButton)
        scrollView = findViewById(R.id.scrollView)

        // Initialize components
        cloudSyncManager = CloudSyncManager(this, habitList, preferences)
        cloudSyncDiagnostics = CloudSyncDiagnostics(this)
        cloudSyncFix = CloudSyncFix(this)
        networkDiagnostics = NetworkDiagnostics(this)

        // Set up listeners
        runDiagnosticsButton.setOnClickListener { runDiagnostics() }
        fixSyncButton.setOnClickListener { attemptFixSync() }
        testConnectionButton.setOnClickListener { testConnection() }
        copyLogButton.setOnClickListener { copyToClipboard() }
        forceSyncButton.setOnClickListener { forceSyncNow() }

        // Show initial status
        updateStatusText("Ready to diagnose sync issues.\nPress 'Run Diagnostics' to begin.")
    }

    private fun runDiagnostics() {
        val progressDialog = ProgressDialog(this).apply {
            setMessage("Running diagnostics...")
            setCancelable(false)
            show()
        }

        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Run full diagnostics
                val report = withContext(Dispatchers.IO) {
                    cloudSyncDiagnostics.runDiagnostics(cloudSyncManager)
                }

                // Format and display results
                val reportText = formatDiagnosticReport(report)
                lastDiagnosticReport = reportText
                updateStatusText(reportText)

                // Show recommendations in a dialog if there are issues
                val recommendations = report.networkDiagnostics?.recommendations
                if (!recommendations.isNullOrEmpty()) {
                    showRecommendations(recommendations)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error running diagnostics", e)
                updateStatusText("Error running diagnostics: ${e.message}")
            } finally {
                progressDialog.dismiss()
            }
        }
    }

    private fun formatDiagnosticReport(report: CloudSyncDiagnostics.DiagnosticReport): String {
        val networkResults = report.networkDiagnostics ?: return "No diagnostic data available"

        return buildString {
            appendLine("===== CLOUD SYNC DIAGNOSTICS =====")
            appendLine("Configuration: ${report.configStatus}")
            appendLine("Sync Enabled: ${report.syncEnabled}")
            appendLine("Last Sync: ${report.lastSyncTime}")
            appendLine("")

            appendLine("===== NETWORK STATUS =====")
            appendLine("Connected: ${networkResults.networkStatus.isConnected}")
            appendLine("Connection Type: ${networkResults.networkStatus.transportTypes.joinToString(", ")}")
            appendLine("")

            appendLine("===== INTERNET CONNECTIVITY =====")
            appendLine("Internet Reachable: ${networkResults.internetCheck.isReachable}")
            if (!networkResults.internetCheck.isReachable) {
                appendLine("Error: ${networkResults.internetCheck.error ?: "Unknown error"}")
            }
            appendLine("")

            appendLine("===== API ENDPOINT CHECK =====")
            appendLine("URL: ${networkResults.apiGatewayCheck.url}")
            appendLine("Reachable: ${networkResults.apiGatewayCheck.isReachable}")
            appendLine("Response Code: ${networkResults.apiGatewayCheck.responseCode}")
            appendLine("Response Time: ${networkResults.apiGatewayCheck.responseTime}ms")
            if (!networkResults.apiGatewayCheck.isReachable) {
                appendLine("Error: ${networkResults.apiGatewayCheck.error ?: "Unknown error"}")
            }
            appendLine("")

            appendLine("===== DNS LOOKUP =====")
            appendLine("Lookup Time: ${if (networkResults.dnsLookupTime < 0) "Failed" else "${networkResults.dnsLookupTime}ms"}")
            appendLine("Resolved IP: ${networkResults.resolvedIp ?: "Unknown"}")
            appendLine("")

            if (report.lastNetworkError != null) {
                appendLine("===== LAST SYNC ERROR =====")
                appendLine("Error Code: ${report.lastNetworkError!!.code}")
                appendLine("Error Message: ${report.lastNetworkError!!.message}")
                appendLine("Is Connectivity Error: ${report.lastNetworkError!!.isConnectivityError}")
                appendLine("Is Timeout Error: ${report.lastNetworkError!!.isTimeoutError}")
                appendLine("Is Auth Error: ${report.lastNetworkError!!.isAuthError}")
                appendLine("")
            }

            appendLine("===== RECOMMENDATIONS =====")
            if (networkResults.recommendations.isEmpty()) {
                appendLine("No issues detected that require attention.")
            } else {
                networkResults.recommendations.forEachIndexed { index, recommendation ->
                    appendLine("${index + 1}. $recommendation")
                }
            }
        }
    }

    private fun showRecommendations(recommendations: List<String>) {
        val message = StringBuilder("Based on the diagnostic results, here are some recommendations:\n\n")
        recommendations.forEachIndexed { index, recommendation ->
            message.appendLine("${index + 1}. $recommendation")
        }

        AlertDialog.Builder(this)
            .setTitle("Sync Recommendations")
            .setMessage(message.toString())
            .setPositiveButton("OK", null)
            .show()
    }

    private fun attemptFixSync() {
        val progressDialog = ProgressDialog(this).apply {
            setMessage("Attempting to fix sync issues...")
            setCancelable(false)
            show()
        }

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    cloudSyncFix.fixSyncIssues(cloudSyncManager)
                }

                if (result) {
                    updateStatusText("Sync fix applied successfully!\nSync is now enabled.\n\nPress 'Test Connection' to verify connectivity.")
                    Toast.makeText(this@CloudSyncDiagnosticsActivity, "Sync has been fixed successfully", Toast.LENGTH_SHORT).show()
                } else {
                    updateStatusText("Failed to fix sync issues.\n\nRun diagnostics for more details.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fixing sync", e)
                updateStatusText("Error fixing sync: ${e.message}")
            } finally {
                progressDialog.dismiss()
            }
        }
    }

    private fun testConnection() {
        val progressDialog = ProgressDialog(this).apply {
            setMessage("Testing connection to API endpoint...")
            setCancelable(false)
            show()
        }

        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Extract the API endpoint from config or use default
                val apiEndpoint = "https://jodcprzip3.execute-api.eu-central-1.amazonaws.com/prod/sync"

                // Test connection
                val endpointCheck = withContext(Dispatchers.IO) {
                    networkDiagnostics.runDiagnostics(apiEndpoint)
                }

                val result = endpointCheck.apiGatewayCheck
                val internetStatus = endpointCheck.internetCheck.isReachable

                val message = StringBuilder()
                message.appendLine("===== CONNECTION TEST RESULTS =====")
                
                if (!internetStatus) {
                    message.appendLine("❌ Internet connection test failed!")
                    message.appendLine("Your device cannot reach the internet.")
                    message.appendLine("Please check your WiFi or mobile data connection.")
                } else {
                    message.appendLine("✓ Internet connection test passed")
                    
                    if (result.isReachable) {
                        message.appendLine("✓ API endpoint is reachable!")
                        message.appendLine("Response Time: ${result.responseTime}ms")
                        message.appendLine("Response Code: ${result.responseCode}")
                        message.appendLine("\nSync should work properly. If you're still experiencing issues, try performing a sync.")
                    } else {
                        message.appendLine("❌ API endpoint is NOT reachable!")
                        message.appendLine("URL: ${result.url}")
                        message.appendLine("Error: ${result.error ?: "Unknown error"}")
                        
                        if (endpointCheck.dnsLookupTime < 0) {
                            message.appendLine("\nDNS lookup failed - Your network may be blocking the AWS API Gateway domain.")
                        } else if (result.responseCode == 403 || result.responseCode == 401) {
                            message.appendLine("\nAuthentication error - The API key in your config may be invalid.")
                        } else if (result.responseCode == 404) {
                            message.appendLine("\nEndpoint not found - The Lambda function may not be deployed correctly.")
                        }
                    }
                }

                updateStatusText(message.toString())
            } catch (e: Exception) {
                Log.e(TAG, "Error testing connection", e)
                updateStatusText("Error testing connection: ${e.message}")
            } finally {
                progressDialog.dismiss()
            }
        }
    }

    private fun copyToClipboard() {
        if (lastDiagnosticReport.isNullOrEmpty()) {
            Toast.makeText(this, "No diagnostic data available", Toast.LENGTH_SHORT).show()
            return
        }

        val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Sync Diagnostics Report", lastDiagnosticReport)
        clipboardManager.setPrimaryClip(clip)
        Toast.makeText(this, "Diagnostic report copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    private fun updateStatusText(text: String) {
        diagnosticsTextView.text = text
        scrollView.post {
            scrollView.fullScroll(View.FOCUS_UP)
        }
        lastDiagnosticReport = text
    }

    private fun forceSyncNow() {
        updateStatusText("⏳ Forcing sync now...\n")
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = cloudSyncManager.performSync()
                
                withContext(Dispatchers.Main) {
                    when (result) {
                        is CloudSyncManager.SyncResult.Success -> {
                            updateStatusText("✅ Sync completed successfully!\n\n" +
                                    "Check your VPS logs:\n" +
                                    "tail -f /var/www/kpitracker.quest/logs/access.log\n\n" +
                                    "Check database:\n" +
                                    "mysql -u uhabits_user -p uhabits_analytics\n" +
                                    "SELECT COUNT(*) FROM habit_syncs;")
                            Toast.makeText(this@CloudSyncDiagnosticsActivity, 
                                "Sync successful!", Toast.LENGTH_LONG).show()
                        }
                        is CloudSyncManager.SyncResult.Error -> {
                            updateStatusText("❌ Sync error: ${result.message}\n\n" +
                                    "Check app logs:\n" +
                                    "adb logcat | grep -i sync")
                            Toast.makeText(this@CloudSyncDiagnosticsActivity, 
                                "Sync error: ${result.message}", Toast.LENGTH_LONG).show()
                        }
                        is CloudSyncManager.SyncResult.NetworkError -> {
                            updateStatusText("❌ Network error!\n\n" +
                                    "Check your internet connection\n" +
                                    "Verify VPS is accessible:\n" +
                                    "curl https://kpitracker.quest/api/sync.php")
                            Toast.makeText(this@CloudSyncDiagnosticsActivity, 
                                "Network error", Toast.LENGTH_LONG).show()
                        }
                        is CloudSyncManager.SyncResult.ConfigError -> {
                            updateStatusText("❌ Configuration error!\n\n" +
                                    "Check cloud config")
                            Toast.makeText(this@CloudSyncDiagnosticsActivity, 
                                "Config error", Toast.LENGTH_SHORT).show()
                        }
                        is CloudSyncManager.SyncResult.Disabled -> {
                            updateStatusText("⚠️ Sync is disabled")
                            Toast.makeText(this@CloudSyncDiagnosticsActivity, 
                                "Sync disabled", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    updateStatusText("❌ Sync exception: ${e.message}\n\n${e.stackTraceToString()}")
                    Toast.makeText(this@CloudSyncDiagnosticsActivity, 
                        "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
                Log.e(TAG, "Force sync error", e)
            }
        }
    }
}
