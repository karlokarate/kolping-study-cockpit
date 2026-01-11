package de.kolping.cockpit.android.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

/**
 * Manages authentication tokens using DataStore
 */
class TokenManager(private val dataStore: DataStore<Preferences>) {
    
    companion object {
        private val BEARER_TOKEN_KEY = stringPreferencesKey("bearer_token")
        private val SESSION_COOKIE_KEY = stringPreferencesKey("session_cookie")
    }
    
    // Flow for observing tokens
    val bearerTokenFlow: Flow<String?> = dataStore.data.map { preferences ->
        preferences[BEARER_TOKEN_KEY]
    }
    
    val sessionCookieFlow: Flow<String?> = dataStore.data.map { preferences ->
        preferences[SESSION_COOKIE_KEY]
    }
    
    val isAuthenticatedFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[BEARER_TOKEN_KEY] != null && preferences[SESSION_COOKIE_KEY] != null
    }
    
    /**
     * Get bearer token synchronously.
     * Should only be called from background threads or coroutines.
     */
    fun getBearerTokenSync(): String? = runBlocking {
        bearerTokenFlow.first()
    }
    
    /**
     * Get session cookie synchronously.
     * Should only be called from background threads or coroutines.
     */
    fun getSessionCookieSync(): String? = runBlocking {
        sessionCookieFlow.first()
    }
    
    /**
     * Save Bearer token
     */
    suspend fun saveBearerToken(token: String) {
        dataStore.edit { preferences ->
            preferences[BEARER_TOKEN_KEY] = token
        }
    }
    
    /**
     * Save session cookie
     */
    suspend fun saveSessionCookie(cookie: String) {
        dataStore.edit { preferences ->
            preferences[SESSION_COOKIE_KEY] = cookie
        }
    }
    
    /**
     * Clear all tokens (logout)
     */
    suspend fun clearTokens() {
        dataStore.edit { preferences ->
            preferences.remove(BEARER_TOKEN_KEY)
            preferences.remove(SESSION_COOKIE_KEY)
        }
    }
}
