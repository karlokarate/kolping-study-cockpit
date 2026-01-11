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
    
    // API Clients (created lazily based on stored tokens)
    factory { 
        val tokenManager: TokenManager = get()
        GraphQLClient(bearerToken = tokenManager.getBearerToken())
    }
    
    factory { 
        val tokenManager: TokenManager = get()
        MoodleClient(sessionCookie = tokenManager.getSessionCookie())
    }
    
    // Repository
    factory { StudyRepository(get(), get()) }
    
    // ViewModels
    viewModel { LoginViewModel(get()) }
    viewModel { DashboardViewModel(get()) }
    viewModel { GradesViewModel(get()) }
    viewModel { CoursesViewModel(get()) }
}
