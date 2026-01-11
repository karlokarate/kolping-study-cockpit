package de.kolping.cockpit.android.auth

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

/**
 * WebView for Microsoft Entra SSO authentication
 * Intercepts Bearer token and session cookie from the authentication flow
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun EntraAuthWebView(
    onTokensReceived: (bearerToken: String, sessionCookie: String) -> Unit,
    onError: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var currentUrl by remember { mutableStateOf("") }
    
    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        userAgentString = "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36"
                    }
                    
                    webViewClient = object : WebViewClient() {
                        
                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            super.onPageStarted(view, url, favicon)
                            isLoading = true
                            currentUrl = url ?: ""
                            loadError = null
                        }
                        
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            isLoading = false
                            
                            // Check if we've reached the CMS dashboard (successful login)
                            if (url?.contains("cms.kolping-hochschule.de") == true &&
                                !url.contains("/login") &&
                                !url.contains("/authorize")
                            ) {
                                // Get cookies from CookieManager
                                val cookieManager = CookieManager.getInstance()
                                
                                // Extract session cookie from portal
                                val portalCookies = cookieManager.getCookie("portal.kolping-hochschule.de")
                                val sessionCookie = portalCookies?.split(";")
                                    ?.find { it.trim().startsWith("MoodleSession=") }
                                    ?.substringAfter("=")
                                    ?.trim()
                                
                                // Extract bearer token from CMS domain
                                val cmsCookies = cookieManager.getCookie("cms.kolping-hochschule.de")
                                
                                // Try to intercept bearer token from page context
                                // Check if we can find the token in the page
                                view?.evaluateJavascript(
                                    """
                                    (function() {
                                        // Check if authorization header or token is available in page context
                                        if (window.authToken) return window.authToken;
                                        if (window.bearerToken) return window.bearerToken;
                                        
                                        // Check meta tags or data attributes
                                        var meta = document.querySelector('meta[name="auth-token"]');
                                        if (meta) return meta.getAttribute('content');
                                        
                                        return null;
                                    })();
                                    """.trimIndent()
                                ) { tokenResult ->
                                    val extractedToken = tokenResult?.trim('"')?.takeIf { 
                                        it != "null" && it.isNotBlank() 
                                    }
                                    
                                    // For production: token should be intercepted from network calls
                                    // This is a fallback - ideally use OkHttp interceptor or WebViewClient
                                    // to capture the Authorization header from API calls
                                    
                                    if (sessionCookie != null) {
                                        // We have session cookie, proceed with it
                                        // Bearer token will be obtained from first API call or separate auth flow
                                        val bearerToken = extractedToken ?: ""
                                        onTokensReceived(bearerToken, sessionCookie)
                                    } else {
                                        onError("Authentication incomplete - session cookie not found")
                                    }
                                }
                            }
                        }
                        
                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: android.webkit.WebResourceError?
                        ) {
                            super.onReceivedError(view, request, error)
                            if (request?.isForMainFrame == true) {
                                loadError = error?.description?.toString() ?: "Unknown error"
                                onError(loadError!!)
                            }
                        }
                    }
                    
                    // Start the auth flow by loading the CMS login page
                    loadUrl("https://cms.kolping-hochschule.de")
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        
        // Loading indicator
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator()
                    Text(
                        text = "Lade Anmeldeseite...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (currentUrl.isNotBlank()) {
                        Text(
                            text = currentUrl,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        
        // Error display
        loadError?.let { error ->
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = "Fehler: $error",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}
