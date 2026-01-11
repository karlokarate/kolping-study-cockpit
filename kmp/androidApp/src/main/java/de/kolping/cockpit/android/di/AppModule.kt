package de.kolping.cockpit.android.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import de.kolping.cockpit.android.auth.TokenManager
import de.kolping.cockpit.android.viewmodel.DashboardViewModel
import de.kolping.cockpit.android.viewmodel.GradesViewModel
import de.kolping.cockpit.android.viewmodel.CoursesViewModel
import de.kolping.cockpit.android.viewmodel.LoginViewModel
import de.kolping.cockpit.api.GraphQLClient
import de.kolping.cockpit.api.MoodleClient
import de.kolping.cockpit.repository.StudyRepository
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_tokens")

val appModule = module {
    
    // DataStore
    single { get<Context>().dataStore }
    
    // Token Manager
    single { TokenManager(get()) }
    
    // API Clients (singletons that get tokens from Flow)
    single { 
        val tokenManager: TokenManager = get()
        // Client will be recreated when tokens change via ViewModel
        GraphQLClient(bearerToken = null) // Token injected later via flow
    }
    
    single { 
        val tokenManager: TokenManager = get()
        // Client will be recreated when tokens change via ViewModel
        MoodleClient(sessionCookie = null) // Cookie injected later via flow
    }
    
    // Repository - factory to allow recreation with fresh clients
    factory { 
        val tokenManager: TokenManager = get()
        // Get current tokens synchronously for client creation
        // This is acceptable as it's called in background thread via ViewModels
        val bearerToken = tokenManager.getBearerTokenSync()
        val sessionCookie = tokenManager.getSessionCookieSync()
        
        StudyRepository(
            GraphQLClient(bearerToken),
            MoodleClient(sessionCookie)
        )
    }
    
    // ViewModels
    viewModel { LoginViewModel(get()) }
    viewModel { DashboardViewModel(get()) }
    viewModel { GradesViewModel(get()) }
    viewModel { CoursesViewModel(get()) }
}
