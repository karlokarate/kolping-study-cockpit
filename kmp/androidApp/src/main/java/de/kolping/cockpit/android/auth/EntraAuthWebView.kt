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
                                // Extract Bearer token from page
                                view?.evaluateJavascript(
                                    """
                                    (function() {
                                        // Try to find token in localStorage or sessionStorage
                                        const token = localStorage.getItem('token') || 
                                                     localStorage.getItem('bearerToken') ||
                                                     localStorage.getItem('auth_token') ||
                                                     sessionStorage.getItem('token');
                                        return token;
                                    })();
                                    """.trimIndent()
                                ) { result ->
                                    val token = result?.trim('"')
                                    
                                    // Get cookies
                                    val cookieManager = CookieManager.getInstance()
                                    val cookies = cookieManager.getCookie("portal.kolping-hochschule.de")
                                    val sessionCookie = cookies?.split(";")
                                        ?.find { it.trim().startsWith("MoodleSession=") }
                                        ?.substringAfter("=")
                                    
                                    if (!token.isNullOrBlank() && token != "null" && sessionCookie != null) {
                                        onTokensReceived(token, sessionCookie)
                                    } else if (sessionCookie != null) {
                                        // If we have cookie but no token, we might need to navigate to the GraphQL endpoint
                                        // For now, signal partial success
                                        onError("Token extraction incomplete - please check authentication flow")
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
