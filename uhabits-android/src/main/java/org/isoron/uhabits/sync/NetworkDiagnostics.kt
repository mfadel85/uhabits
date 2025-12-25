package org.isoron.uhabits.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.InetAddress

/**
 * Network diagnostics utility for troubleshooting cloud sync issues
 */
class NetworkDiagnostics(private val context: Context) {
    
    companion object {
        private const val TAG = "NetworkDiagnostics"
    }
    
    /**
     * Data class representing the network connectivity status
     */
    data class NetworkStatus(
        val isConnected: Boolean = false,
        val isWifi: Boolean = false,
        val isMobile: Boolean = false,
        val isVpn: Boolean = false,
        val transportTypes: List<String> = emptyList(),
        val networkInfo: String = ""
    )
    
    /**
     * Data class representing the result of an endpoint check
     */
    data class EndpointCheck(
        val url: String,
        val isReachable: Boolean,
        val responseCode: Int = -1,
        val responseTime: Long = -1,
        val error: String? = null
    )
    
    /**
     * Data class representing all diagnostic results
     */
    data class DiagnosticResults(
        val networkStatus: NetworkStatus,
        val internetCheck: EndpointCheck,
        val apiGatewayCheck: EndpointCheck,
        val dnsLookupTime: Long = -1,
        val resolvedIp: String? = null,
        val recommendations: List<String> = emptyList()
    )
    
    /**
     * Run a comprehensive network diagnostic test
     */
    suspend fun runDiagnostics(apiEndpoint: String): DiagnosticResults = withContext(Dispatchers.IO) {
        val networkStatus = checkNetworkConnectivity()
        
        // Check internet connectivity first
        val internetCheck = checkEndpoint("https://www.google.com")
        
        // Check API Gateway endpoint
        val apiGatewayCheck = checkEndpoint(apiEndpoint)
        
        // DNS lookup time test
        var dnsLookupTime = -1L
        var resolvedIp: String? = null
        try {
            val host = URL(apiEndpoint).host
            val startTime = System.currentTimeMillis()
            val inetAddress = InetAddress.getByName(host)
            dnsLookupTime = System.currentTimeMillis() - startTime
            resolvedIp = inetAddress.hostAddress
        } catch (e: Exception) {
            Log.e(TAG, "DNS lookup failed", e)
        }
        
        // Generate recommendations based on test results
        val recommendations = mutableListOf<String>()
        
        if (!networkStatus.isConnected) {
            recommendations.add("Your device is not connected to any network. Enable WiFi or mobile data.")
        } else if (!internetCheck.isReachable) {
            recommendations.add("Your device is connected to a network but cannot reach the internet. Check your network settings.")
        } else if (!apiGatewayCheck.isReachable) {
            recommendations.add("Cannot reach the API endpoint. The AWS API Gateway may be unavailable or misconfigured.")
            
            if (apiGatewayCheck.responseCode == 403 || apiGatewayCheck.responseCode == 401) {
                recommendations.add("Authentication failed. The API key may be invalid or expired.")
            } else if (apiGatewayCheck.responseCode == 404) {
                recommendations.add("API endpoint not found. Check if the Lambda function and API Gateway are correctly deployed.")
            } else if (apiGatewayCheck.responseCode >= 500) {
                recommendations.add("Server error. The AWS Lambda function may have encountered an error.")
            }
        }
        
        if (dnsLookupTime > 1000) {
            recommendations.add("DNS resolution is slow (${dnsLookupTime}ms). Your network might have DNS issues.")
        }
        
        DiagnosticResults(
            networkStatus = networkStatus,
            internetCheck = internetCheck,
            apiGatewayCheck = apiGatewayCheck,
            dnsLookupTime = dnsLookupTime,
            resolvedIp = resolvedIp,
            recommendations = recommendations
        )
    }
    
    /**
     * Check current network connectivity status
     */
    private fun checkNetworkConnectivity(): NetworkStatus {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return NetworkStatus(isConnected = false)
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return NetworkStatus(isConnected = false)
            
            val isConnected = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            val isWifi = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
            val isMobile = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
            val isVpn = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
            
            val transportTypes = mutableListOf<String>()
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) transportTypes.add("WiFi")
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) transportTypes.add("Mobile")
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) transportTypes.add("VPN")
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) transportTypes.add("Ethernet")
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH)) transportTypes.add("Bluetooth")
            
            return NetworkStatus(
                isConnected = isConnected,
                isWifi = isWifi,
                isMobile = isMobile,
                isVpn = isVpn,
                transportTypes = transportTypes,
                networkInfo = "Connected via ${transportTypes.joinToString(", ")}"
            )
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            @Suppress("DEPRECATION")
            val isConnected = networkInfo != null && networkInfo.isConnected
            @Suppress("DEPRECATION")
            val isWifi = networkInfo?.type == ConnectivityManager.TYPE_WIFI
            @Suppress("DEPRECATION")
            val isMobile = networkInfo?.type == ConnectivityManager.TYPE_MOBILE
            
            val transportType = when {
                isWifi -> "WiFi"
                isMobile -> "Mobile"
                else -> "Unknown"
            }
            
            return NetworkStatus(
                isConnected = isConnected,
                isWifi = isWifi,
                isMobile = isMobile,
                transportTypes = listOf(transportType),
                networkInfo = "Connected via $transportType"
            )
        }
    }
    
    /**
     * Check if an endpoint is reachable
     */
    private suspend fun checkEndpoint(urlString: String): EndpointCheck = withContext(Dispatchers.IO) {
        try {
            val url = URL(urlString)
            val startTime = System.currentTimeMillis()
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.instanceFollowRedirects = true
            connection.requestMethod = "GET"
            
            val responseCode = connection.responseCode
            val responseTime = System.currentTimeMillis() - startTime
            
            EndpointCheck(
                url = urlString,
                isReachable = responseCode in 200..399,
                responseCode = responseCode,
                responseTime = responseTime
            )
        } catch (e: IOException) {
            Log.e(TAG, "Failed to check endpoint: $urlString", e)
            EndpointCheck(
                url = urlString,
                isReachable = false,
                error = e.message
            )
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error checking endpoint: $urlString", e)
            EndpointCheck(
                url = urlString,
                isReachable = false,
                error = e.message
            )
        }
    }
}
