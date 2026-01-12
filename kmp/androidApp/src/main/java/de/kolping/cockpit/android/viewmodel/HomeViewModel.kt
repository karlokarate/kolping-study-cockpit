package de.kolping.cockpit.android.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.kolping.cockpit.android.database.entities.CalendarEventEntity
import de.kolping.cockpit.android.database.entities.ModuleEntity
import de.kolping.cockpit.android.database.entities.StudentProfileEntity
import de.kolping.cockpit.android.repository.OfflineRepository
import de.kolping.cockpit.android.sync.SyncManager
import de.kolping.cockpit.android.sync.SyncProgress
import de.kolping.cockpit.android.sync.SyncResult
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * ViewModel for HomeScreen
 * Manages offline data display and "Fetch All" sync functionality
 * Based on Issue #6 PR 4 requirements
 */
class HomeViewModel(
    private val offlineRepository: OfflineRepository,
    private val syncManager: SyncManager
) : ViewModel() {
    
    companion object {
        private const val TAG = "HomeViewModel"
    }
    
    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()
    
    init {
        loadOfflineData()
    }
    
    /**
     * Load data from offline database
     * Shows cached data immediately (offline-first)
     */
    fun loadOfflineData() {
        viewModelScope.launch {
            try {
                _uiState.value = HomeUiState.Loading
                
                // Collect all offline data
                combine(
                    offlineRepository.getCurrentSemesterModules(),
                    offlineRepository.getStudentProfile(),
                    offlineRepository.getUpcomingEvents(limit = 3)
                ) { modules, profile, events ->
                    Triple(modules, profile, events)
                }.collect { (modules, profile, events) ->
                    val lastSync = profile?.lastSync ?: 0L
                    _uiState.value = HomeUiState.Success(
                        studentProfile = profile,
                        currentSemesterModules = modules,
                        upcomingEvents = events,
                        lastSyncTimestamp = lastSync
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load offline data", e)
                _uiState.value = HomeUiState.Error("Fehler beim Laden der Daten: ${e.message}")
            }
        }
    }
    
    /**
     * Start "Fetch All" sync
     * Downloads and stores all study data
     */
    fun startSync() {
        viewModelScope.launch {
            try {
                _syncState.value = SyncState.Syncing(
                    progress = 0f,
                    phase = "Initialisierung...",
                    filesDownloaded = 0,
                    totalFiles = 0
                )
                
                syncManager.syncAll().collect { result ->
                    when (result) {
                        is SyncProgress -> {
                            _syncState.value = SyncState.Syncing(
                                progress = result.progress,
                                phase = result.message ?: result.phase.name,
                                filesDownloaded = result.filesDownloaded,
                                totalFiles = result.totalFiles
                            )
                        }
                        is SyncResult.Success -> {
                            _syncState.value = SyncState.Success(
                                modulesCount = result.modulesCount,
                                coursesCount = result.coursesCount,
                                filesDownloaded = result.filesDownloaded,
                                eventsCount = result.eventsCount,
                                durationMs = result.durationMs
                            )
                            // Reload data after successful sync
                            loadOfflineData()
                        }
                        is SyncResult.Failure -> {
                            _syncState.value = SyncState.Error(
                                message = result.message ?: "Sync fehlgeschlagen"
                            )
                        }
                        is SyncResult.Cancelled -> {
                            _syncState.value = SyncState.Idle
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Sync failed with exception", e)
                _syncState.value = SyncState.Error("Sync fehlgeschlagen: ${e.message}")
            }
        }
    }
    
    /**
     * Reset sync state to idle
     */
    fun resetSyncState() {
        _syncState.value = SyncState.Idle
    }
    
    /**
     * Format timestamp to human-readable string
     */
    fun formatLastSync(timestamp: Long): String {
        if (timestamp == 0L) return "Noch nie synchronisiert"
        
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        
        val minutes = diff / (1000 * 60)
        val hours = diff / (1000 * 60 * 60)
        val days = diff / (1000 * 60 * 60 * 24)
        
        return when {
            minutes < 1 -> "Gerade eben"
            minutes < 60 -> "vor $minutes Minute${if (minutes > 1) "n" else ""}"
            hours < 24 -> "vor $hours Stunde${if (hours > 1) "n" else ""}"
            days < 7 -> "vor $days Tag${if (days > 1) "en" else ""}"
            else -> {
                val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.GERMAN)
                sdf.format(Date(timestamp))
            }
        }
    }
    
    /**
     * UI State for HomeScreen
     */
    sealed class HomeUiState {
        object Loading : HomeUiState()
        data class Success(
            val studentProfile: StudentProfileEntity?,
            val currentSemesterModules: List<ModuleEntity>,
            val upcomingEvents: List<CalendarEventEntity>,
            val lastSyncTimestamp: Long
        ) : HomeUiState()
        data class Error(val message: String) : HomeUiState()
    }
    
    /**
     * Sync State for progress tracking
     */
    sealed class SyncState {
        object Idle : SyncState()
        data class Syncing(
            val progress: Float,
            val phase: String,
            val filesDownloaded: Int,
            val totalFiles: Int
        ) : SyncState()
        data class Success(
            val modulesCount: Int,
            val coursesCount: Int,
            val filesDownloaded: Int,
            val eventsCount: Int,
            val durationMs: Long
        ) : SyncState()
        data class Error(val message: String) : SyncState()
    }
}
