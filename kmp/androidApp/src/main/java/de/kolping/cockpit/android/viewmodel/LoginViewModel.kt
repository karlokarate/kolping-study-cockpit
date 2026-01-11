package de.kolping.cockpit.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.kolping.cockpit.android.auth.TokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LoginViewModel(
    private val tokenManager: TokenManager
) : ViewModel() {
    
    private val _loginState = MutableStateFlow<LoginState>(LoginState.Initial)
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()
    
    fun onTokensReceived(bearerToken: String, sessionCookie: String) {
        viewModelScope.launch {
            try {
                _loginState.value = LoginState.Loading
                tokenManager.saveBearerToken(bearerToken)
                tokenManager.saveSessionCookie(sessionCookie)
                _loginState.value = LoginState.Success
            } catch (e: Exception) {
                _loginState.value = LoginState.Error(e.message ?: "Unknown error")
            }
        }
    }
    
    fun onError(message: String) {
        _loginState.value = LoginState.Error(message)
    }
    
    fun reset() {
        _loginState.value = LoginState.Initial
    }
    
    sealed class LoginState {
        object Initial : LoginState()
        object Loading : LoginState()
        object Success : LoginState()
        data class Error(val message: String) : LoginState()
    }
}
