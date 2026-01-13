package de.kolping.cockpit.android.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.kolping.cockpit.android.database.entities.FileEntity
import de.kolping.cockpit.android.database.entities.ModuleEntity
import de.kolping.cockpit.android.repository.OfflineRepository
import de.kolping.cockpit.android.util.FileUtils
import kotlinx.coroutines.flow.*

/**
 * Type alias for FileTypeCategory from FileUtils.
 * Review Finding Fix: Moved enum to shared utility to avoid cross-ViewModel dependencies.
 */
typealias FileTypeCategory = FileUtils.FileTypeCategory

/**
 * ViewModel for ModuleDetailScreen
 * Manages display of individual module with all associated files
 * Based on Issue #6 PR 5 requirements
 */
class ModuleDetailViewModel(
    private val offlineRepository: OfflineRepository,
    private val moduleId: String
) : ViewModel() {
    
    companion object {
        private const val TAG = "ModuleDetailViewModel"
    }
    
    /**
     * UI state that combines module and files data with proper lifecycle management
     * Using stateIn() to convert Flow to StateFlow with proper cancellation
     */
    val uiState: StateFlow<ModuleDetailUiState> = flow {
        try {
            emit(ModuleDetailUiState.Loading)
            
            // Load module entity
            val module = offlineRepository.getModuleById(moduleId)
            
            if (module == null) {
                emit(ModuleDetailUiState.Error("Modul nicht gefunden"))
                return@flow
            }
            
            // Transform files Flow to UI state
            offlineRepository.getFilesByModule(moduleId)
                .map { files ->
                    ModuleDetailUiState.Success(
                        module = module,
                        files = files.sortedBy { it.fileName }
                    )
                }
                .collect { emit(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load module details", e)
            emit(ModuleDetailUiState.Error("Fehler beim Laden der Moduldetails: ${e.message}"))
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ModuleDetailUiState.Loading
    )
    
    /**
     * Refresh module details
     */
    fun refresh() {
        // Trigger a refresh by re-collecting the flow
        // The flow will automatically re-emit when the database changes
        // This is a no-op since we're using reactive flows
    }
    
    /**
     * Format file size in human-readable format.
     * Review Finding Fix: Delegates to shared FileUtils to avoid code duplication.
     */
    fun formatFileSize(sizeBytes: Long): String = FileUtils.formatFileSize(sizeBytes)
    
    /**
     * Get file type category based on file extension.
     * Review Finding Fix: Delegates to shared FileUtils to avoid code duplication.
     */
    fun getFileTypeCategory(fileType: String): FileTypeCategory = FileUtils.getFileTypeCategory(fileType)
    
    /**
     * UI State for ModuleDetailScreen
     */
    sealed class ModuleDetailUiState {
        object Loading : ModuleDetailUiState()
        data class Success(
            val module: ModuleEntity,
            val files: List<FileEntity>
        ) : ModuleDetailUiState()
        data class Error(val message: String) : ModuleDetailUiState()
    }
}
