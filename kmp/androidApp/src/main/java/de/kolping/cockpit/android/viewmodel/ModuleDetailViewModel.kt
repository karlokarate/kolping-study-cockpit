package de.kolping.cockpit.android.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.kolping.cockpit.android.database.entities.FileEntity
import de.kolping.cockpit.android.database.entities.ModuleEntity
import de.kolping.cockpit.android.repository.OfflineRepository
import kotlinx.coroutines.flow.*

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
     * Format file size in human-readable format
     */
    fun formatFileSize(sizeBytes: Long): String {
        val kb = 1024.0
        val mb = kb * 1024
        val gb = mb * 1024
        
        return when {
            sizeBytes >= gb -> String.format("%.2f GB", sizeBytes / gb)
            sizeBytes >= mb -> String.format("%.2f MB", sizeBytes / mb)
            sizeBytes >= kb -> String.format("%.2f KB", sizeBytes / kb)
            else -> "$sizeBytes B"
        }
    }
    
    /**
     * Get file type category based on file extension
     */
    fun getFileTypeCategory(fileType: String): FileTypeCategory {
        return when (fileType.lowercase()) {
            "pdf" -> FileTypeCategory.PDF
            "doc", "docx", "odt", "txt", "rtf" -> FileTypeCategory.DOCUMENT
            "xls", "xlsx", "ods", "csv" -> FileTypeCategory.SPREADSHEET
            "ppt", "pptx", "odp" -> FileTypeCategory.PRESENTATION
            "jpg", "jpeg", "png", "gif", "bmp", "svg", "webp" -> FileTypeCategory.IMAGE
            "mp4", "avi", "mkv", "mov", "wmv", "flv", "webm" -> FileTypeCategory.VIDEO
            "mp3", "wav", "ogg", "m4a", "flac", "aac" -> FileTypeCategory.AUDIO
            "zip", "rar", "7z", "tar", "gz", "bz2" -> FileTypeCategory.ARCHIVE
            else -> FileTypeCategory.OTHER
        }
    }
    
    /**
     * File type categories for grouping and icon selection
     */
    enum class FileTypeCategory {
        PDF,
        DOCUMENT,
        SPREADSHEET,
        PRESENTATION,
        IMAGE,
        VIDEO,
        AUDIO,
        ARCHIVE,
        OTHER
    }
    
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
