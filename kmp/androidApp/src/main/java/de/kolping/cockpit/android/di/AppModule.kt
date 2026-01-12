package de.kolping.cockpit.android.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import de.kolping.cockpit.android.auth.TokenManager
import de.kolping.cockpit.android.database.KolpingDatabase
import de.kolping.cockpit.android.repository.OfflineRepository
import de.kolping.cockpit.android.storage.FileStorageManager
import de.kolping.cockpit.android.sync.DownloadManager
import de.kolping.cockpit.android.sync.SyncManager
import de.kolping.cockpit.android.viewmodel.DashboardViewModel
import de.kolping.cockpit.android.viewmodel.GradesViewModel
import de.kolping.cockpit.android.viewmodel.CoursesViewModel
import de.kolping.cockpit.android.viewmodel.LoginViewModel
import de.kolping.cockpit.android.viewmodel.HomeViewModel
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
    
    // Room Database
    single { KolpingDatabase.getInstance(get()) }
    
    // Database DAOs
    single { get<KolpingDatabase>().moduleDao() }
    single { get<KolpingDatabase>().courseDao() }
    single { get<KolpingDatabase>().fileDao() }
    single { get<KolpingDatabase>().calendarEventDao() }
    single { get<KolpingDatabase>().studentProfileDao() }
    
    // File Storage Manager
    single { FileStorageManager(get()) }
    
    // Download Manager
    single { DownloadManager(get()) }
    
    // Offline Repository
    single { 
        OfflineRepository(
            moduleDao = get(),
            courseDao = get(),
            fileDao = get(),
            calendarEventDao = get(),
            studentProfileDao = get()
        )
    }
    
    // Sync Manager (factory to get fresh clients with tokens)
    factory {
        val tokenManager: TokenManager = get()
        val bearerToken = tokenManager.getBearerTokenSync()
        val sessionCookie = tokenManager.getSessionCookieSync()
        
        SyncManager(
            graphQLClient = GraphQLClient(bearerToken),
            moodleClient = MoodleClient(sessionCookie),
            fileStorage = get(),
            downloadManager = get(),
            moduleDao = get(),
            courseDao = get(),
            fileDao = get(),
            calendarEventDao = get(),
            studentProfileDao = get()
        )
    }
    
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
    viewModel { HomeViewModel(get(), get()) }
    viewModel { DashboardViewModel(get()) }
    viewModel { GradesViewModel(get()) }
    viewModel { CoursesViewModel(get()) }
}
