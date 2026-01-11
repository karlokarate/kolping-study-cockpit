package de.kolping.cockpit.android.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import de.kolping.cockpit.android.auth.EntraAuthWebView
import de.kolping.cockpit.android.viewmodel.LoginViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = koinViewModel()
) {
    val loginState by viewModel.loginState.collectAsState()
    
    LaunchedEffect(loginState) {
        if (loginState is LoginViewModel.LoginState.Success) {
            onLoginSuccess()
        }
    }
    
    EntraAuthWebView(
        onTokensReceived = { bearerToken, sessionCookie ->
            viewModel.onTokensReceived(bearerToken, sessionCookie)
        },
        onError = { error ->
            viewModel.onError(error)
        },
        modifier = modifier
    )
}
