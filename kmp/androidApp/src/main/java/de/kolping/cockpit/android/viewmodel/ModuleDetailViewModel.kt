package de.kolping.cockpit.android.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.kolping.cockpit.android.database.entities.FileEntity
import de.kolping.cockpit.android.database.entities.ModuleEntity
import de.kolping.cockpit.android.repository.OfflineRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

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
    
    private val _uiState = MutableStateFlow<ModuleDetailUiState>(ModuleDetailUiState.Loading)
    val uiState: StateFlow<ModuleDetailUiState> = _uiState.asStateFlow()
    
    init {
        loadModuleDetails()
    }
    
    /**
     * Load module details and associated files from offline database
     */
    private fun loadModuleDetails() {
        viewModelScope.launch {
            try {
                _uiState.value = ModuleDetailUiState.Loading
                
                // Load module entity
                val module = offlineRepository.getModuleById(moduleId)
                
                if (module == null) {
                    _uiState.value = ModuleDetailUiState.Error("Modul nicht gefunden")
                    return@launch
                }
                
                // Load files for this module
                offlineRepository.getFilesByModule(moduleId).collect { files ->
                    _uiState.value = ModuleDetailUiState.Success(
                        module = module,
                        files = files.sortedBy { it.fileName }
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load module details", e)
                _uiState.value = ModuleDetailUiState.Error("Fehler beim Laden: ${e.message}")
            }
        }
    }
    
    /**
     * Refresh module data
     */
    fun refresh() {
        loadModuleDetails()
    }
    
    /**
     * Get file type category for UI grouping
     */
    fun getFileTypeCategory(fileType: String): FileTypeCategory {
        return when (fileType.lowercase()) {
            "pdf" -> FileTypeCategory.PDF
            "doc", "docx" -> FileTypeCategory.DOCUMENT
            "xls", "xlsx" -> FileTypeCategory.SPREADSHEET
            "ppt", "pptx" -> FileTypeCategory.PRESENTATION
            "jpg", "jpeg", "png", "gif" -> FileTypeCategory.IMAGE
            "mp4", "avi", "mov" -> FileTypeCategory.VIDEO
            "mp3", "wav" -> FileTypeCategory.AUDIO
            "zip", "rar", "7z" -> FileTypeCategory.ARCHIVE
            else -> FileTypeCategory.OTHER
        }
    }
    
    /**
     * Format file size to human-readable string
     */
    fun formatFileSize(sizeBytes: Long): String {
        val kb = 1024
        val mb = kb * 1024
        val gb = mb * 1024
        
        return when {
            sizeBytes >= gb -> String.format("%.2f GB", sizeBytes.toDouble() / gb)
            sizeBytes >= mb -> String.format("%.2f MB", sizeBytes.toDouble() / mb)
            sizeBytes >= kb -> String.format("%.2f KB", sizeBytes.toDouble() / kb)
            else -> "$sizeBytes Bytes"
        }
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
    
    /**
     * File type categories for grouping and icons
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
}
